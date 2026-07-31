package com.eter.undiamas.features.estadisticas.domain

import com.eter.undiamas.core.domain.model.CheckInEntry
import com.eter.undiamas.core.domain.model.RiskLevel
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Instant

class RiskInsightsTest {
    private val insights = RiskInsights()
    private val utc = TimeZone.UTC

    private fun entry(hour: Int, level: RiskLevel, dayOffset: Long = 0) = CheckInEntry(
        id = "$dayOffset-$hour-$level",
        userId = "u1",
        answeredAt = Instant.fromEpochSeconds(dayOffset * 86_400 + hour * 3_600L),
        answers = emptyMap(),
        riskLevel = level,
    )

    @Test
    fun `la hora critica es aquella con mas check-ins en riesgo`() {
        val entries = listOf(
            entry(22, RiskLevel.ROJO),
            entry(22, RiskLevel.AMARILLO, dayOffset = 1),
            entry(9, RiskLevel.AMARILLO, dayOffset = 2),
        )

        assertEquals(22, insights.riskiestHour(entries, utc))
    }

    @Test
    fun `los check-ins en verde no definen la hora critica`() {
        val entries = listOf(
            entry(8, RiskLevel.VERDE),
            entry(8, RiskLevel.VERDE, dayOffset = 1),
            entry(23, RiskLevel.ROJO, dayOffset = 2),
        )

        assertEquals(23, insights.riskiestHour(entries, utc))
    }

    @Test
    fun `sin check-ins en riesgo no hay hora critica que reportar`() {
        val entries = listOf(entry(8, RiskLevel.VERDE), entry(10, RiskLevel.VERDE, dayOffset = 1))

        assertNull(insights.riskiestHour(entries, utc))
        assertNull(insights.riskiestHour(emptyList(), utc))
    }

    @Test
    fun `cuenta como impulso superado cada check-in en riesgo sin recaida posterior`() {
        val entries = listOf(
            entry(22, RiskLevel.ROJO),
            entry(20, RiskLevel.AMARILLO, dayOffset = 1),
            entry(9, RiskLevel.VERDE, dayOffset = 2),
        )

        assertEquals(2, insights.urgesOvercome(entries))
    }

    @Test
    fun `el reparto por hora agrupa los conteos de riesgo`() {
        val entries = listOf(
            entry(22, RiskLevel.ROJO),
            entry(22, RiskLevel.ROJO, dayOffset = 1),
            entry(7, RiskLevel.AMARILLO, dayOffset = 2),
        )

        val byHour = insights.riskByHour(entries, utc)

        assertEquals(2, byHour[22])
        assertEquals(1, byHour[7])
        assertEquals(0, byHour[12])
    }
}
