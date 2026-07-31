import { buildLogIaTrace } from "../logIaTrace";
import { AiMessageRecord, CheckInRecord } from "../types";

describe("buildLogIaTrace", () => {
  const checkIn: CheckInRecord = {
    id: "checkin-1",
    userId: "user-1",
    riskLevel: "AMARILLO",
    answeredAt: "2026-07-31T10:00:00Z",
  };

  const aiMessage: AiMessageRecord = {
    id: "ai-msg-1",
    userId: "user-1",
    riskLevelContext: "AMARILLO",
    sentAt: "2026-07-31T10:00:05Z",
  };

  it("enlaza el check-in con la respuesta de IA para trazabilidad completa", () => {
    const trace = buildLogIaTrace(checkIn, aiMessage);

    expect(trace.checkInId).toBe("checkin-1");
    expect(trace.userId).toBe("user-1");
    expect(trace.aiMessageId).toBe("ai-msg-1");
    expect(trace.riskLevel).toBe("AMARILLO");
  });

  it("usa el nivel de riesgo del check-in como fuente de verdad, no el del mensaje de IA", () => {
    const aiMessageDesalineado: AiMessageRecord = { ...aiMessage, riskLevelContext: "ROJO" };

    const trace = buildLogIaTrace(checkIn, aiMessageDesalineado);

    expect(trace.riskLevel).toBe("AMARILLO");
  });

  it("lanza un error si el mensaje de IA pertenece a un usuario distinto al del check-in", () => {
    const aiMessageDeOtroUsuario: AiMessageRecord = { ...aiMessage, userId: "user-2" };

    expect(() => buildLogIaTrace(checkIn, aiMessageDeOtroUsuario)).toThrow();
  });
});
