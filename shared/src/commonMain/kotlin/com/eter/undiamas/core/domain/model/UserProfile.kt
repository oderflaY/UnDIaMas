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
)
