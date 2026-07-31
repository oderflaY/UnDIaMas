export type RiskLevel = "VERDE" | "AMARILLO" | "ROJO";

export interface CheckInRecord {
  id: string;
  userId: string;
  riskLevel: RiskLevel;
  answeredAt: string;
}

export interface TrustedContactRecord {
  name: string;
  phone: string;
  role: string;
}

export interface UserProfileRecord {
  userId: string;
  fcmToken?: string;
  trustedContact?: TrustedContactRecord;
}

export interface AiMessageRecord {
  id: string;
  userId: string;
  riskLevelContext?: RiskLevel;
  sentAt: string;
}
