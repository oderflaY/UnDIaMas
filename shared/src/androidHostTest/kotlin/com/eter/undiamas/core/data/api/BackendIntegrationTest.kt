package com.eter.undiamas.core.data.api

import com.eter.undiamas.core.data.local.LocalStore
import com.eter.undiamas.core.data.local.Outbox
import com.eter.undiamas.core.data.local.SyncManager
import com.eter.undiamas.core.data.local.UndiamasDatabase
import com.eter.undiamas.core.domain.model.CheckInEntry
import com.eter.undiamas.core.domain.model.Mood
import com.eter.undiamas.core.domain.model.RiskLevel
import com.eter.undiamas.core.domain.model.SupportRole
import com.eter.undiamas.core.domain.model.Trigger
import com.eter.undiamas.core.domain.model.TrustedContact
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.runBlocking
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Clock

private const val BASE_URL = "http://192.168.1.145:8080"

/** Un puerto donde no escucha nadie: es la forma más fiel de simular "sin conexión". */
private const val URL_SIN_RED = "http://127.0.0.1:9"

/**
 * Recorre la capa de datos completa contra el backend de verdad.
 *
 * No sustituye a los tests con motor falso: esos comprueban la lógica y corren siempre.
 * Este comprueba lo que ningún mock puede — que los nombres de los campos coinciden con
 * los del servidor, y que lo guardado sin conexión llega de verdad cuando vuelve la red.
 *
 * Si el servidor no está levantado, cada prueba se salta sola en vez de fallar: no tener
 * Postgres arriba no es un error del código de la app.
 */
class BackendIntegrationTest {

    private val disponible: Boolean = runCatching {
        runBlocking {
            HttpClient(OkHttp).use { it.get("$BASE_URL/healthz").bodyAsText().contains("ok") }
        }
    }.onFailure { println("No se pudo contactar al backend: $it") }.getOrDefault(false)

    /** Cambiarla simula perder y recuperar la conexión sin tocar el wifi de la máquina. */
    private var urlActual = BASE_URL

    private val store = InMemoryTokenStore()
    private val api = UnDiaMasApi(createApiHttpClient(store, { urlActual }, OkHttp.create()), store)

    private val archivo: File = File.createTempFile("undiamas-test", ".db").also { it.delete() }
    private val db = UndiamasDatabase(archivo.absolutePath)
    private val local = LocalStore(db)
    private val outbox = Outbox(db)
    private val sync = SyncManager(api, local, outbox)
    private val alSincronizar: suspend () -> Unit = { runCatching { sync.sincronizar() } }

    private val perfil = OfflinePerfilRepository(api, local, outbox, alSincronizar)
    private val checkIns = OfflineCheckInRepository(api, local, outbox, alSincronizar)
    private val diario = OfflineDiaryRepository(api, local, outbox, alSincronizar)
    private val animos = OfflineMoodRepository(api, local, outbox, alSincronizar)
    private val recaidas = OfflineRelapseRepository(api, local, outbox, alSincronizar)
    private val semaforo = OfflineTrafficLightRepository(api, local, outbox, alSincronizar)
    private val alertas = OfflineAlertRepository(api, local, outbox)
    private val estadisticas = ApiStatsRepository(api)

    @AfterTest
    fun limpiar() {
        if (disponible) runBlocking { runCatching { api.logout() } }
        db.cerrar()
        archivo.delete()
    }

    /** Cada ejecución crea su propia cuenta: así las pruebas no dependen unas de otras. */
    private fun conCuentaNueva(bloque: suspend () -> Unit) {
        if (!disponible) {
            println("Backend apagado en $BASE_URL — prueba de integración omitida.")
            return
        }
        runBlocking {
            val correo = "kmp-test-${Clock.System.now().toEpochMilliseconds()}@test.mx"
            val sesion = api.register(correo, "12345678", "Prueba KMP")
            local.abrirSesion(sesion.user.id)
            bloque()
        }
    }

    // ---- Lo que importa de verdad: el ciclo sin conexión --------------------------

