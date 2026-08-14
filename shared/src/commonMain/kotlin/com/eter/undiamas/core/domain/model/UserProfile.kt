package com.eter.undiamas.core.domain.model

import kotlin.time.Instant
import kotlinx.serialization.Serializable

/** Rol de cada persona de la red de soporte, para saber a quién se está llamando. */
@Serializable
enum class SupportRole(val label: String) {
    PADRINO("Padrino / Sponsor"),
    TERAPEUTA("Terapeuta"),
    FAMILIAR("Familiar"),
    AMISTAD("Amistad"),
}

@Serializable
data class TrustedContact(
    val name: String,
    val phone: String,
    val role: SupportRole = SupportRole.FAMILIAR,
)

/** Meta de ahorro personal que la persona quiere alcanzar con lo que deja de gastar. */
@Serializable
data class SavingsGoal(
    val title: String,
    val targetAmount: Double,
)

@Serializable
data class UserProfile(
    val userId: String,
    val displayName: String,
    val sobrietyStartDate: Instant,
    val recordStreakSeconds: Long = 0,
    val previousDailyExpense: Double = 0.0,
    val trustedContact: TrustedContact? = null,
    /** Contactos adicionales de la red de soporte, más allá del principal. */
    val supportNetwork: List<TrustedContact> = emptyList(),
    /** "Mi por qué": el motivo personal que la persona escribe para recordarse por qué empezó. */
    val personalWhy: String = "",
    val savingsGoal: SavingsGoal? = null,
    /**
     * Los detonantes que esta persona ya sabe que le afectan.
     *
     * Se preguntan al crear la cuenta y sirven para poner primero los suyos en el check-in
     * y al registrar una recaída: quien está mal no debería tener que buscar su detonante
     * entre seis opciones en el orden en que las escribió alguien más.
     */
    val habitualTriggers: List<Trigger> = emptyList(),
    /** Qué está intentando dejar; personaliza mensajes y alertas biométricas. */
    val addiction: AddictionType? = null,
)

/**
 * Los detonantes, con los de esta persona primero.
 *
 * El orden de [Trigger.entries] es el que le pareció bien a quien escribió el enum, y no
 * tiene por qué parecerse al de nadie. Quien marcó "soledad" y "cansancio" al crear su
 * cuenta los encuentra arriba, que es lo que importa cuando se está eligiendo un detonante
 * en mitad de un mal momento y no apetece leer una lista.
 *
 * No se ocultan los demás: alguien puede recaer por algo que nunca le había pasado, y esa
 * es justamente la información que más vale la pena registrar.
 */
fun UserProfile.triggersInOrder(): List<Trigger> =
    habitualTriggers.distinct() + Trigger.entries.filterNot { it in habitualTriggers }
