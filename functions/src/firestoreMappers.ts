import { AiMessageRecord, CheckInRecord, RiskLevel, TrustedContactRecord, UserProfileRecord } from "./types";

/** Forma real de cada entrada de `/usuarios/{uid}` en `contactosEmergencia[]`. */
interface ContactoEmergenciaDoc {
  nombre: string;
  telefono: string;
  rol: string;
}

/** Forma real del documento `/usuarios/{uid}` en Firestore (espejo de UsuarioDoc en Kotlin). */
export interface UsuarioDoc {
  alias?: string;
  fechaInicioSobriedad?: number;
  recordRachaSegundos?: number;
  gastoDiarioEstimado?: number;
  contactosEmergencia?: ContactoEmergenciaDoc[];
  fcmToken?: string;
}

/** Forma real de cada documento en `/usuarios/{uid}/checkins` (espejo de CheckInDoc en Kotlin). */
export interface CheckInDoc {
  nivelCraving?: number;
  estadoAnimo?: string;
  gatillos?: string[];
  nivelRiesgo?: RiskLevel;
  fechaHora?: number;
  nota?: string;
}

/** Forma real de cada documento en `/usuarios/{uid}/mensajesIA` (espejo de AiMessageDoc en Kotlin). */
export interface AiMessageDoc {
  rol?: string;
  contenido?: string;
  nivelRiesgoContexto?: RiskLevel;
  fecha?: number;
}

export function toUserProfileRecord(uid: string, doc: UsuarioDoc): UserProfileRecord {
  const primero = doc.contactosEmergencia?.[0];
  const trustedContact: TrustedContactRecord | undefined = primero
    ? { name: primero.nombre, phone: primero.telefono, role: primero.rol }
    : undefined;
  return { userId: uid, fcmToken: doc.fcmToken, trustedContact };
}

export function toCheckInRecord(id: string, uid: string, doc: CheckInDoc): CheckInRecord {
  return {
    id,
    userId: uid,
    riskLevel: doc.nivelRiesgo ?? "VERDE",
    answeredAt: new Date((doc.fechaHora ?? 0) * 1000).toISOString(),
  };
}

export function toAiMessageRecord(id: string, uid: string, doc: AiMessageDoc): AiMessageRecord {
  return {
    id,
    userId: uid,
    riskLevelContext: doc.nivelRiesgoContexto,
    sentAt: new Date((doc.fecha ?? 0) * 1000).toISOString(),
  };
}