    /**
     * El caso completo: alguien escribe un check-in sin señal, cierra la app, y al recuperar
     * la conexión el dato llega solo. Si esta prueba falla, la app pierde datos de personas.
     */
    @Test
    fun `un check-in escrito sin conexion llega al servidor cuando vuelve la red`() = conCuentaNueva {
        urlActual = URL_SIN_RED

        checkIns.add(
            CheckInEntry(
                id = "",
                userId = "",
                answeredAt = Clock.System.now(),
                answers = mapOf("impulso_consumo" to "si"),
                riskLevel = RiskLevel.ROJO,
                triggers = setOf(Trigger.SOLEDAD),
                urgeIntensity = 9,
                note = "sin señal en el metro",
            ),
            mood = Mood.MUY_MAL,
        )

        // Ya se ve en pantalla aunque no haya salido del teléfono.
        assertEquals(1, checkIns.items.value.size)
        assertEquals("sin señal en el metro", checkIns.items.value.single().note)
        assertEquals(1, outbox.pendientes.value, "debe quedar encolado")
        assertTrue(local.sinEnviar.value.isNotEmpty(), "debe marcarse como no enviado")

        urlActual = BASE_URL
        assertTrue(sync.sincronizar(), "con red, la cola debe vaciarse")

        assertEquals(0, outbox.pendientes.value)
        assertTrue(local.sinEnviar.value.isEmpty(), "ya no queda nada sin enviar")

        // Y está en el servidor de verdad, no solo marcado como enviado en local.
        val enServidor = api.checkIns()
        assertEquals(1, enServidor.size)
        assertEquals("sin señal en el metro", enServidor.single().note)
        assertEquals("ROJO", enServidor.single().riskLevel)

        // El id provisional se cambió por el que asignó el servidor.
        val idLocal = checkIns.items.value.single().id
        assertEquals(enServidor.single().id, idLocal)
        assertTrue(!idLocal.startsWith("local-"), "el id local debía sustituirse: $idLocal")
    }

    @Test
    fun `varios cambios sin conexion llegan en el orden en que ocurrieron`() = conCuentaNueva {
        urlActual = URL_SIN_RED

        diario.add("primera entrada")
        animos.add(Mood.MAL)
        diario.add("segunda entrada")

        assertEquals(3, outbox.pendientes.value)
        assertEquals(listOf("segunda entrada", "primera entrada"), diario.items.value.map { it.text })

        urlActual = BASE_URL
        assertTrue(sync.sincronizar())

        val entradas = api.journal()
        assertEquals(2, entradas.size)
        assertEquals(1, api.moodLogs().size)
        // El servidor devuelve de más reciente a más antigua: la segunda se creó después.
        assertEquals("segunda entrada", entradas.first().content)
    }

    /**
     * Escribir y borrar sin conexión no debe dejar rastro en el servidor: crear algo para
     * borrarlo acto seguido es trabajo inútil y datos de alguien que decidió que no existan.
     */
    @Test
    fun `lo creado y borrado sin conexion nunca sale del telefono`() = conCuentaNueva {
        urlActual = URL_SIN_RED

        val entrada = diario.add("esto no debería llegar nunca")
        assertEquals(1, outbox.pendientes.value)

        diario.delete(entrada.id)
        assertEquals(0, outbox.pendientes.value, "las dos operaciones se anulan")
        assertTrue(diario.items.value.isEmpty())

        urlActual = BASE_URL
        assertTrue(sync.sincronizar())

        assertTrue(api.journal().isEmpty(), "el servidor no debe saber de esa entrada")
    }

    @Test
    fun `lo pendiente sobrevive a cerrar la app`() = conCuentaNueva {
        urlActual = URL_SIN_RED
        animos.add(Mood.MUY_MAL)
        assertEquals(1, outbox.pendientes.value)

        // Se abre otra conexión al MISMO archivo: es lo que pasa al reabrir la app.
        val db2 = UndiamasDatabase(archivo.absolutePath)
        try {
            val outbox2 = Outbox(db2)
            outbox2.refrescarConteo()

            assertEquals(1, outbox2.pendientes.value, "la cola vive en disco, no en memoria")
            val pendiente = outbox2.siguientes().single()
            assertTrue("MUY_MAL" in pendiente.carga)
        } finally {
            db2.cerrar()
        }
    }

    @Test
    fun `un dato rechazado por el servidor no bloquea la cola`() = conCuentaNueva {
        urlActual = URL_SIN_RED

        // Intensidad fuera de rango: el servidor responde 400 y no cambiará de opinión.
        checkIns.add(
            CheckInEntry(
                id = "",
                userId = "",
                answeredAt = Clock.System.now(),
                answers = emptyMap(),
                riskLevel = RiskLevel.VERDE,
                urgeIntensity = 99,
            ),
        )
        diario.add("esta sí es válida")

        urlActual = BASE_URL
        sync.sincronizar()

        // Lo válido pasó pese a que lo anterior fuera rechazado; si la cola se hubiera
        // atascado en el primero, esto nunca habría llegado.
        assertEquals(0, outbox.pendientes.value, "la cola no puede quedarse atascada")
        assertEquals(1, api.journal().size)
    }

    // ---- Contrato con el servidor ------------------------------------------------

    @Test
    fun `registro, perfil y contactos de emergencia`() = conCuentaNueva {
        perfil.refresh()
        val inicial = assertNotNull(perfil.profile.value)
        assertEquals("Prueba KMP", inicial.displayName)

        perfil.saveIdentity("Ana", "por mi hija")
        perfil.saveContacts(
            inicial.copy(
                trustedContact = TrustedContact("Luis", "555-1", SupportRole.PADRINO),
                supportNetwork = listOf(TrustedContact("Mar", "555-2", SupportRole.AMISTAD)),
            ),
        )
        perfil.refresh()

        val guardado = assertNotNull(perfil.profile.value)
        assertEquals("Ana", guardado.displayName)
        assertEquals("por mi hija", guardado.personalWhy)
        // El primero es el que usa el protocolo de emergencia: el orden importa.
        assertEquals("Luis", guardado.trustedContact?.name)
        assertEquals(SupportRole.PADRINO, guardado.trustedContact?.role)
        assertEquals(listOf("Mar"), guardado.supportNetwork.map { it.name })
    }

