import { CheckInRecord, RiskLevel } from "./types";

export interface AlertRecord {
  userId: string;
  riskLevel: RiskLevel;
  message: string;
  createdAt: string;
}

export interface AgentTool {
  name: string;
  description: string;
  parameters: Record<string, unknown>;
}

export interface FunctionCall {
  name: string;
  args: Record<string, unknown>;
}

export interface GeminiTurnResult {
  text?: string;
  functionCalls?: FunctionCall[];
}

export interface GeminiClient {
  generateContent(params: {
    prompt: string;
    tools: AgentTool[];
    functionResponses?: { name: string; response: unknown }[];
  }): Promise<GeminiTurnResult>;
}

export interface AgentDeps {
  gemini: GeminiClient;
  readRecentCheckIns: (userId: string, limit: number) => Promise<CheckInRecord[]>;
  saveAlert: (alert: AlertRecord) => Promise<string>;
}

export interface AgentTurnOutcome {
  reply: string;
  savedAlertId?: string;
}

/**
 * Tools que el agente puede invocar. `leer_historial_reciente` nunca recibe el uid como
 * parametro del modelo: siempre se usa el uid de la sesion autenticada (ver runAgentTurn),
 * para que el LLM no pueda pedir datos de otro usuario.
 */
export const AGENT_TOOLS: AgentTool[] = [
  {
    name: "leer_historial_reciente",
    description:
      "Lee los check-ins mas recientes del usuario autenticado para entender su contexto de riesgo antes de responder.",
    parameters: {
      type: "object",
      properties: {
        limite: { type: "number", description: "Cuantos check-ins recientes leer (por defecto 5)." },
      },
      required: [],
    },
  },
  {
    name: "guardar_alerta",
    description:
      "Guarda una micro-intervencion o alerta real cuando, tras analizar el historial, el nivel de riesgo es amarillo o rojo.",
    parameters: {
      type: "object",
      properties: {
        nivelRiesgo: { type: "string", enum: ["VERDE", "AMARILLO", "ROJO"] },
        mensaje: { type: "string", description: "Micro-intervencion o alerta a registrar." },
      },
      required: ["nivelRiesgo", "mensaje"],
    },
  },
];

/**
 * Orquesta un turno del agente: primer llamado al modelo, ejecucion de las tools que
 * pida (acotadas siempre al uid de la sesion autenticada) y una segunda llamada con los
 * resultados para obtener la respuesta final. El uid nunca sale del lado del servidor
 * hacia el modelo como parametro editable, así que no hay forma de que el LLM pida
 * datos de otro usuario.
 */
export async function runAgentTurn(
  userId: string,
  prompt: string,
  deps: AgentDeps
): Promise<AgentTurnOutcome> {
  const firstTurn = await deps.gemini.generateContent({ prompt, tools: AGENT_TOOLS });

  if (!firstTurn.functionCalls || firstTurn.functionCalls.length === 0) {
    return { reply: firstTurn.text ?? "" };
  }

  const functionResponses: { name: string; response: unknown }[] = [];
  let savedAlertId: string | undefined;

  for (const call of firstTurn.functionCalls) {
    if (call.name === "leer_historial_reciente") {
      const limite = typeof call.args.limite === "number" ? call.args.limite : 5;
      const historial = await deps.readRecentCheckIns(userId, limite);
      functionResponses.push({ name: call.name, response: { historial } });
    } else if (call.name === "guardar_alerta") {
      const nivelRiesgo = call.args.nivelRiesgo as RiskLevel;
      const mensaje = call.args.mensaje as string;
      savedAlertId = await deps.saveAlert({
        userId,
        riskLevel: nivelRiesgo,
        message: mensaje,
        createdAt: new Date().toISOString(),
      });
      functionResponses.push({ name: call.name, response: { alertId: savedAlertId } });
    }
  }

  const secondTurn = await deps.gemini.generateContent({
    prompt,
    tools: AGENT_TOOLS,
    functionResponses,
  });

  return { reply: secondTurn.text ?? "", savedAlertId };
}
