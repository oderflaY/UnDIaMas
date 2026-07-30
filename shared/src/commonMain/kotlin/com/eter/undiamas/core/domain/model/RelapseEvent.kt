package com.eter.undiamas.core.domain.model

import kotlin.time.Instant
import kotlinx.serialization.Serializable

@Serializable
data class RelapseEvent(
    val id: String,
    val userId: String,
    val occurredAt: Instant,
    val notes: String? = null,
)
