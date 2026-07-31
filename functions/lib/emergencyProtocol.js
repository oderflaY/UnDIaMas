"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.buildEmergencyAction = buildEmergencyAction;
/**
 * Decide si un check-in debe disparar el protocolo de emergencia y con qué datos.
 * Funcion pura (sin Firebase Admin) para poder probarla sin emulador.
 */
function buildEmergencyAction(checkIn, profile) {
    if (checkIn.riskLevel !== "ROJO")
        return null;
    return {
        shouldNotify: true,
        fcmToken: profile.fcmToken,
        notification: {
            title: "Estamos contigo, un paso a la vez",
            body: "Detectamos un momento de riesgo alto. Respira: aqui tienes a tu persona de confianza.",
        },
        suggestedContact: profile.trustedContact,
    };
}
//# sourceMappingURL=emergencyProtocol.js.map