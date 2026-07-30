package com.eter.undiamas.features.ia.data

import com.eter.undiamas.core.domain.ai.AiProvider
import com.eter.undiamas.core.domain.model.AiMessage
import com.eter.undiamas.core.domain.model.AiMessageRole
import com.eter.undiamas.core.domain.model.RiskLevel
import kotlinx.datetime.Clock

/**
 * Implementación de marcador de posición del puerto [AiProvider] mientras Fase 3 integra
 * Firebase AI Logic. Se reemplazará detrás de la misma interfaz (Factory/Facade) sin tocar
 * el resto de la capa de dominio ni la presentación.
 */
class MockAiProvider : AiProvider {
    override suspend fun generateResponse(
        prompt: String,
        riskLevel: RiskLevel,
        history: List<AiMessage>,
    ): AiMessage {
        val content = when (riskLevel) {
            RiskLevel.VERDE -> "Me alegra leer eso. Sigue reforzando lo que ya te está funcionando hoy."
            RiskLevel.AMARILLO -> "Gracias por contarlo. Pensemos juntos una alternativa ante ese detonante."
            RiskLevel.ROJO -> "Estoy aquí contigo. Vamos a activar tu protocolo de emergencia ahora mismo."
        }

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
