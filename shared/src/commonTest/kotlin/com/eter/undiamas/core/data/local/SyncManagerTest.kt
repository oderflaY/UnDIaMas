package com.eter.undiamas.core.data.local

import com.eter.undiamas.core.data.api.InMemoryTokenStore
import com.eter.undiamas.core.data.api.Tokens
import com.eter.undiamas.core.data.api.UnDiaMasApi
import com.eter.undiamas.core.data.api.createApiHttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")

/**
 * El sincronizador decide qué se reintenta y qué se descarta. Equivocarse hacia un lado
 * pierde datos de alguien; hacia el otro, atasca la cola para siempre y ya no llega nada.
 */
class SyncManagerTest {

    private class Escenario(respuesta: MockEngine.() -> Unit = {}) {
        val db = UndiamasDatabase(":memory:")
        val local = LocalStore(db)
        val outbox = Outbox(db)
        var peticiones = 0
            private set
        lateinit var sync: SyncManager

        fun conMotor(engine: MockEngine): Escenario {
            val store = InMemoryTokenStore(Tokens("t", "r"))
            val api = UnDiaMasApi(createApiHttpClient(store, { "http://test.local" }, engine), store)
            sync = SyncManager(api, local, outbox)
            return this
        }
    }

    private fun escenarioCon(engine: MockEngine): Escenario = Escenario().conMotor(engine)

    @Test
    fun `envia lo pendiente y vacia la cola`() = runTest {
        val creados = mutableListOf<String>()
        val engine = MockEngine { request ->
            creados += request.url.encodedPath
            respond("""{"id":"servidor-1","content":"x"}""", HttpStatusCode.Created, jsonHeaders)
        }
        val e = escenarioCon(engine)
        e.local.abrirSesion("u1")
        e.outbox.encolar(TipoPendiente.DIARIO_CREAR, """{"content":"hola"}""", idLocal = "local-1")

        val exito = e.sync.sincronizar()

        assertTrue(exito)
        assertEquals(0, e.outbox.pendientes.value)
        assertEquals(listOf("/v1/journal"), creados)
        assertEquals(EstadoSync.AL_DIA, e.sync.estado.value)
    }

    @Test
    fun `sin conexion la operacion se queda en la cola`() = runTest {
        val engine = MockEngine { throw kotlinx.io.IOException("sin red") }
        val e = escenarioCon(engine)
        e.outbox.encolar(TipoPendiente.ANIMO, """{"mood":"BIEN"}""", idLocal = "local-1")

        val exito = e.sync.sincronizar()

        assertFalse(exito)
        // Lo que la persona registró no puede evaporarse porque el metro no tenga señal.
        assertEquals(1, e.outbox.pendientes.value)
        assertEquals(EstadoSync.SIN_CONEXION, e.sync.estado.value)
        assertEquals(1, e.outbox.siguientes().single().intentos)
    }

    /**
     * Un 400 no se arregla reintentando. Si se quedara en la cola, todo lo que venga detrás
     * —incluido un check-in en rojo— no llegaría nunca al servidor.
     */
    @Test
    fun `un rechazo definitivo no bloquea lo que viene detras`() = runTest {
        var llamadasDiario = 0
        val engine = MockEngine { request ->
            if (request.url.encodedPath.endsWith("/check-ins")) {
                respondError(
                    HttpStatusCode.BadRequest,
                    """{"error":"invalid-argument","message":"cravingLevel fuera de rango"}""",
                )
            } else {
                llamadasDiario++
                respond("""{"id":"s1","content":"x"}""", HttpStatusCode.Created, jsonHeaders)
            }
        }
        val e = escenarioCon(engine)
        e.outbox.encolar(TipoPendiente.CHECK_IN, """{"riskLevel":"VERDE","cravingLevel":99,"mood":"NEUTRAL","triggers":[],"note":"","answers":{}}""")
        e.outbox.encolar(TipoPendiente.DIARIO_CREAR, """{"content":"válida"}""", idLocal = "local-2")

        val exito = e.sync.sincronizar()

        assertTrue(exito)
        assertEquals(0, e.outbox.pendientes.value)
        assertEquals(1, llamadasDiario, "lo válido tuvo que enviarse igualmente")
    }

    /**
     * Un 401 sí es recuperable: se arregla al renovar el token o al volver a entrar. Tratarlo
     * como definitivo tiraría a la basura lo que alguien escribió mientras caducaba su sesión.
     */
    @Test
    fun `una sesion caducada no descarta la operacion`() = runTest {
        val engine = MockEngine {
            respondError(HttpStatusCode.Unauthorized, """{"error":"unauthenticated"}""")
        }
        val e = escenarioCon(engine)
        e.outbox.encolar(TipoPendiente.CHECK_IN, """{"riskLevel":"ROJO","cravingLevel":9,"mood":"MAL","triggers":[],"note":"","answers":{}}""")

        e.sync.sincronizar()

        assertEquals(1, e.outbox.pendientes.value, "debe sobrevivir para reintentarse")
    }

    @Test
    fun `un 429 se reintenta, no se descarta`() = runTest {
        val engine = MockEngine {
            respondError(HttpStatusCode.TooManyRequests, """{"error":"rate-limited"}""")
        }
        val e = escenarioCon(engine)
        e.outbox.encolar(TipoPendiente.ANIMO, """{"mood":"BIEN"}""")

        e.sync.sincronizar()

        assertEquals(1, e.outbox.pendientes.value)
    }

    @Test
    fun `se envia en el orden en que ocurrieron las cosas`() = runTest {
        val orden = mutableListOf<String>()
        val engine = MockEngine { request ->
            orden += request.url.encodedPath
            respond("""{"id":"s","content":"x","entry":{"id":"s"}}""", HttpStatusCode.Created, jsonHeaders)
        }
        val e = escenarioCon(engine)
        e.outbox.encolar(TipoPendiente.RECAIDA, """{"note":"","triggers":[]}""")
        e.outbox.encolar(TipoPendiente.ANIMO, """{"mood":"MAL"}""")
        e.outbox.encolar(TipoPendiente.DIARIO_CREAR, """{"content":"x"}""")

        e.sync.sincronizar()

        assertEquals(listOf("/v1/relapses", "/v1/mood-logs", "/v1/journal"), orden)
    }

    @Test
    fun `al confirmar, la fila local recibe el id del servidor`() = runTest {
        val engine = MockEngine {
            respond("""{"id":"servidor-99","content":"x"}""", HttpStatusCode.Created, jsonHeaders)
        }
        val e = escenarioCon(engine)
        e.local.abrirSesion("u1")
        e.local.guardarEntradaDiario(
            com.eter.undiamas.features.diario.domain.DiaryEntry(
                id = "local-1",
                userId = "u1",
                createdAt = kotlin.time.Instant.parse("2026-08-03T10:00:00Z"),
                text = "hola",
            ),
            pendiente = true,
        )
        e.outbox.encolar(TipoPendiente.DIARIO_CREAR, """{"content":"hola"}""", idLocal = "local-1")

        e.sync.sincronizar()

        assertEquals("servidor-99", e.local.diario.value.single().id)
        assertTrue(e.local.sinEnviar.value.isEmpty())
    }

    @Test
    fun `sincronizar sin nada pendiente no llama al servidor`() = runTest {
        var llamadas = 0
        val engine = MockEngine {
            llamadas++
            respond("{}", HttpStatusCode.OK, jsonHeaders)
        }
        val e = escenarioCon(engine)

        assertTrue(e.sync.sincronizar())
        assertEquals(0, llamadas)
    }
}
