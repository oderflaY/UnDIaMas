package com.eter.undiamas.features.ia.data

import com.eter.undiamas.core.data.api.ApiException
import com.eter.undiamas.core.data.api.UnDiaMasApi
import com.eter.undiamas.core.domain.ai.AiProvider
import com.eter.undiamas.core.domain.model.AiMessage
import com.eter.undiamas.core.domain.model.AiMessageRole
import com.eter.undiamas.core.domain.model.RiskLevel
import kotlin.time.Clock

/**
 * Asistente conversacional servido por el backend propio.
 *
 * [riskLevel] e [history] se reciben por el contrato de [AiProvider] pero no se mandan: el
 * servidor los lee de la base con el id del token. Eso es mejor que mandarlos — el tono se
 * adapta al riesgo real registrado, no al que la app crea tener, y la conversacion
 * sobrevive a reinstalar.
 */
class ApiAiProvider(
    private val api: UnDiaMasApi,
) : AiProvider {

    override suspend fun generateResponse(
        prompt: String,
        riskLevel: RiskLevel,
        history: List<AiMessage>,
    ): AiMessage {
        val reply = try {
            api.chat(prompt).reply
        } catch (error: ApiException) {
            // El asistente puede faltar (servidor sin clave de IA) o estar saturado. Nada de
            // eso justifica dejar sin respuesta a alguien que escribio pidiendo ayuda.
            fallbackFor(error)
        }

        return AiMessage(
            id = "",
            userId = "",
            role = AiMessageRole.ASISTENTE,
            content = reply,
            riskLevelContext = riskLevel,
            sentAt = Clock.System.now(),
        )
    }

    private fun fallbackFor(error: ApiException): String = when {
        error.status == 404 ->
            "El asistente no está configurado en el servidor todavía. Mientras tanto, " +
                "el botón de emergencia y tus contactos de confianza siguen funcionando."
        error.status == 429 ->
            "Escribiste varios mensajes muy seguidos. Espera un momento y vuelve a " +
                "intentarlo; aquí sigo."
        else ->
            "No pude conectar con el asistente ahora mismo. Si esto es urgente, usa el " +
                "botón de emergencia."
    }
}
