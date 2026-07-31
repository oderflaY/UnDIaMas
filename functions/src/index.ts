import { onDocumentCreated } from "firebase-functions/v2/firestore";
import { onCall, HttpsError } from "firebase-functions/v2/https";
import { defineSecret } from "firebase-functions/params";
import * as logger from "firebase-functions/logger";
import { initializeApp } from "firebase-admin/app";
import { getFirestore, Timestamp } from "firebase-admin/firestore";
import { getMessaging } from "firebase-admin/messaging";
import { buildEmergencyAction } from "./emergencyProtocol";
import { buildLogIaTrace } from "./logIaTrace";
import { runAgentTurn } from "./aiAgent";
import { RestGeminiClient } from "./geminiClient";
import { toAiMessageRecord, toCheckInRecord, toUserProfileRecord } from "./firestoreMappers";

initializeApp();

const geminiApiKey = defineSecret("GEMINI_API_KEY");

// Colecciones planas en la raiz: el aislamiento sale del campo `userId` del documento,
// no de la ruta. Los mismos nombres que usa la app en Colecciones.kt.
const COLECCION_USERS = "users";
const COLECCION_CHECK_INS = "check_ins";
const COLECCION_AI_MESSAGES = "ai_messages";
const COLECCION_ALERTS = "alerts";
const COLECCION_AI_LOGS = "ai_logs";

/**
 * Al crearse un check-in en rojo, envia la notificacion de emergencia y sugiere el
 * contacto de confianza del perfil. La decision vive en `buildEmergencyAction`
 * (funcion pura, cubierta por tests); aqui solo se conecta con Firebase.
 */
export const onCheckInCreated = onDocumentCreated(
  `${COLECCION_CHECK_INS}/{checkInId}`,
  async (event) => {
    const checkInDoc = event.data?.data();
    // En el esquema plano el uid ya no viene en la ruta: viene dentro del documento.
    // Un check-in sin userId no se puede atribuir a nadie, asi que no se procesa.
    const userId = checkInDoc?.userId;
    if (!checkInDoc || typeof userId !== "string" || userId.length === 0) return;

    const checkIn = toCheckInRecord(event.params.checkInId, userId, checkInDoc);

    const profileSnap = await getFirestore().collection(COLECCION_USERS).doc(userId).get();
    if (!profileSnap.exists) return;
    const profile = toUserProfileRecord(userId, profileSnap.data() ?? {});

    const action = buildEmergencyAction(checkIn, profile);
    if (!action) return;

    if (action.fcmToken) {
      await getMessaging().send({
        token: action.fcmToken,
        notification: action.notification,
      });
    }

    logger.info("Protocolo de emergencia activado", {
      userId,
      checkInId: event.params.checkInId,
      suggestedContact: action.suggestedContact?.name,
    });
  }
);

/**
 * Al crearse un mensaje en el chat de IA, lo enlaza con el check-in mas reciente del
 * usuario y escribe el registro de trazabilidad en `ai_logs` (check-in -> nivel de
 * riesgo -> respuesta de IA). El chat no esta atado a un check-in especifico en la UI,
 * asi que se usa el mas reciente como contexto de riesgo vigente.
 */
export const onAiMessageCreated = onDocumentCreated(
  `${COLECCION_AI_MESSAGES}/{aiMessageId}`,
  async (event) => {
    const aiMessageDoc = event.data?.data();
    const userId = aiMessageDoc?.userId;
    if (!aiMessageDoc || typeof userId !== "string" || userId.length === 0) return;

    const aiMessage = toAiMessageRecord(event.params.aiMessageId, userId, aiMessageDoc);

    const latestCheckInSnap = await getFirestore()
      .collection(COLECCION_CHECK_INS)
      .where("userId", "==", userId)
      .orderBy("timestamp", "desc")
      .limit(1)
      .get();
    if (latestCheckInSnap.empty) return;
    const latestDoc = latestCheckInSnap.docs[0];
    const checkIn = toCheckInRecord(latestDoc.id, userId, latestDoc.data());

    const trace = buildLogIaTrace(checkIn, aiMessage);
    // `buildLogIaTrace` es pura y devuelve la fecha como ISO; al persistir se convierte a
    // Timestamp, que es como estan todas las demas fechas de la base.
    await getFirestore()
      .collection(COLECCION_AI_LOGS)
      .add({ ...trace, createdAt: Timestamp.fromMillis(Date.parse(trace.createdAt)) });
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

  const outcome = await runAgentTurn(userId, prompt, {
    gemini: new RestGeminiClient(geminiApiKey.value()),
    readRecentCheckIns: async (uid, limit) => {
      const snap = await firestore
        .collection(COLECCION_CHECK_INS)
        .where("userId", "==", uid)
        .orderBy("timestamp", "desc")
        .limit(limit)
        .get();
      return snap.docs.map((doc) => toCheckInRecord(doc.id, uid, doc.data()));
    },
    saveAlert: async (alert) => {
      const ref = await firestore.collection(COLECCION_ALERTS).add({
        userId: alert.userId,
        riskLevel: alert.riskLevel,
        message: alert.message,
        timestamp: Timestamp.fromMillis(Date.parse(alert.createdAt) || Date.now()),
        handled: false,
      });
      return ref.id;
    },
  });

  return outcome;
});
