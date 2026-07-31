import { CheckInRecord, RiskLevel } from "./types";

export interface RiskTrend {
  totalCheckIns: number;
  countByLevel: Record<RiskLevel, number>;
  cleanStreakCheckIns: number;
}

/**
 * Agrega el historico de check-ins en tendencias de riesgo: totales por nivel y la
 * racha de check-ins mas recientes que no llegaron a rojo.
 */
export function aggregateRiskTrends(checkIns: CheckInRecord[]): RiskTrend {
  const countByLevel: Record<RiskLevel, number> = { VERDE: 0, AMARILLO: 0, ROJO: 0 };
  checkIns.forEach((entry) => {
    countByLevel[entry.riskLevel] += 1;
  });

  const chronological = [...checkIns].sort(
    (a, b) => new Date(a.answeredAt).getTime() - new Date(b.answeredAt).getTime()
  );

  let cleanStreakCheckIns = 0;
  for (let i = chronological.length - 1; i >= 0; i--) {
    if (chronological[i].riskLevel === "ROJO") break;
    cleanStreakCheckIns += 1;
  }

  return {
    totalCheckIns: checkIns.length,
    countByLevel,
    cleanStreakCheckIns,
  };
}
