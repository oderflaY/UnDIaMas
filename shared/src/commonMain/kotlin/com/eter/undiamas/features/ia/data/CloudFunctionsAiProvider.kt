package com.eter.undiamas.features.ia.data

import com.eter.undiamas.core.domain.ai.AiProvider
import com.eter.undiamas.core.domain.model.AiMessage
import com.eter.undiamas.core.domain.model.AiMessageRole
import com.eter.undiamas.core.domain.model.RiskLevel
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.functions.functions
import kotlin.time.Clock
import kotlinx.serialization.Serializable

@Serializable
private data class AgentChatTurn(val role: String, val content: String)

@Serializable
private data class AgentChatRequest(
    val prompt: String,
    val riskLevel: String,
    val history: List<AgentChatTurn>,
)

@Serializable
private data class AgentChatResponse(val reply: String, val savedAlertId: String? = null)

/**
 * Implementacion real del puerto [AiProvider] (Fase 3 · Agentes de IA): llama a la Cloud
 * Function callable `agentChat`, que orquesta Gemini con Tools/Function Calling sobre el
 * historial real de Firestore (ver functions/src/aiAgent.ts). El uid nunca se envia en el
 * payload: la Cloud Function lo toma del token de Auth de la llamada callable.
 *
 * Mismo contrato que [MockAiProvider]: intercambiables via Factory/Facade sin tocar
 * dominio ni presentacion.
 */
class CloudFunctionsAiProvider : AiProvider {
    private val functions = Firebase.functions

    override suspend fun generateResponse(
        prompt: String,
        riskLevel: RiskLevel,
        history: List<AiMessage>,
    ): AiMessage {
        val request = AgentChatRequest(
            prompt = prompt,
            riskLevel = riskLevel.name,
            history = history.takeLast(10).map { AgentChatTurn(role = it.role.name, content = it.content) },
        )
        val result = functions.httpsCallable("agentChat").invoke(request)
        val response = result.data<AgentChatResponse>()

        return AiMessage(
            id = "agent-${Clock.System.now().epochSeconds}-${history.size}",
            userId = Firebase.auth.currentUser?.uid ?: "unknown",
            role = AiMessageRole.ASISTENTE,
            content = response.reply,
            riskLevelContext = riskLevel,
            sentAt = Clock.System.now(),
        )
    }
}
