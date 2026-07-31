package com.eter.undiamas.features.emergencia.domain

/** Las tres etapas del búnker de 15 minutos, en orden. */
enum class UrgeStage(val title: String, val subtitle: String) {
    RESPIRACION(
        title = "Respira",
        subtitle = "Baja la activación del cuerpo antes de pensar en nada más.",
    ),
    REENCUADRE(
        title = "Reencuadra",
        subtitle = "Ahora que el cuerpo bajó, revisemos lo que la mente te está diciendo.",
    ),
    DISTRACCION(
        title = "Redirige",
        subtitle = "Ocupa las manos y la atención; el pico ya va de bajada.",
    ),
}

/** Pregunta de reestructuración cognitiva con la distorsión que ayuda a nombrar. */
data class ReframingPrompt(val distortion: String, val question: String)

/**
 * Sesión de "urge surfing": el impulso agudo suele durar un pico de unos 15 minutos,
 * así que la sesión acompaña ese arco completo en tres etapas de cinco minutos.
 *
 * La clase es pura: recibe los segundos transcurridos y devuelve en qué punto va.
 * Quien la use decide de dónde sale ese tiempo (un reloj real, un test, una pausa).
 */
class UrgeSurfingSession {

    val stageSeconds: Int = 5 * 60
    val totalSeconds: Int = stageSeconds * UrgeStage.entries.size

    val reframingPrompts: List<ReframingPrompt> = listOf(
        ReframingPrompt(
            distortion = "Todo o nada",
            question = "¿De verdad un mal momento arruina todo lo que llevas construido?",
        ),
        ReframingPrompt(
            distortion = "Justificación",
            question = "Si mañana te leyeras esta excusa, ¿te parecería una razón o un permiso?",
        ),
        ReframingPrompt(
            distortion = "Catastrofización",
            question = "¿Qué es lo más probable que pase si no consumes en la próxima hora?",
        ),
        ReframingPrompt(
            distortion = "Memoria selectiva",
            question = "¿Cómo te sentiste realmente la última vez, no al principio sino al día siguiente?",
        ),
    )

    private fun clamp(elapsedSeconds: Int): Int = elapsedSeconds.coerceAtLeast(0)

    /** Etapa correspondiente; si ya se pasó del total se queda en la última. */
    fun stageAt(elapsedSeconds: Int): UrgeStage {
        val index = clamp(elapsedSeconds) / stageSeconds
        return UrgeStage.entries[index.coerceAtMost(UrgeStage.entries.lastIndex)]
    }

    fun isComplete(elapsedSeconds: Int): Boolean = clamp(elapsedSeconds) >= totalSeconds

    fun remainingSeconds(elapsedSeconds: Int): Int =
        (totalSeconds - clamp(elapsedSeconds)).coerceAtLeast(0)

    fun progress(elapsedSeconds: Int): Float =
        (clamp(elapsedSeconds).toFloat() / totalSeconds).coerceIn(0f, 1f)

    /** Progreso dentro de la etapa en curso, para el anillo de la etapa. */
    fun stageProgress(elapsedSeconds: Int): Float {
        val within = clamp(elapsedSeconds) % stageSeconds
        return if (isComplete(elapsedSeconds)) 1f else within.toFloat() / stageSeconds
    }
}
