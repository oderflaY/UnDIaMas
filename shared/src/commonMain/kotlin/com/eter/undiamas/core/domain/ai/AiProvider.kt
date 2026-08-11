package com.eter.undiamas.core.domain.ai

import com.eter.undiamas.core.domain.model.AiMessage
import com.eter.undiamas.core.domain.model.RiskLevel

/**
 * Puerto del proveedor de IA conversacional. La implementación real (el asistente del
 * backend, u otro proveedor) se resuelve mediante Factory/Facade en la capa data.
 */
interface AiProvider {
    suspend fun generateResponse(
        prompt: String,
        riskLevel: RiskLevel,
        history: List<AiMessage>,
    ): AiMessage
}
