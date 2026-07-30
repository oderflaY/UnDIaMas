package com.eter.undiamas.core.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class RiskLevel {
    VERDE,
    AMARILLO,
    ROJO,
}
