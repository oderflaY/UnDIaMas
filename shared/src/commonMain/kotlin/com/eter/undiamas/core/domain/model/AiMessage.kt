package com.eter.undiamas.core.domain.model

import kotlin.time.Instant
import kotlinx.serialization.Serializable

@Serializable
enum class AiMessageRole {
    USUARIO,
    ASISTENTE,
}

@Serializable
data class AiMessage(
    val id: String,
    val userId: String,
    val role: AiMessageRole,
    val content: String,
    val riskLevelContext: RiskLevel? = null,
    val sentAt: Instant,
)
