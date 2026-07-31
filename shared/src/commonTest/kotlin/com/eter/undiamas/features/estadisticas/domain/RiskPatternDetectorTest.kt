package com.eter.undiamas.features.estadisticas.domain

import com.eter.undiamas.core.domain.model.CheckInEntry
import com.eter.undiamas.core.domain.model.RiskLevel
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Instant

class RiskPatternDetectorTest {
    private val detector = RiskPatternDetector()
    private val utc = TimeZone.UTC

    // 1970-01-01 fue jueves; sumar días permite fijar el día de la semana con precisión.
    private fun onDay(dayOffset: Long, hour: Int, level: RiskLevel) = CheckInEntry(
        id = "$dayOffset-$hour-$level",
        userId = "u1",
        answeredAt = Instant.fromEpochSeconds(dayOffset * 86_400 + hour * 3_600),
        answers = emptyMap(),
        riskLevel = level,
    )

    @Test
    fun `sin suficientes registros no se afirma ningun patron`() {
        // Dos viernes en riesgo no bastan: podría ser casualidad.
        val entries = listOf(
            onDay(1, 20, RiskLevel.ROJO),
            onDay(8, 20, RiskLevel.ROJO),
        )

        assertNull(detector.riskiestDay(entries, utc))
    }

    @Test
    fun `detecta el dia de la semana con mas riesgo cuando hay evidencia suficiente`() {
        // Cuatro viernes (offset 1, 8, 15, 22) en riesgo, frente a lunes tranquilos.
        val entries = listOf(
            onDay(1, 20, RiskLevel.ROJO),
            onDay(8, 20, RiskLevel.AMARILLO),
            onDay(15, 20, RiskLevel.ROJO),
            onDay(22, 20, RiskLevel.ROJO),
            onDay(4, 10, RiskLevel.VERDE),
            onDay(11, 10, RiskLevel.VERDE),
        )

        assertEquals(DayOfWeek.FRIDAY, detector.riskiestDay(entries, utc))
    }

    @Test
    fun `un dia con registros pero todos en verde no se marca como riesgoso`() {
        val entries = (0..5).map { onDay(it * 7L + 1, 20, RiskLevel.VERDE) }

        assertNull(detector.riskiestDay(entries, utc))
    }

    @Test
    fun `detecta la hora critica cuando se repite lo suficiente`() {
        val entries = listOf(
            onDay(1, 22, RiskLevel.ROJO),
            onDay(8, 22, RiskLevel.ROJO),
            onDay(15, 22, RiskLevel.AMARILLO),
            onDay(3, 9, RiskLevel.VERDE),
        )

        assertEquals(22, detector.riskiestHour(entries, utc))
    }

    @Test
    fun `sin registros de riesgo no hay hora critica`() {
        val entries = (0..5).map { onDay(it.toLong(), 12, RiskLevel.VERDE) }

        assertNull(detector.riskiestHour(entries, utc))
    }

    @Test
    fun `una lista vacia no rompe la deteccion`() {
        assertNull(detector.riskiestDay(emptyList(), utc))
        assertNull(detector.riskiestHour(emptyList(), utc))
    }

    @Test
    fun `solo alerta cuando hoy coincide con el dia patron`() {
        val entries = listOf(
            onDay(1, 20, RiskLevel.ROJO),
            onDay(8, 20, RiskLevel.AMARILLO),
            onDay(15, 20, RiskLevel.ROJO),
            onDay(22, 20, RiskLevel.ROJO),
        )

        assertEquals(true, detector.shouldWarnToday(entries, utc, DayOfWeek.FRIDAY))
        assertEquals(false, detector.shouldWarnToday(entries, utc, DayOfWeek.MONDAY))
    }
}
