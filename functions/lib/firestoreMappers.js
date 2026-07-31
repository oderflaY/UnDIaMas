"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.toUserProfileRecord = toUserProfileRecord;
exports.toCheckInRecord = toCheckInRecord;
exports.toAiMessageRecord = toAiMessageRecord;
function toUserProfileRecord(uid, doc) {
    const primero = doc.contactosEmergencia?.[0];
    const trustedContact = primero
        ? { name: primero.nombre, phone: primero.telefono, role: primero.rol }
        : undefined;
    return { userId: uid, fcmToken: doc.fcmToken, trustedContact };
}
function toCheckInRecord(id, uid, doc) {
    return {
        id,
        userId: uid,
        riskLevel: doc.nivelRiesgo ?? "VERDE",
        answeredAt: new Date((doc.fechaHora ?? 0) * 1000).toISOString(),
    };
}
function toAiMessageRecord(id, uid, doc) {
    return {
        id,
        userId: uid,
        riskLevelContext: doc.nivelRiesgoContexto,
        sentAt: new Date((doc.fecha ?? 0) * 1000).toISOString(),
    };
}
//# sourceMappingURL=firestoreMappers.js.map