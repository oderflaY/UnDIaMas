package com.eter.undiamas.core.data.api

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")

class ApiClientTest {

    private fun graphWith(engine: MockEngine, tokens: Tokens? = null): Pair<UnDiaMasApi, TokenStore> {
        val store = InMemoryTokenStore(tokens)
        val client = createApiHttpClient(store, { "http://test.local" }, engine)
        return UnDiaMasApi(client, store) to store
    }

    @Test
    fun `manda el token en las rutas de datos y no en las de sesion`() = runTest {
        val seenAuthHeaders = mutableListOf<String?>()
        val engine = MockEngine { request ->
            seenAuthHeaders += request.headers[HttpHeaders.Authorization]
            respond("""{"id":"u1","email":"a@b.mx","displayName":"Ana"}""", headers = jsonHeaders)
        }
        val (api, _) = graphWith(engine, Tokens("token-de-acceso", "refresco"))

        api.me()
        api.login("a@b.mx", "12345678")

        assertEquals("Bearer token-de-acceso", seenAuthHeaders[0])
        // El login no debe llevar el token viejo: si estuviera caducado ensuciaría el
        // intento de volver a entrar, que es justo lo que hay que arreglar en ese momento.
        assertNull(seenAuthHeaders[1])
    }

    @Test
    fun `renueva el token solo cuando algo responde 401 y repite la peticion`() = runTest {
        var trackerCalls = 0
        val engine = MockEngine { request ->
            when {
                request.url.encodedPath.endsWith("/auth/refresh") -> respond(
                    """{"accessToken":"nuevo","refreshToken":"refresco-2","expiresIn":900,
                       "user":{"id":"u1","email":"a@b.mx","displayName":"Ana","role":"patient"}}""",
                    headers = jsonHeaders,
                )

                else -> {
                    trackerCalls++
                    if (request.headers[HttpHeaders.Authorization] == "Bearer caducado") {
                        respondError(HttpStatusCode.Unauthorized, """{"error":"unauthenticated"}""")
                    } else {
                        respond("""{"rachaSegundos":42}""", headers = jsonHeaders)
                    }
                }
            }
        }
        val (api, store) = graphWith(engine, Tokens("caducado", "refresco-1"))

        val tracker = api.tracker()

        assertEquals(42, tracker.rachaSegundos)
        assertEquals(2, trackerCalls, "debe reintentar la petición original tras renovar")
        // El backend rota el refresh en cada uso: guardar el viejo mataría la sesión en la
        // siguiente renovación.
        assertEquals("refresco-2", store.read()?.refreshToken)
        assertEquals("nuevo", store.read()?.accessToken)
    }

    @Test
    fun `si el refresco tambien falla se borran los tokens`() = runTest {
        val engine = MockEngine { request ->
            if (request.url.encodedPath.endsWith("/auth/refresh")) {
                respondError(HttpStatusCode.Unauthorized, """{"error":"unauthenticated"}""")
            } else {
                respondError(HttpStatusCode.Unauthorized, """{"error":"unauthenticated"}""")
            }
        }
        val (api, store) = graphWith(engine, Tokens("caducado", "tambien-caducado"))

        assertFailsWith<ApiException> { api.tracker() }

        // Sin esto la app reintentaría en bucle con un refresh que ya no vale.
        assertNull(store.read())
    }

    @Test
    fun `traduce el cuerpo de error del backend a un codigo estable`() = runTest {
        val engine = MockEngine {
            respondError(
                HttpStatusCode.BadRequest,
                """{"error":"invalid-argument","message":"cravingLevel debe estar entre 0 y 10"}""",
            )
        }
        val (api, _) = graphWith(engine, Tokens("t", "r"))

        val error = assertFailsWith<ApiException> { api.tracker() }

        assertEquals(ApiErrorCode.INVALID_ARGUMENT, error.code)
        assertEquals(400, error.status)
        // El `message` del servidor es para depurar; a la persona se le enseña otra cosa.
        assertTrue("cravingLevel" in error.debugMessage)
        assertTrue("cravingLevel" !in error.userMessage)
    }

    @Test
    fun `un 429 se explica como que hay que esperar, no como un fallo`() = runTest {
        val engine = MockEngine {
            respondError(HttpStatusCode.TooManyRequests, """{"error":"rate-limited","message":"20/min"}""")
        }
        val (api, _) = graphWith(engine, Tokens("t", "r"))

        val error = assertFailsWith<ApiException> { api.chat("hola") }

        assertEquals(ApiErrorCode.RATE_LIMITED, error.code)
        assertTrue("Espera" in error.userMessage)
    }

    @Test
    fun `cerrar sesion borra los tokens aunque el servidor no conteste`() = runTest {
        val engine = MockEngine { respondError(HttpStatusCode.InternalServerError) }
        val (api, store) = graphWith(engine, Tokens("t", "r"))

        api.logout()

        // Si el par siguiera guardado, "cerrar sesión" no habría cerrado nada en este
        // teléfono, que es exactamente lo contrario de lo que alguien espera al pulsarlo.
        assertNull(store.read())
    }

    @Test
    fun `guarda el par nuevo al entrar`() = runTest {
        val engine = MockEngine {
            respond(
                """{"accessToken":"a1","refreshToken":"r1","expiresIn":900,
                   "user":{"id":"u1","email":"a@b.mx","displayName":"Ana","role":"patient"}}""",
                headers = jsonHeaders,
            )
        }
        val (api, store) = graphWith(engine)

        val response = api.login("a@b.mx", "12345678")

        assertEquals("u1", response.user.id)
        assertEquals(Tokens("a1", "r1"), store.read())
    }

    @Test
    fun `las listas del backend vienen envueltas en items`() = runTest {
        val engine = MockEngine {
            respond(
                """{"items":[{"id":"c1","riskLevel":"ROJO","cravingLevel":10,"mood":"MUY_MAL",
                   "triggers":["SOLEDAD"],"note":"","answers":{},
                   "createdAt":"2026-08-03T11:45:39.471784-06:00"}]}""",
                headers = jsonHeaders,
            )
        }
        val (api, _) = graphWith(engine, Tokens("t", "r"))

        val checkIns = api.checkIns()

        assertEquals(1, checkIns.size)
        assertEquals("ROJO", checkIns.first().riskLevel)
    }
}
