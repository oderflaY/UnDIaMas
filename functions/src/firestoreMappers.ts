import { AiMessageRecord, CheckInRecord, RiskLevel, TrustedContactRecord, UserProfileRecord } from "./types";

/**
 * Espejo en TypeScript del esquema PLANO real de Firestore (`/users`, `/check_ins`,
 * `/ai_messages`), el mismo que modela ClinicalDocs.kt en la app.
 *
 * Las fechas llegan como Timestamp del Admin SDK, no como numero, porque asi estan
 * guardadas. Todo campo es opcional: los documentos se crearon en momentos distintos y
 * no todos traen los mismos campos.
 */
interface FirestoreTimestampLike {
  toDate?: () => Date;
  _seconds?: number;
  seconds?: number;
}

/**
 * Firestore guarda el semaforo en ingles y el dominio lo nombra en espanol. Un codigo
 * desconocido cae a VERDE a proposito: inventar un ROJO por un dato corrupto activaria
 * el protocolo de emergencia de alguien sin motivo.
 */
export function toRiskLevel(code: unknown): RiskLevel {
  switch (String(code ?? "").toUpperCase()) {
    case "RED":
    case "ROJO":
      return "ROJO";
    case "YELLOW":
    case "AMARILLO":
      return "AMARILLO";
    default:
      return "VERDE";
  }
}

/** Timestamp de Firestore -> ISO 8601. Una fecha ausente cae a epoch, nunca a `Invalid Date`. */
export function toIsoDate(value: unknown): string {
  const ts = value as FirestoreTimestampLike | undefined;
  if (ts?.toDate) return ts.toDate().toISOString();
  const seconds = ts?._seconds ?? ts?.seconds;
  if (typeof seconds === "number") return new Date(seconds * 1000).toISOString();
  if (typeof value === "number") return new Date(value * 1000).toISOString();
  return new Date(0).toISOString();
}

/** Contacto de confianza tal como lo guarda la app dentro de `/users/{uid}`. */
interface ContactoEmergenciaDoc {
  nombre?: string;
  telefono?: string;
  rol?: string;
}

/** Documento de `/users/{uid}`. */
export interface UserDoc {
  uid?: string;
  displayName?: string;
  email?: string;
  role?: string;
  contactosEmergencia?: ContactoEmergenciaDoc[];
  fcmToken?: string;
}

/** Documento de `/check_ins/{id}`. */
export interface CheckInDoc {
  userId?: string;
  riskLevel?: string;
  cravingLevel?: number;
  mood?: string;
  triggers?: string[];
  note?: string;
  timestamp?: unknown;
}

/** Documento de `/ai_messages/{id}`. */
export interface AiMessageDoc {
  userId?: string;
  role?: string;
  content?: string;
  riskLevelContext?: string;
  timestamp?: unknown;
}

export function toUserProfileRecord(uid: string, doc: UserDoc): UserProfileRecord {
  const primero = doc.contactosEmergencia?.[0];
  const trustedContact: TrustedContactRecord | undefined = primero
    ? { name: primero.nombre ?? "", phone: primero.telefono ?? "", role: primero.rol ?? "" }
    : undefined;
  return { userId: uid, fcmToken: doc.fcmToken, trustedContact };
}

export function toCheckInRecord(id: string, uid: string, doc: CheckInDoc): CheckInRecord {
  return {
    id,
    userId: uid,
    riskLevel: toRiskLevel(doc.riskLevel),
    answeredAt: toIsoDate(doc.timestamp),
  };
}

export function toAiMessageRecord(id: string, uid: string, doc: AiMessageDoc): AiMessageRecord {
  return {
    id,
    userId: uid,
    riskLevelContext: doc.riskLevelContext ? toRiskLevel(doc.riskLevelContext) : undefined,
    sentAt: toIsoDate(doc.timestamp),
  };
}
