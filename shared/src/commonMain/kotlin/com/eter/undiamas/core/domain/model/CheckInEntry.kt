package com.eter.undiamas.core.domain.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class CheckInEntry(
    val id: String,
    val userId: String,
    val answeredAt: Instant,
    val answers: Map<String, String>,
    val riskLevel: RiskLevel,
)

@Serializable
data class RiskAssessment(
    val riskLevel: RiskLevel,
    val recommendation: String,
)
