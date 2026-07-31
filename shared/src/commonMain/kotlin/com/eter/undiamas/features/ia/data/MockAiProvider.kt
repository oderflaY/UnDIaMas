package com.eter.undiamas.features.ia.data

import com.eter.undiamas.core.domain.ai.AiProvider
import com.eter.undiamas.core.domain.model.AiMessage
import com.eter.undiamas.core.domain.model.AiMessageRole
import com.eter.undiamas.core.domain.model.RiskLevel
import kotlin.time.Clock

/**
 * Implementación de marcador de posición del puerto [AiProvider] mientras Fase 3 integra
 * Firebase AI Logic. Se reemplazará detrás de la misma interfaz (Factory/Facade) sin tocar
 * el resto de la capa de dominio ni la presentación.
 *
 * El tono cambia según el nivel de riesgo: alegre en verde, reflexivo en amarillo y de
 * contención directa en rojo.
 */
class MockAiProvider : AiProvider {

    private val verde = listOf(
        "Me alegra leerte así. ¿Qué fue lo que mejor te funcionó hoy?",
        "Vas sostenido. Pongámosle nombre a una meta pequeña para esta semana.",
        "Eso que estás haciendo bien no es suerte: es tu trabajo. Sigamos.",
    )

    private val amarillo = listOf(
        "Gracias por contarlo. Respiremos un momento: ¿qué apareció justo antes de sentirte así?",
        "Ese detonante ya lo conoces. ¿Qué alternativa te ha servido otras veces?",
        "Vamos a bajar el ruido. Dime una cosa concreta que puedas hacer en los próximos 10 minutos.",
    )

    private val rojo = listOf(
        "Estoy aquí contigo. No estás solo/a en este momento. Vamos paso a paso.",
        "Primero tu seguridad: respira conmigo y, si puedes, llama a tu contacto de confianza.",
        "Este impulso va a bajar. Quédate conmigo mientras pasa; no tienes que resolverlo todo ahora.",
    )

    override suspend fun generateResponse(
        prompt: String,
        riskLevel: RiskLevel,
        history: List<AiMessage>,
    ): AiMessage {
        val pool = when (riskLevel) {
            RiskLevel.VERDE -> verde
            RiskLevel.AMARILLO -> amarillo
            RiskLevel.ROJO -> rojo
        }
        // Rota las respuestas según la conversación para que no se repita siempre la misma.
        val content = pool[(history.size / 2) % pool.size]

        return AiMessage(
            id = "mock-${history.size}",
            userId = "demo-user",
            role = AiMessageRole.ASISTENTE,
            content = content,
            riskLevelContext = riskLevel,
            sentAt = Clock.System.now(),
        )
    }
}
