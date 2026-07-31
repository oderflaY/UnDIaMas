"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const logIaTrace_1 = require("../logIaTrace");
describe("buildLogIaTrace", () => {
    const checkIn = {
        id: "checkin-1",
        userId: "user-1",
        riskLevel: "AMARILLO",
        answeredAt: "2026-07-31T10:00:00Z",
    };
    const aiMessage = {
        id: "ai-msg-1",
        userId: "user-1",
        riskLevelContext: "AMARILLO",
        sentAt: "2026-07-31T10:00:05Z",
    };
    it("enlaza el check-in con la respuesta de IA para trazabilidad completa", () => {
        const trace = (0, logIaTrace_1.buildLogIaTrace)(checkIn, aiMessage);
        expect(trace.checkInId).toBe("checkin-1");
        expect(trace.userId).toBe("user-1");
        expect(trace.aiMessageId).toBe("ai-msg-1");
        expect(trace.riskLevel).toBe("AMARILLO");
    });
    it("usa el nivel de riesgo del check-in como fuente de verdad, no el del mensaje de IA", () => {
        const aiMessageDesalineado = { ...aiMessage, riskLevelContext: "ROJO" };
        const trace = (0, logIaTrace_1.buildLogIaTrace)(checkIn, aiMessageDesalineado);
        expect(trace.riskLevel).toBe("AMARILLO");
    });
    it("lanza un error si el mensaje de IA pertenece a un usuario distinto al del check-in", () => {
        const aiMessageDeOtroUsuario = { ...aiMessage, userId: "user-2" };
        expect(() => (0, logIaTrace_1.buildLogIaTrace)(checkIn, aiMessageDeOtroUsuario)).toThrow();
    });
});
//# sourceMappingURL=logIaTrace.test.js.map