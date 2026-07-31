"use strict";
var __createBinding = (this && this.__createBinding) || (Object.create ? (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    var desc = Object.getOwnPropertyDescriptor(m, k);
    if (!desc || ("get" in desc ? !m.__esModule : desc.writable || desc.configurable)) {
      desc = { enumerable: true, get: function() { return m[k]; } };
    }
    Object.defineProperty(o, k2, desc);
}) : (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    o[k2] = m[k];
}));
var __setModuleDefault = (this && this.__setModuleDefault) || (Object.create ? (function(o, v) {
    Object.defineProperty(o, "default", { enumerable: true, value: v });
}) : function(o, v) {
    o["default"] = v;
});
var __importStar = (this && this.__importStar) || (function () {
    var ownKeys = function(o) {
        ownKeys = Object.getOwnPropertyNames || function (o) {
            var ar = [];
            for (var k in o) if (Object.prototype.hasOwnProperty.call(o, k)) ar[ar.length] = k;
            return ar;
        };
        return ownKeys(o);
    };
    return function (mod) {
        if (mod && mod.__esModule) return mod;
        var result = {};
        if (mod != null) for (var k = ownKeys(mod), i = 0; i < k.length; i++) if (k[i] !== "default") __createBinding(result, mod, k[i]);
        __setModuleDefault(result, mod);
        return result;
    };
})();
Object.defineProperty(exports, "__esModule", { value: true });
exports.agentChat = exports.onAiMessageCreated = exports.onCheckInCreated = void 0;
const firestore_1 = require("firebase-functions/v2/firestore");
const https_1 = require("firebase-functions/v2/https");
const params_1 = require("firebase-functions/params");
const logger = __importStar(require("firebase-functions/logger"));
const app_1 = require("firebase-admin/app");
const firestore_2 = require("firebase-admin/firestore");
const messaging_1 = require("firebase-admin/messaging");
const emergencyProtocol_1 = require("./emergencyProtocol");
const logIaTrace_1 = require("./logIaTrace");
const aiAgent_1 = require("./aiAgent");
const geminiClient_1 = require("./geminiClient");
const firestoreMappers_1 = require("./firestoreMappers");
(0, app_1.initializeApp)();
const geminiApiKey = (0, params_1.defineSecret)("GEMINI_API_KEY");
const COLECCION_USUARIOS = "usuarios";
const SUBCOLECCION_CHECKINS = "checkins";
const SUBCOLECCION_ALERTAS = "alertas";
const SUBCOLECCION_MENSAJES_IA = "mensajesIA";
/**
 * Al crearse un check-in en rojo, envia la notificacion de emergencia y sugiere el
 * contacto de confianza del perfil. La decision vive en `buildEmergencyAction`
 * (funcion pura, cubierta por tests); aqui solo se conecta con Firebase.
 */
exports.onCheckInCreated = (0, firestore_1.onDocumentCreated)(`${COLECCION_USUARIOS}/{userId}/${SUBCOLECCION_CHECKINS}/{checkInId}`, async (event) => {
    const checkInDoc = event.data?.data();
    if (!checkInDoc)
        return;
    const checkIn = (0, firestoreMappers_1.toCheckInRecord)(event.params.checkInId, event.params.userId, checkInDoc);
    const profileSnap = await (0, firestore_2.getFirestore)().collection(COLECCION_USUARIOS).doc(event.params.userId).get();
    if (!profileSnap.exists)
        return;
    const profile = (0, firestoreMappers_1.toUserProfileRecord)(event.params.userId, profileSnap.data() ?? {});
    const action = (0, emergencyProtocol_1.buildEmergencyAction)(checkIn, profile);
    if (!action)
        return;
    if (action.fcmToken) {
        await (0, messaging_1.getMessaging)().send({
            token: action.fcmToken,
            notification: action.notification,
        });
    }
    logger.info("Protocolo de emergencia activado", {
        userId: event.params.userId,
        checkInId: event.params.checkInId,
        suggestedContact: action.suggestedContact?.name,
    });
});
/**
 * Al crearse un mensaje en el chat de IA, lo enlaza con el check-in mas reciente del
 * usuario y escribe el registro de trazabilidad en `logs_ia` (check-in -> nivel de
 * riesgo -> respuesta de IA). El chat no esta atado a un check-in especifico en la UI,
 * asi que se usa el mas reciente como contexto de riesgo vigente.
 */
exports.onAiMessageCreated = (0, firestore_1.onDocumentCreated)(`${COLECCION_USUARIOS}/{userId}/${SUBCOLECCION_MENSAJES_IA}/{aiMessageId}`, async (event) => {
    const aiMessageDoc = event.data?.data();
    if (!aiMessageDoc)
        return;
    const aiMessage = (0, firestoreMappers_1.toAiMessageRecord)(event.params.aiMessageId, event.params.userId, aiMessageDoc);
    const latestCheckInSnap = await (0, firestore_2.getFirestore)()
        .collection(COLECCION_USUARIOS)
        .doc(event.params.userId)
        .collection(SUBCOLECCION_CHECKINS)
        .orderBy("fechaHora", "desc")
        .limit(1)
        .get();
    if (latestCheckInSnap.empty)
        return;
    const latestDoc = latestCheckInSnap.docs[0];
    const checkIn = (0, firestoreMappers_1.toCheckInRecord)(latestDoc.id, event.params.userId, latestDoc.data());
    const trace = (0, logIaTrace_1.buildLogIaTrace)(checkIn, aiMessage);
    await (0, firestore_2.getFirestore)().collection("logs_ia").add(trace);
});
/**
 * Callable que atiende al agente de IA (Gemini + Tools/Function Calling). El uid nunca
 * viaja en el payload: viene siempre de `request.auth`, así que el modelo no puede pedir
 * datos de otro usuario aunque lo intente via los argumentos de una tool.
 */
exports.agentChat = (0, https_1.onCall)({ secrets: [geminiApiKey] }, async (request) => {
    if (!request.auth) {
        throw new https_1.HttpsError("unauthenticated", "Debes iniciar sesion para hablar con el asistente.");
    }
    const userId = request.auth.uid;
    const prompt = request.data?.prompt;
    if (typeof prompt !== "string" || prompt.trim().length === 0) {
        throw new https_1.HttpsError("invalid-argument", "El mensaje (prompt) es obligatorio.");
    }
    const firestore = (0, firestore_2.getFirestore)();
    const checkInsOf = firestore.collection(COLECCION_USUARIOS).doc(userId).collection(SUBCOLECCION_CHECKINS);
    const outcome = await (0, aiAgent_1.runAgentTurn)(userId, prompt, {
        gemini: new geminiClient_1.RestGeminiClient(geminiApiKey.value()),
        readRecentCheckIns: async (uid, limit) => {
            const snap = await checkInsOf.orderBy("fechaHora", "desc").limit(limit).get();
            return snap.docs.map((doc) => (0, firestoreMappers_1.toCheckInRecord)(doc.id, uid, doc.data()));
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
//# sourceMappingURL=index.js.map