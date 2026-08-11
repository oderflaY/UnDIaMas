package com.eter.undiamas.core.data.api

import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.prepareRequest
import io.ktor.client.request.url
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.HttpMethod
import io.ktor.utils.io.readUTF8Line
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement

/**
 * Aviso que empuja el servidor por la conexion abierta.
 *
 * Sustituye a las notificaciones push: mientras la app este abierta, un semaforo en rojo
 * llega en el momento. Con la app cerrada no llega nada, pero tampoco se pierde — queda
 * en `/v1/alerts`, que es lo que hay que leer al volver.
 */
@Serializable
data class ServerEvent(
    val type: String = "",
    val payload: JsonElement? = null,
    val createdAt: String? = null,
)

object ServerEventType {
    const val ALERT = "alert"
    const val TRAFFIC_LIGHT = "traffic_light"
    const val CHECK_IN_REMINDER = "check_in_reminder"
}

/** Contenido de un evento `alert`: la alerta y a quien sugiere llamar el protocolo. */
@Serializable
data class AlertEventPayload(
    val alert: AlertDto = AlertDto(),
    val deliveredOnline: Boolean = false,
    val trustedContact: ContactDto? = null,
)

/**
 * Lee el canal de avisos del servidor y reconecta cuando se cae.
 *
 * La reconexion sube 2s, 4s, 8s... hasta [maxDelayMillis]. Es el aviso de emergencia de
 * una app de recuperacion: quedarse callado tras un corte de wifi no es una opcion, pero
 * martillear al servidor cada segundo tampoco.
 */
class EventStream(
    private val api: UnDiaMasApi,
    private val initialDelayMillis: Long = 2_000,
    private val maxDelayMillis: Long = 60_000,
) {

    fun events(): Flow<ServerEvent> = channelFlow {
        var retryDelay = initialDelayMillis
        while (true) {
            val connected = runCatching { collectOnce { send(it) } }
            if (connected.isSuccess) {
                // El servidor cerro limpiamente: se vuelve a intentar desde el principio.
                retryDelay = initialDelayMillis
            } else {
                val error = connected.exceptionOrNull()
                if (error is CancellationException) throw error
                // Sesion muerta: reconectar con el mismo token seria un bucle infinito.
                if (error is ApiException && error.isAuthExpired) throw error
            }
            delay(retryDelay)
            retryDelay = (retryDelay * 2).coerceAtMost(maxDelayMillis)
        }
    }

    /**
     * Una conexion, hasta que se corte.
     *
     * SSE separa los eventos con una linea en blanco. Solo interesan las lineas `data:`;
     * las que empiezan por `:` son latidos para mantener viva la conexion y hay que
     * ignorarlas, y `event: ready` solo confirma que el canal quedo abierto.
     */
    private suspend fun collectOnce(emit: suspend (ServerEvent) -> Unit) {
        val request: HttpRequestBuilder.() -> Unit = {
            method = HttpMethod.Get
            url("/v1/events")
            headers.append("Accept", "text/event-stream")
        }
        api.http.prepareRequest(request).execute { response ->
            response.throwIfError()
            val channel = response.bodyAsChannel()
            while (true) {
                val line = channel.readUTF8Line() ?: break
                if (line.isBlank() || line.startsWith(":")) continue
                val data = line.removePrefix("data:").trim()
                if (!line.startsWith("data:") || data.isBlank() || data == "{}") continue
                val event = runCatching { apiJson.decodeFromString<ServerEvent>(data) }.getOrNull()
                if (event != null && event.type.isNotBlank()) emit(event)
            }
        }
    }
}

/** Lee el contenido de un evento `alert`, o null si no lo es o viene mal formado. */
fun ServerEvent.asAlert(): AlertEventPayload? {
    if (type != ServerEventType.ALERT) return null
    val body = payload ?: return null
    return runCatching { apiJson.decodeFromJsonElement<AlertEventPayload>(body) }.getOrNull()
}

/** Lee el contenido de un evento `traffic_light`, o null si no lo es. */
fun ServerEvent.asTrafficLight(): TrafficLightDto? {
    if (type != ServerEventType.TRAFFIC_LIGHT) return null
    val body = payload ?: return null
    return runCatching { apiJson.decodeFromJsonElement<TrafficLightDto>(body) }.getOrNull()
}
