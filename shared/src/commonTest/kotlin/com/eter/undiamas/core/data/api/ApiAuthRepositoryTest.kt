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
import kotlin.test.assertNotNull
import kotlin.test.assertNull

private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")

/**
 * Abrir la app sin conexión no puede echar a nadie de sus propios datos.
 *
 * Estas pruebas cuidan la diferencia entre "tu sesión murió" y "ahora mismo no puedo
 * preguntarle al servidor", que son cosas muy distintas y se resuelven al revés.
 */
class ApiAuthRepositoryTest {

    private fun repositorio(
        engine: MockEngine,
        tokens: Tokens? = Tokens("acceso", "refresco"),
        cache: SessionCache = InMemorySessionCache(),
    ): Pair<ApiAuthRepository, TokenStore> {
        val store = InMemoryTokenStore(tokens)
        val api = UnDiaMasApi(createApiHttpClient(store, { "http://test.local" }, engine), store)
        return ApiAuthRepository(api, store, cache) to store
    }

    @Test
    fun `sin tokens no hay sesion que recuperar`() = runTest {
        val engine = MockEngine { respond("{}", headers = jsonHeaders) }
        val (repo, _) = repositorio(engine, tokens = null)

        assertNull(repo.restore())
    }

    @Test
    fun `con conexion la sesion viene del servidor y se guarda en el telefono`() = runTest {
        val engine = MockEngine {
            respond(
                """{"id":"u1","email":"ana@correo.mx","displayName":"Ana","role":"patient"}""",
                headers = jsonHeaders,
            )
        }
        val cache = InMemorySessionCache()
        val (repo, _) = repositorio(engine, cache = cache)

        val sesion = assertNotNull(repo.restore())

        assertEquals("u1", sesion.userId)
        assertEquals("Ana", sesion.displayName)
        // Guardarla es lo que permite volver a entrar la próxima vez sin conexión.
        assertEquals("u1", cache.read()?.userId)
    }

    /**
     * El caso que motivó todo esto: alguien abre la app en el metro. Antes veía una pantalla
     * de error; ahora entra y ve su racha, su diario y su "por qué".
     */
    @Test
    fun `sin red se entra con la sesion guardada en el telefono`() = runTest {
        val engine = MockEngine { throw kotlinx.io.IOException("sin red") }
        val cache = InMemorySessionCache(
            CachedSession("u1", "ana@correo.mx", "Ana", "patient"),
        )
        val (repo, store) = repositorio(engine, cache = cache)

        val sesion = assertNotNull(repo.restore())

        assertEquals("u1", sesion.userId)
        assertEquals("Ana", sesion.displayName)
        // Y los tokens siguen ahí: la sesión no ha muerto, solo no hay cobertura.
        assertNotNull(store.read())
    }

    @Test
    fun `sin red y sin copia local no se puede adivinar de quien son los datos`() = runTest {
        val engine = MockEngine { throw kotlinx.io.IOException("sin red") }
        val (repo, store) = repositorio(engine)

        assertFailsWith<Exception> { repo.restore() }

        // No se borra nada: cuando vuelva la red, la sesión sigue siendo válida.
        assertNotNull(store.read())
    }

    /**
     * Un 401 sí es definitivo. Aquí hay que borrar todo, incluida la copia local: si se
     * quedara, la app entraría una y otra vez a una sesión que el servidor ya no reconoce.
     */
    @Test
    fun `una sesion revocada borra los tokens y la copia local`() = runTest {
        val engine = MockEngine {
            respondError(HttpStatusCode.Unauthorized, """{"error":"unauthenticated"}""")
        }
        val cache = InMemorySessionCache(
            CachedSession("u1", "ana@correo.mx", "Ana", "patient"),
        )
        val (repo, store) = repositorio(engine, cache = cache)

        assertNull(repo.restore())

        assertNull(store.read())
        assertNull(cache.read())
    }

    @Test
    fun `cerrar sesion borra tambien la copia local`() = runTest {
        val engine = MockEngine { respond("{}", headers = jsonHeaders) }
        val cache = InMemorySessionCache(
            CachedSession("u1", "ana@correo.mx", "Ana", "patient"),
        )
        val (repo, store) = repositorio(engine, cache = cache)

        repo.logout()

        assertNull(store.read())
        assertNull(cache.read())
    }

    @Test
    fun `al entrar se guarda la sesion para la proxima vez`() = runTest {
        val engine = MockEngine {
            respond(
                """{"accessToken":"a1","refreshToken":"r1","expiresIn":900,
                   "user":{"id":"u9","email":"luis@correo.mx","displayName":"Luis","role":"patient"}}""",
                headers = jsonHeaders,
            )
        }
        val cache = InMemorySessionCache()
        val (repo, _) = repositorio(engine, tokens = null, cache = cache)

        val sesion = repo.login("luis@correo.mx", "12345678")

        assertEquals("u9", sesion.userId)
        assertEquals("Luis", cache.read()?.displayName)
    }
}
