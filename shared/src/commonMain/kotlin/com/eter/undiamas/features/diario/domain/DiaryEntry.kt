package com.eter.undiamas.features.diario.domain

import kotlin.time.Instant
import kotlinx.serialization.Serializable

@Serializable
data class DiaryEntry(
    val id: String,
    val userId: String,
    val createdAt: Instant,
    val text: String,
)
