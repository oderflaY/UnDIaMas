import { onDocumentCreated } from "firebase-functions/v2/firestore";
import { onCall, HttpsError } from "firebase-functions/v2/https";
import { defineSecret } from "firebase-functions/params";
import * as logger from "firebase-functions/logger";
import { initializeApp } from "firebase-admin/app";
import { getFirestore } from "firebase-admin/firestore";
import { getMessaging } from "firebase-admin/messaging";
import { buildEmergencyAction } from "./emergencyProtocol";
import { buildLogIaTrace } from "./logIaTrace";
import { runAgentTurn } from "./aiAgent";
import { RestGeminiClient } from "./geminiClient";
import { toAiMessageRecord, toCheckInRecord, toUserProfileRecord } from "./firestoreMappers";

initializeApp();

const geminiApiKey = defineSecret("GEMINI_API_KEY");

const COLECCION_USUARIOS = "usuarios";
const SUBCOLECCION_CHECKINS = "checkins";
const SUBCOLECCION_ALERTAS = "alertas";
const SUBCOLECCION_MENSAJES_IA = "mensajesIA";

/**
 * Al crearse un check-in en rojo, envia la notificacion de emergencia y sugiere el
 * contacto de confianza del perfil. La decision vive en `buildEmergencyAction`
 * (funcion pura, cubierta por tests); aqui solo se conecta con Firebase.
 */
export const onCheckInCreated = onDocumentCreated(
  `${COLECCION_USUARIOS}/{userId}/${SUBCOLECCION_CHECKINS}/{checkInId}`,
  async (event) => {
    const checkInDoc = event.data?.data();
    if (!checkInDoc) return;
    const checkIn = toCheckInRecord(event.params.checkInId, event.params.userId, checkInDoc);

    const profileSnap = await getFirestore().collection(COLECCION_USUARIOS).doc(event.params.userId).get();
    if (!profileSnap.exists) return;
    const profile = toUserProfileRecord(event.params.userId, profileSnap.data() ?? {});

    const action = buildEmergencyAction(checkIn, profile);
    if (!action) return;

    if (action.fcmToken) {
      await getMessaging().send({
        token: action.fcmToken,
        notification: action.notification,
      });
    }

    logger.info("Protocolo de emergencia activado", {
      userId: event.params.userId,
      checkInId: event.params.checkInId,
      suggestedContact: action.suggestedContact?.name,
    });
  }
);

/**
 * Al crearse un mensaje en el chat de IA, lo enlaza con el check-in mas reciente del
 * usuario y escribe el registro de trazabilidad en `logs_ia` (check-in -> nivel de
 * riesgo -> respuesta de IA). El chat no esta atado a un check-in especifico en la UI,
 * asi que se usa el mas reciente como contexto de riesgo vigente.
 */
export const onAiMessageCreated = onDocumentCreated(
  `${COLECCION_USUARIOS}/{userId}/${SUBCOLECCION_MENSAJES_IA}/{aiMessageId}`,
  async (event) => {
    const aiMessageDoc = event.data?.data();
    if (!aiMessageDoc) return;
    const aiMessage = toAiMessageRecord(event.params.aiMessageId, event.params.userId, aiMessageDoc);

    const latestCheckInSnap = await getFirestore()
      .collection(COLECCION_USUARIOS)
      .doc(event.params.userId)
      .collection(SUBCOLECCION_CHECKINS)
      .orderBy("fechaHora", "desc")
      .limit(1)
      .get();
    if (latestCheckInSnap.empty) return;
    const latestDoc = latestCheckInSnap.docs[0];
    const checkIn = toCheckInRecord(latestDoc.id, event.params.userId, latestDoc.data());

    const trace = buildLogIaTrace(checkIn, aiMessage);
    await getFirestore().collection("logs_ia").add(trace);
  }
);

/**
 * Callable que atiende al agente de IA (Gemini + Tools/Function Calling). El uid nunca
 * viaja en el payload: viene siempre de `request.auth`, así que el modelo no puede pedir
 * datos de otro usuario aunque lo intente via los argumentos de una tool.
 */
export const agentChat = onCall({ secrets: [geminiApiKey] }, async (request) => {
  if (!request.auth) {
    throw new HttpsError("unauthenticated", "Debes iniciar sesion para hablar con el asistente.");
  }
  const userId = request.auth.uid;
  const prompt = request.data?.prompt;
  if (typeof prompt !== "string" || prompt.trim().length === 0) {
    throw new HttpsError("invalid-argument", "El mensaje (prompt) es obligatorio.");
  }

  const firestore = getFirestore();
  const checkInsOf = firestore.collection(COLECCION_USUARIOS).doc(userId).collection(SUBCOLECCION_CHECKINS);

  const outcome = await runAgentTurn(userId, prompt, {
    gemini: new RestGeminiClient(geminiApiKey.value()),
    readRecentCheckIns: async (uid, limit) => {
      const snap = await checkInsOf.orderBy("fechaHora", "desc").limit(limit).get();
      return snap.docs.map((doc) => toCheckInRecord(doc.id, uid, doc.data()));
    },
    saveAlert: async (alert) => {
      const ref = await firestore
        .collection(COLECCION_USUARIOS)
        .doc(alert.userId)
        .collection(SUBCOLECCION_ALERTAS)
        .add({ nivelRiesgo: alert.riskLevel, mensaje: alert.message, fecha: alert.createdAt });
      return ref.id;
    },
  });

  return outcome;
});
