package com.eter.undiamas.features.sobriedad.domain

/** Placa de sobriedad que la persona va desbloqueando conforme sostiene la racha. */
data class Milestone(val days: Int, val title: String)

/**
 * Mapa de hitos: sirve tanto para la galería de insignias del perfil como para
 * el "te faltan N días" de la pantalla de sobriedad.
 */
class Milestones {
    val all: List<Milestone> = listOf(
        Milestone(1, "Primer día"),
        Milestone(7, "Una semana"),
        Milestone(14, "Dos semanas"),
        Milestone(30, "Un mes"),
        Milestone(60, "Dos meses"),
        Milestone(90, "Tres meses"),
        Milestone(180, "Medio año"),
        Milestone(365, "Un año"),
    )

    /** Hitos ya conseguidos con [currentDays] días de racha. */
    fun reached(currentDays: Long): List<Milestone> = all.filter { currentDays >= it.days }

    /** Siguiente hito por alcanzar, o null si ya se superaron todos. */
    fun next(currentDays: Long): Milestone? = all.firstOrNull { currentDays < it.days }

    /** Días que faltan para el siguiente hito, o null si ya no queda ninguno. */
    fun daysUntilNext(currentDays: Long): Long? = next(currentDays)?.let { it.days - currentDays }
}
