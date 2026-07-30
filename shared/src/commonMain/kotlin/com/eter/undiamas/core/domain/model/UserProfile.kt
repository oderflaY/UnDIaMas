package com.eter.undiamas.core.domain.model

import kotlin.time.Instant
import kotlinx.serialization.Serializable

@Serializable
data class TrustedContact(
    val name: String,
    val phone: String,
)

@Serializable
data class UserProfile(
    val userId: String,
    val displayName: String,
    val sobrietyStartDate: Instant,
    val recordStreakSeconds: Long = 0,
    val previousDailyExpense: Double = 0.0,
    val trustedContact: TrustedContact? = null,
)
