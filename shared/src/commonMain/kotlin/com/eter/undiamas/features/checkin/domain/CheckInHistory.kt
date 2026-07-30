package com.eter.undiamas.features.checkin.domain

import com.eter.undiamas.core.domain.model.CheckInEntry
import com.eter.undiamas.core.domain.model.RiskLevel
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Convierte los check-ins sueltos en el registro diario: qué día quedó registrado y
 * con qué nivel de riesgo. Es lo que alimenta el calendario de días limpios.
 */
class CheckInHistory {

    // Severidad explícita para no depender del orden de declaración del enum.
    private fun severity(level: RiskLevel): Int = when (level) {
        RiskLevel.VERDE -> 0
        RiskLevel.AMARILLO -> 1
        RiskLevel.ROJO -> 2
    }

    /**
     * Nivel de riesgo por día calendario. Si un día tuvo varios check-ins gana el más
     * severo: un momento en rojo marca el día aunque después se haya estabilizado.
     */
    fun byDay(entries: List<CheckInEntry>, timeZone: TimeZone): Map<LocalDate, RiskLevel> =
        entries.groupBy { it.answeredAt.toLocalDateTime(timeZone).date }
            .mapValues { (_, dayEntries) -> dayEntries.maxBy { severity(it.riskLevel) }.riskLevel }

    /** Días distintos con al menos un check-in, sin importar cuántos hubo ese día. */
    fun registeredDays(entries: List<CheckInEntry>, timeZone: TimeZone): Int =
        byDay(entries, timeZone).size

    /** Días registrados que no llegaron a semáforo rojo. */
    fun cleanDays(entries: List<CheckInEntry>, timeZone: TimeZone): Int =
        byDay(entries, timeZone).count { (_, level) -> level != RiskLevel.ROJO }

    fun hasCheckInOn(entries: List<CheckInEntry>, date: LocalDate, timeZone: TimeZone): Boolean =
        byDay(entries, timeZone).containsKey(date)
}
