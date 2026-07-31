"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const emergencyProtocol_1 = require("../emergencyProtocol");
describe("buildEmergencyAction", () => {
    const baseCheckIn = {
        id: "checkin-1",
        userId: "user-1",
        riskLevel: "ROJO",
        answeredAt: "2026-07-31T10:00:00Z",
    };
    const baseProfile = {
        userId: "user-1",
        fcmToken: "token-abc",
        trustedContact: { name: "Ana", phone: "+525500000000", role: "FAMILIAR" },
    };
    it("dispara el protocolo de emergencia y sugiere el contacto de confianza cuando el semaforo esta en rojo", () => {
        const action = (0, emergencyProtocol_1.buildEmergencyAction)(baseCheckIn, baseProfile);
        expect(action).not.toBeNull();
        expect(action?.shouldNotify).toBe(true);
        expect(action?.fcmToken).toBe("token-abc");
        expect(action?.suggestedContact).toEqual(baseProfile.trustedContact);
        expect(action?.notification.title.length).toBeGreaterThan(0);
        expect(action?.notification.body.length).toBeGreaterThan(0);
    });
    it("no lanza error y omite el contacto sugerido si el usuario no tiene contacto de confianza registrado", () => {
        const profileSinContacto = { userId: "user-1", fcmToken: "token-abc" };
        const action = (0, emergencyProtocol_1.buildEmergencyAction)(baseCheckIn, profileSinContacto);
        expect(action?.shouldNotify).toBe(true);
        expect(action?.suggestedContact).toBeUndefined();
    });
    it("no dispara el protocolo cuando el semaforo esta en amarillo", () => {
        const checkInAmarillo = { ...baseCheckIn, riskLevel: "AMARILLO" };
        expect((0, emergencyProtocol_1.buildEmergencyAction)(checkInAmarillo, baseProfile)).toBeNull();
    });
    it("no dispara el protocolo cuando el semaforo esta en verde", () => {
        const checkInVerde = { ...baseCheckIn, riskLevel: "VERDE" };
        expect((0, emergencyProtocol_1.buildEmergencyAction)(checkInVerde, baseProfile)).toBeNull();
    });
});
//# sourceMappingURL=emergencyProtocol.test.js.map