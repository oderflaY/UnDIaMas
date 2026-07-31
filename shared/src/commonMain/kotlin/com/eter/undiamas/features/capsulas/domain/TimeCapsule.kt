package com.eter.undiamas.features.capsulas.domain

import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

/**
 * Mensaje que la persona se escribe a sí misma en un buen día para leerlo más adelante,
 * cuando le cueste recordar por qué empezó.
 */
@Serializable
data class TimeCapsule(
    val id: String,
    val userId: String,
    val title: String,
    val message: String,
    val createdOn: LocalDate,
    val unlockOn: LocalDate,
)

/** Reglas de apertura de las cápsulas: qué está disponible y cuánto falta. */
class TimeCapsuleVault {

    fun isUnlocked(capsule: TimeCapsule, today: LocalDate): Boolean = today >= capsule.unlockOn

    /**
     * Días que faltan para poder abrirla. Cero si ya se puede abrir.
     * Se calcula sobre el número de día absoluto para no depender de meses de distinto largo.
     */
    fun daysUntilUnlock(capsule: TimeCapsule, today: LocalDate): Int {
        val diff = capsule.unlockOn.toEpochDays() - today.toEpochDays()
        return if (diff <= 0) 0 else diff.toInt()
    }

    /** Cápsulas ya disponibles, de la más reciente en desbloquearse a la más antigua. */
    fun unlocked(capsules: List<TimeCapsule>, today: LocalDate): List<TimeCapsule> =
        capsules.filter { isUnlocked(it, today) }.sortedByDescending { it.unlockOn }

    /** Cápsulas aún cerradas, ordenadas por la que se abre primero. */
    fun locked(capsules: List<TimeCapsule>, today: LocalDate): List<TimeCapsule> =
        capsules.filterNot { isUnlocked(it, today) }.sortedBy { it.unlockOn }
}