    @Test
    fun `un check-in en rojo crea la alerta del protocolo de emergencia`() = conCuentaNueva {
        checkIns.add(
            CheckInEntry(
                id = "",
                userId = "",
                answeredAt = Clock.System.now(),
                answers = mapOf("impulso_consumo" to "si"),
                riskLevel = RiskLevel.ROJO,
                triggers = setOf(Trigger.SOLEDAD),
                urgeIntensity = 10,
            ),
            mood = Mood.MUY_MAL,
        )

        alertas.refresh()
        semaforo.refresh()

        assertEquals(RiskLevel.ROJO, semaforo.current.value)
        val alerta = alertas.items.value.firstOrNull()
        assertNotNull(alerta, "un check-in en ROJO debe dejar alerta en el servidor")
        assertEquals(RiskLevel.ROJO, alerta.riskLevel)
        assertTrue(!alerta.handled)

        alertas.markHandled(alerta.id)
        sync.sincronizar()
        alertas.refresh()
        assertTrue(alertas.items.value.first { it.id == alerta.id }.handled)
    }

    @Test
    fun `una recaida reinicia la racha y conserva el record`() = conCuentaNueva {
        perfil.refresh()
        val hace30Dias = Clock.System.now().minus(kotlin.time.Duration.parse("720h"))
        perfil.saveTracker(
            assertNotNull(perfil.profile.value).copy(
                sobrietyStartDate = hace30Dias,
                previousDailyExpense = 100.0,
            ),
        )
        perfil.refresh()
        val rachaPrevia = perfil.streakSeconds.value
        assertTrue(rachaPrevia > 0, "la racha debía haber empezado hace 30 días")

        recaidas.register("prueba", listOf("ESTRES"))
        perfil.refresh()

        assertTrue(perfil.streakSeconds.value < rachaPrevia, "la racha debe reiniciarse")
        // El récord es lo único que sobrevive a una recaída: si esto falla, la app le está
        // borrando a alguien el mejor tramo que consiguió.
        assertTrue(
            assertNotNull(perfil.profile.value).recordStreakSeconds >= rachaPrevia,
            "el récord histórico debe conservarse",
        )
        assertEquals(1, recaidas.items.value.size)
    }

    @Test
    fun `diario y animo van y vuelven`() = conCuentaNueva {
        val entrada = diario.add("hoy fue un día difícil pero aguanté")
        animos.add(Mood.BIEN)

        diario.refresh()
        animos.refresh()

        assertEquals("hoy fue un día difícil pero aguanté", diario.items.value.single().text)
        assertEquals(Mood.BIEN, animos.items.value.single().mood)

        diario.delete(diario.items.value.single().id)
        sync.sincronizar()
        diario.refresh()
        assertTrue(diario.items.value.isEmpty())
    }

    @Test
    fun `las estadisticas cuentan lo que se guardo`() = conCuentaNueva {
        repeat(2) {
            checkIns.add(
                CheckInEntry(
                    id = "",
                    userId = "",
                    answeredAt = Clock.System.now(),
                    answers = emptyMap(),
                    riskLevel = RiskLevel.AMARILLO,
                    triggers = setOf(Trigger.ESTRES),
                    urgeIntensity = 5,
                ),
            )
        }

        estadisticas.refresh(30)

        val tendencias = assertNotNull(estadisticas.trends.value)
        assertEquals(2, tendencias.totalCheckIns)
        assertEquals(2, tendencias.yellow)
        assertEquals(5.0, tendencias.averageCraving)
        assertTrue(tendencias.topTriggers.any { it.first == "ESTRES" })
    }

    @Test
    fun `la sesion se recupera con el token guardado`() = conCuentaNueva {
        val auth = ApiAuthRepository(api, store)

        val sesion = assertNotNull(auth.restore())

        assertTrue(sesion.userId.isNotBlank())
        assertEquals("patient", sesion.role)
    }

    @Test
    fun `sin sesion no se puede leer nada`() {
        if (!disponible) return
        runBlocking {
            val anonimo = InMemoryTokenStore()
            val sinSesion = UnDiaMasApi(createApiHttpClient(anonimo, { BASE_URL }, OkHttp.create()), anonimo)

            val error = runCatching { sinSesion.checkIns() }.exceptionOrNull()

            assertTrue(error is ApiException, "debía fallar, falló con: $error")
            assertEquals(ApiErrorCode.UNAUTHENTICATED, error.code)
        }
    }
}
