package com.eter.undiamas.features.checkin.domain

import com.eter.undiamas.core.domain.model.CheckInEntry
import com.eter.undiamas.core.domain.model.RiskLevel
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CheckInHistoryTest {
    private val history = CheckInHistory()
    private val utc = TimeZone.UTC

    private fun entry(atSeconds: Long, level: RiskLevel) = CheckInEntry(
        id = "$atSeconds-$level",
        userId = "u1",
        answeredAt = Instant.fromEpochSeconds(atSeconds),
        answers = emptyMap(),
        riskLevel = level,
    )

    @Test
    fun `agrupa cada check-in en su dia calendario`() {
        val day0 = entry(0, RiskLevel.VERDE)
        val day1 = entry(86_400, RiskLevel.AMARILLO)

        val byDay = history.byDay(listOf(day0, day1), utc)

        assertEquals(RiskLevel.VERDE, byDay[LocalDate(1970, 1, 1)])
        assertEquals(RiskLevel.AMARILLO, byDay[LocalDate(1970, 1, 2)])
    }

    @Test
    fun `si un dia tiene varios check-ins se conserva el nivel mas severo`() {
        val manana = entry(3_600, RiskLevel.VERDE)
        val tarde = entry(50_000, RiskLevel.ROJO)
        val noche = entry(80_000, RiskLevel.AMARILLO)

        val byDay = history.byDay(listOf(manana, tarde, noche), utc)

        assertEquals(RiskLevel.ROJO, byDay[LocalDate(1970, 1, 1)])
    }

    @Test
    fun `cuenta los dias distintos con registro y no los check-ins totales`() {
        val entries = listOf(
            entry(0, RiskLevel.VERDE),
            entry(3_600, RiskLevel.VERDE),
            entry(86_400, RiskLevel.VERDE),
        )

        assertEquals(2, history.registeredDays(entries, utc))
    }

    @Test
    fun `cuenta como limpios los dias sin semaforo rojo`() {
        val entries = listOf(
            entry(0, RiskLevel.VERDE),
            entry(86_400, RiskLevel.AMARILLO),
            entry(172_800, RiskLevel.ROJO),
        )

        assertEquals(2, history.cleanDays(entries, utc))
    }

    @Test
    fun `sabe si ya hay registro para una fecha dada`() {
        val entries = listOf(entry(0, RiskLevel.VERDE))

        assertTrue(history.hasCheckInOn(entries, LocalDate(1970, 1, 1), utc))
        assertFalse(history.hasCheckInOn(entries, LocalDate(1970, 1, 2), utc))
    }

    @Test
    fun `sin check-ins el historial queda vacio`() {
        assertEquals(emptyMap(), history.byDay(emptyList(), utc))
        assertEquals(0, history.registeredDays(emptyList(), utc))
        assertEquals(0, history.cleanDays(emptyList(), utc))
    }
}
