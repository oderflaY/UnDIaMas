import { CheckInRecord, UserProfileRecord, TrustedContactRecord } from "./types";

export interface EmergencyAction {
  shouldNotify: true;
  fcmToken?: string;
  notification: { title: string; body: string };
  suggestedContact?: TrustedContactRecord;
}

/**
 * Decide si un check-in debe disparar el protocolo de emergencia y con qué datos.
 * Funcion pura (sin Firebase Admin) para poder probarla sin emulador.
 */
export function buildEmergencyAction(
  checkIn: CheckInRecord,
  profile: UserProfileRecord
): EmergencyAction | null {
  if (checkIn.riskLevel !== "ROJO") return null;

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
