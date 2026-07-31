import { AiMessageRecord, CheckInRecord, RiskLevel } from "./types";

export interface LogIaTraceEntry {
  checkInId: string;
  userId: string;
  riskLevel: RiskLevel;
  aiMessageId: string;
  createdAt: string;
}

/**
 * Construye el registro de trazabilidad check-in -> nivel de riesgo -> respuesta de IA
 * que alimenta la coleccion `logs_ia`. Lanza error si el mensaje de IA pertenece a otro
 * usuario: los datos de recaida/riesgo nunca deben cruzarse entre UIDs distintos.
 */
export function buildLogIaTrace(
  checkIn: CheckInRecord,
  aiMessage: AiMessageRecord
): LogIaTraceEntry {
  if (aiMessage.userId !== checkIn.userId) {
    throw new Error(
      "El mensaje de IA pertenece a un usuario distinto al del check-in: no se puede registrar la traza."
    );
  }

  return {
    checkInId: checkIn.id,
    userId: checkIn.userId,
    riskLevel: checkIn.riskLevel,
    aiMessageId: aiMessage.id,
    createdAt: new Date().toISOString(),
  };
}
