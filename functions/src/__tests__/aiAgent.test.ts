import { runAgentTurn, GeminiClient, GeminiTurnResult } from "../aiAgent";
import { CheckInRecord } from "../types";

function fakeClient(turns: GeminiTurnResult[]): GeminiClient {
  let call = 0;
  return {
    generateContent: jest.fn(async () => turns[Math.min(call++, turns.length - 1)]),
  };
}

describe("runAgentTurn", () => {
  it("responde directo cuando el modelo no pide ninguna tool", async () => {
    const gemini = fakeClient([{ text: "Todo bien, sigue asi." }]);
    const readRecentCheckIns = jest.fn();
    const saveAlert = jest.fn();

    const outcome = await runAgentTurn("user-1", "hola", { gemini, readRecentCheckIns, saveAlert });

    expect(outcome.reply).toBe("Todo bien, sigue asi.");
    expect(readRecentCheckIns).not.toHaveBeenCalled();
    expect(saveAlert).not.toHaveBeenCalled();
  });

  it("lee el historial reciente del usuario autenticado cuando el modelo pide la tool", async () => {
    const historial: CheckInRecord[] = [
      { id: "c1", userId: "user-1", riskLevel: "AMARILLO", answeredAt: "2026-07-30T10:00:00Z" },
    ];
    const gemini = fakeClient([
      { functionCalls: [{ name: "leer_historial_reciente", args: { limite: 3 } }] },
      { text: "Veo que ayer hubo un detonante, ¿como te sientes hoy?" },
    ]);
    const readRecentCheckIns = jest.fn().mockResolvedValue(historial);
    const saveAlert = jest.fn();

    const outcome = await runAgentTurn("user-1", "no se que hacer", {
      gemini,
      readRecentCheckIns,
      saveAlert,
    });

    expect(readRecentCheckIns).toHaveBeenCalledWith("user-1", 3);
    expect(outcome.reply).toContain("detonante");
    expect(saveAlert).not.toHaveBeenCalled();
  });

  it("guarda una alerta real cuando el modelo determina riesgo rojo", async () => {
    const gemini = fakeClient([
      {
        functionCalls: [
          { name: "guardar_alerta", args: { nivelRiesgo: "ROJO", mensaje: "Activar protocolo de emergencia" } },
        ],
      },
      { text: "Activo tu protocolo de emergencia ahora mismo." },
    ]);
    const readRecentCheckIns = jest.fn();
    const saveAlert = jest.fn().mockResolvedValue("alert-123");

    const outcome = await runAgentTurn("user-1", "quiero consumir ahora", {
      gemini,
      readRecentCheckIns,
      saveAlert,
    });

    expect(saveAlert).toHaveBeenCalledWith(
      expect.objectContaining({ userId: "user-1", riskLevel: "ROJO", message: "Activar protocolo de emergencia" })
    );
    expect(outcome.savedAlertId).toBe("alert-123");
  });

  it("nunca permite que el modelo pida el historial de otro usuario: el uid siempre es el de la sesion", async () => {
    const gemini = fakeClient([
      { functionCalls: [{ name: "leer_historial_reciente", args: { limite: 5, userId: "user-2" } }] },
      { text: "ok" },
    ]);
    const readRecentCheckIns = jest.fn().mockResolvedValue([]);
    const saveAlert = jest.fn();

    await runAgentTurn("user-1", "hola", { gemini, readRecentCheckIns, saveAlert });

    expect(readRecentCheckIns).toHaveBeenCalledWith("user-1", 5);
  });
});
