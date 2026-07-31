import { aggregateRiskTrends } from "../statsAggregation";
import { CheckInRecord } from "../types";

function checkIn(id: string, riskLevel: CheckInRecord["riskLevel"], answeredAt: string): CheckInRecord {
  return { id, userId: "user-1", riskLevel, answeredAt };
}

describe("aggregateRiskTrends", () => {
  it("devuelve totales en cero cuando no hay check-ins", () => {
    const trend = aggregateRiskTrends([]);

    expect(trend.totalCheckIns).toBe(0);
    expect(trend.countByLevel).toEqual({ VERDE: 0, AMARILLO: 0, ROJO: 0 });
    expect(trend.cleanStreakCheckIns).toBe(0);
  });

  it("cuenta los check-ins por nivel de riesgo", () => {
    const checkIns = [
      checkIn("1", "VERDE", "2026-07-01T08:00:00Z"),
      checkIn("2", "AMARILLO", "2026-07-02T08:00:00Z"),
      checkIn("3", "ROJO", "2026-07-03T08:00:00Z"),
      checkIn("4", "VERDE", "2026-07-04T08:00:00Z"),
    ];

    const trend = aggregateRiskTrends(checkIns);

    expect(trend.totalCheckIns).toBe(4);
    expect(trend.countByLevel).toEqual({ VERDE: 2, AMARILLO: 1, ROJO: 1 });
  });

  it("calcula la racha de check-ins recientes que no llegaron a rojo, sin importar el orden de entrada", () => {
    const checkIns = [
      checkIn("4", "AMARILLO", "2026-07-04T08:00:00Z"),
      checkIn("1", "ROJO", "2026-07-01T08:00:00Z"),
      checkIn("3", "VERDE", "2026-07-03T08:00:00Z"),
      checkIn("2", "VERDE", "2026-07-02T08:00:00Z"),
    ];

    const trend = aggregateRiskTrends(checkIns);

    expect(trend.cleanStreakCheckIns).toBe(3);
  });

  it("la racha limpia es igual al total cuando nunca hubo un rojo", () => {
    const checkIns = [
      checkIn("1", "VERDE", "2026-07-01T08:00:00Z"),
      checkIn("2", "AMARILLO", "2026-07-02T08:00:00Z"),
    ];

    expect(aggregateRiskTrends(checkIns).cleanStreakCheckIns).toBe(2);
  });
});
