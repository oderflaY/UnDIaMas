package com.eter.undiamas.core.data.api

import com.eter.undiamas.core.data.local.LocalStore
import com.eter.undiamas.core.data.local.Outbox
import com.eter.undiamas.core.data.local.UndiamasDatabase
import com.eter.undiamas.core.domain.model.UserProfile
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Clock

private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")

private const val USUARIO =
    """{"id":"u1","email":"ana@correo.mx","displayName":"Ana","role":"patient"}"""

/**
 * Qué cuenta como "esta persona ya contestó el cuestionario inicial".
 *
 * Hubo un fallo real por confundir esto con "tiene nombre": como el registro ya pide el
 * nombre, el cuestionario se cerraba solo a media pregunta en cuanto llegaba el perfil del
 * servidor. Quien acababa de crear su cuenta veía las preguntas desaparecer sin haberlas
 * contestado, y entraba a una app sin fecha de inicio ni gasto diario, es decir, sin
 * contador y sin ahorro.
 *
 * La marca buena es tener fecha de inicio de racha, porque solo la escribe el propio
 * cuestionario.
 */
class OnboardingCompletoTest {

    private fun repositorio(engine: MockEngine): OfflinePerfilRepository {
        val store = InMemoryTokenStore(Tokens("acceso", "refresco"))
        val api = UnDiaMasApi(createApiHttpClient(store, { "http://test.local" }, engine), store)
        val db = UndiamasDatabase(":memory:")
        return OfflinePerfilRepository(api, LocalStore(db), Outbox(db), alSincronizar = {})
    }

    /** Responde al perfil y al tracker en ese orden, que es como los pide `refresh()`. */
    private fun servidor(tracker: String): MockEngine {
        var primera = true
        return MockEngine {
            val cuerpo = if (primera) USUARIO else tracker
            primera = false
            respond(cuerpo, headers = jsonHeaders)
        }
    }

    @Test
    fun `una cuenta recien creada no cuenta como cuestionario contestado`() = runTest {
        // Tiene nombre —el registro lo pidió— pero no tiene fecha de inicio.
        val repo = repositorio(servidor("""{"startDate":null,"dailySavingsRate":0.0}"""))

        repo.refresh()

        assertFalse(
            repo.onboardingCompleto.value,
            "tener nombre no es haber contestado el cuestionario",
        )
    }

    @Test
    fun `una cuenta con fecha de inicio ya lo contesto`() = runTest {
        // El caso de reinstalar la app: en el teléfono no queda nada, pero la cuenta existe.
        val repo = repositorio(
            servidor("""{"startDate":"2026-01-15T08:00:00Z","dailySavingsRate":150.0}"""),
        )

        repo.refresh()

        assertTrue(repo.onboardingCompleto.value)
    }

    @Test
    fun `terminar el cuestionario lo marca aunque no haya servidor`() = runTest {
        val repo = repositorio(MockEngine { respond("{}", headers = jsonHeaders) })

        repo.saveTracker(
            UserProfile(
                userId = "u1",
                displayName = "Ana",
                sobrietyStartDate = Clock.System.now(),
            ),
        )

        assertTrue(
            repo.onboardingCompleto.value,
            "sin conexión también se termina el cuestionario, y no puede volver a salir",
        )
    }
}
