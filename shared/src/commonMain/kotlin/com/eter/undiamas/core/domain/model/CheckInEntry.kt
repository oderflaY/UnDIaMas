package com.eter.undiamas.core.domain.model

import kotlin.time.Instant
import kotlinx.serialization.Serializable

/** Detonantes que la persona puede marcar cuando reconoce que algo la activó. */
@Serializable
enum class Trigger(val label: String, val emoji: String) {
    ESTRES("Estrés", "😰"),
    SOLEDAD("Soledad", "🌑"),
    CANSANCIO("Cansancio", "😴"),
    ABURRIMIENTO("Aburrimiento", "🥱"),
    SOCIAL("Social", "🎉"),
    TRABAJO("Trabajo", "💼"),
}

@Serializable
data class CheckInEntry(
    val id: String,
    val userId: String,
    val answeredAt: Instant,
    val answers: Map<String, String>,
    val riskLevel: RiskLevel,
    val triggers: Set<Trigger> = emptySet(),
    /** Intensidad del impulso declarada del 1 al 10; 0 cuando no aplicó la pregunta. */
    val urgeIntensity: Int = 0,
    val note: String = "",
)

@Serializable
data class RiskAssessment(
    val riskLevel: RiskLevel,
    val recommendation: String,
)

/** Registro rápido de ánimo, sin pasar por el check-in completo. */
@Serializable
enum class Mood(val label: String, val emoji: String) {
    MUY_MAL("Muy mal", "😞"),
    MAL("Mal", "🙁"),
    NEUTRAL("Neutral", "😐"),
    BIEN("Bien", "🙂"),
    MUY_BIEN("Muy bien", "😄"),
}

@Serializable
data class MoodEntry(
    val id: String,
    val userId: String,
    val mood: Mood,
    val registeredAt: Instant,
)
