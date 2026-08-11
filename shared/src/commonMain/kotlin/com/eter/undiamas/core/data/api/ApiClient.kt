package com.eter.undiamas.core.data.api

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpResponseValidator
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.clearAuthTokens
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.URLProtocol
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/** Configuracion de JSON compartida por el cliente HTTP y por el lector de eventos SSE. */
val apiJson: Json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    explicitNulls = false
}

/**
 * Crea el cliente HTTP de la app.
 *
 * Tres decisiones que importan:
 *
 * 1. El token se adjunta a todo salvo a las rutas de sesion. Mandarlo tambien al login haria que
 *    un token caducado ensuciara el intento de volver a entrar.
 * 2. La renovacion la lleva el plugin Auth: cuando algo responde 401, Ktor llama solo a
 *    `/v1/auth/refresh`, guarda el par nuevo y repite la peticion original. La pantalla
 *    ni se entera.
 * 3. Si el refresh tambien falla, se borran los tokens y la excepcion sube como
 *    `unauthenticated`, que es la señal de "manda a la persona a la pantalla de entrada".
 */
fun createApiHttpClient(
    tokenStore: TokenStore,
    baseUrl: () -> String = { ApiConfig.baseUrl },
    engine: HttpClientEngine? = null,
    onSessionExpired: suspend () -> Unit = {},
): HttpClient {
    val config: HttpClientConfig<*>.() -> Unit = {
        expectSuccess = false

        install(ContentNegotiation) {
            json(apiJson)
        }

        defaultRequest {
            // Se lee en cada peticion, no una sola vez al construir el cliente: asi cambiar
            // la direccion en Configuracion surte efecto sin reiniciar la app.
            url(baseUrl())
            contentType(ContentType.Application.Json)
        }

        install(Auth) {
            bearer {
                loadTokens {
                    tokenStore.read()?.let { BearerTokens(it.accessToken, it.refreshToken) }
                }

                refreshTokens {
                    val stored = oldTokens?.refreshToken ?: tokenStore.read()?.refreshToken
                    if (stored == null) {
                        onSessionExpired()
                        return@refreshTokens null
                    }
                    val response = runCatching {
                        client.post("/v1/auth/refresh") {
                            markAsRefreshTokenRequest()
                            contentType(ContentType.Application.Json)
                            setBody(RefreshRequest(stored))
                        }
                    }.getOrNull()

                    if (response == null || !response.status.isSuccess()) {
                        // La sesion murio de verdad (refresh caducado o revocado por logout
                        // en otro dispositivo). Borrar el par evita reintentar en bucle.
                        tokenStore.clear()
                        onSessionExpired()
                        return@refreshTokens null
                    }

                    val fresh: AuthResponse = response.body()
                    // El backend rota el refresh en cada uso: guardar el nuevo es obligatorio.
                    tokenStore.save(Tokens(fresh.accessToken, fresh.refreshToken))
                    BearerTokens(fresh.accessToken, fresh.refreshToken)
                }

                sendWithoutRequest { request ->
                    request.url.pathSegments.none { it == "auth" }
                }
            }
        }

        HttpResponseValidator {
            validateResponse { response -> response.throwIfError() }
        }
    }

    return if (engine != null) HttpClient(engine, config) else HttpClient(config)
}

/**
 * Convierte una respuesta de error en [ApiException].
 *
 * Se lee el cuerpo `{error,message}` porque el codigo HTTP solo no basta: un 404 puede ser
 * "esa ruta no existe" (el servidor arranco sin clave de IA) o "ese dato no es tuyo".
 */
suspend fun HttpResponse.throwIfError() {
    if (status.isSuccess()) return
    val raw = runCatching { bodyAsText() }.getOrDefault("")
    val parsed = runCatching { apiJson.decodeFromString<ApiErrorBody>(raw) }.getOrNull()
    throw ApiException(
        code = parsed?.error ?: defaultCodeFor(status.value),
        status = status.value,
        debugMessage = parsed?.message ?: raw.ifBlank { "sin cuerpo" },
    )
}

private fun defaultCodeFor(status: Int): String = when (status) {
    400 -> ApiErrorCode.INVALID_ARGUMENT
    401 -> ApiErrorCode.UNAUTHENTICATED
    403 -> ApiErrorCode.FORBIDDEN
    404 -> ApiErrorCode.NOT_FOUND
    429 -> ApiErrorCode.RATE_LIMITED
    502 -> ApiErrorCode.AI_UNAVAILABLE
    else -> "unknown"
}

/**
 * Olvida el token que el plugin Auth tiene cacheado.
 *
 * Hay que llamarlo despues de entrar o de salir: sin esto, Ktor seguiria mandando el token
 * de la sesion anterior hasta que alguien responda 401, y en el peor caso una persona
 * veria por un instante datos de la cuenta anterior.
 */
fun HttpClient.forgetCachedToken() {
    clearAuthTokens()
}

/** Comprueba que la direccion escrita a mano en Configuracion sea usable. */
fun isValidBaseUrl(value: String): Boolean = runCatching {
    val url = io.ktor.http.Url(value)
    url.host.isNotBlank() && (url.protocol == URLProtocol.HTTP || url.protocol == URLProtocol.HTTPS)
}.getOrDefault(false)
