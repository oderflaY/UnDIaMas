package com.eter.undiamas.core.data.local

import com.eter.undiamas.core.domain.model.CheckInEntry
import com.eter.undiamas.core.domain.model.Mood
import com.eter.undiamas.core.domain.model.MoodEntry
import com.eter.undiamas.core.domain.model.RiskLevel
import com.eter.undiamas.core.domain.model.Trigger
import com.eter.undiamas.core.domain.model.UserProfile
import com.eter.undiamas.features.diario.domain.DiaryEntry
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Instant

class LocalStoreTest {

    private fun store() = LocalStore(UndiamasDatabase(":memory:"))

    private fun checkIn(id: String, riesgo: RiskLevel = RiskLevel.VERDE, nota: String = "") =
        CheckInEntry(
            id = id,
            userId = "u1",
            answeredAt = Instant.parse("2026-08-03T12:00:00Z"),
            answers = mapOf("impulso_consumo" to "no"),
            riskLevel = riesgo,
            triggers = setOf(Trigger.ESTRES),
            urgeIntensity = 4,
            note = nota,
        )

    @Test
    fun `guarda y devuelve un check-in completo`() = runTest {
        val store = store()
        store.abrirSesion("u1")

        store.guardarCheckIn(checkIn("c1", RiskLevel.ROJO, "día duro"), Mood.MUY_MAL, pendiente = true)

        val guardado = store.checkIns.value.single()
        assertEquals("c1", guardado.id)
        assertEquals(RiskLevel.ROJO, guardado.riskLevel)
        assertEquals(setOf(Trigger.ESTRES), guardado.triggers)
        assertEquals(4, guardado.urgeIntensity)
        assertEquals("día duro", guardado.note)
        assertEquals(mapOf("impulso_consumo" to "no"), guardado.answers)
    }

    @Test
    fun `marca lo que todavia no salio del telefono`() = runTest {
        val store = store()
        store.abrirSesion("u1")

        store.guardarCheckIn(checkIn("c1"), Mood.NEUTRAL, pendiente = true)
        store.guardarCheckIn(checkIn("c2"), Mood.NEUTRAL, pendiente = false)

        assertEquals(setOf("c1"), store.sinEnviar.value)
    }

    /**
     * El refresco del servidor no puede llevarse por delante lo que sigue en la cola: ese
     * check-in existe en el teléfono de alguien y todavía no llegó a Postgres.
     */
    @Test
    fun `refrescar desde el servidor no borra lo pendiente`() = runTest {
        val store = store()
        store.abrirSesion("u1")
        store.guardarCheckIn(checkIn("local-1", nota = "sin enviar"), Mood.NEUTRAL, pendiente = true)
        store.guardarCheckIn(checkIn("viejo"), Mood.NEUTRAL, pendiente = false)

        store.reemplazarCheckIns(listOf(checkIn("s1") to Mood.BIEN, checkIn("s2") to Mood.MAL))

        val ids = store.checkIns.value.map { it.id }.toSet()
        assertTrue("local-1" in ids, "lo pendiente debe seguir ahí")
        assertTrue("s1" in ids && "s2" in ids)
        assertFalse("viejo" in ids, "lo ya confirmado sí se reemplaza")
    }

    @Test
    fun `al confirmar, el id provisional se cambia por el del servidor`() = runTest {
        val store = store()
        store.abrirSesion("u1")
        store.guardarCheckIn(checkIn("local-abc"), Mood.NEUTRAL, pendiente = true)

        store.confirmarCheckIn("local-abc", "servidor-123")

        assertEquals("servidor-123", store.checkIns.value.single().id)
        assertTrue(store.sinEnviar.value.isEmpty(), "ya no está pendiente")
    }

    @Test
    fun `confirmar cuando el refresco ya trajo la fila no la duplica`() = runTest {
        val store = store()
        store.abrirSesion("u1")
        store.guardarCheckIn(checkIn("local-abc"), Mood.NEUTRAL, pendiente = true)
        store.guardarCheckIn(checkIn("servidor-123"), Mood.NEUTRAL, pendiente = false)

        store.confirmarCheckIn("local-abc", "servidor-123")

        assertEquals(1, store.checkIns.value.size)
        assertEquals("servidor-123", store.checkIns.value.single().id)
    }

    @Test
    fun `el diario ordena de mas reciente a mas antiguo`() = runTest {
        val store = store()
        store.abrirSesion("u1")

        store.guardarEntradaDiario(
            DiaryEntry("d1", "u1", Instant.parse("2026-08-01T10:00:00Z"), "primera"),
            pendiente = false,
        )
        store.guardarEntradaDiario(
            DiaryEntry("d2", "u1", Instant.parse("2026-08-03T10:00:00Z"), "última"),
            pendiente = false,
        )

        assertEquals(listOf("última", "primera"), store.diario.value.map { it.text })
    }

    @Test
    fun `guarda el perfil con la racha y el ahorro`() = runTest {
        val store = store()
        store.abrirSesion("u1")
        val perfil = UserProfile(
            userId = "u1",
            displayName = "Ana",
            sobrietyStartDate = Instant.parse("2026-07-01T00:00:00Z"),
            personalWhy = "por mi hija",
        )

        store.guardarPerfil(perfil, rachaSegundos = 2_915_111, ahorro = 4065.6)

        val leido = assertNotNull(store.perfil.value)
        assertEquals("Ana", leido.displayName)
        assertEquals("por mi hija", leido.personalWhy)
        assertEquals(2_915_111, store.rachaSegundos.value)
        assertEquals(4065.6, store.ahorro.value)
    }

    /**
     * La protección más importante de la caché: dos personas que comparten un teléfono no
     * pueden verse el historial de recaídas la una a la otra.
     */
    @Test
    fun `entrar con otra cuenta borra los datos de la anterior`() = runTest {
        val store = store()
        store.abrirSesion("ana")
        store.guardarCheckIn(checkIn("c1", nota = "privado de Ana"), Mood.MAL, pendiente = false)
        store.guardarEntradaDiario(
            DiaryEntry("d1", "ana", Instant.parse("2026-08-01T10:00:00Z"), "diario de Ana"),
            pendiente = false,
        )

        val huboCambio = store.abrirSesion("luis")

        assertTrue(huboCambio)
        assertTrue(store.checkIns.value.isEmpty(), "no puede quedar nada de la cuenta anterior")
        assertTrue(store.diario.value.isEmpty())
    }

    @Test
    fun `volver a entrar con la misma cuenta conserva los datos`() = runTest {
        val store = store()
        store.abrirSesion("ana")
        store.guardarCheckIn(checkIn("c1"), Mood.NEUTRAL, pendiente = false)

        val huboCambio = store.abrirSesion("ana")

        assertFalse(huboCambio)
        assertEquals(1, store.checkIns.value.size)
    }

    @Test
    fun `un animo desconocido en la base cae a neutral`() = runTest {
        val store = store()
        store.abrirSesion("u1")

        store.guardarAnimo(
            MoodEntry("a1", "u1", Mood.MUY_BIEN, Instant.parse("2026-08-03T10:00:00Z")),
            pendiente = false,
        )

        assertEquals(Mood.MUY_BIEN, store.animos.value.single().mood)
    }

    @Test
    fun `borrarlo todo deja la base vacia`() = runTest {
        val store = store()
        store.abrirSesion("u1")
        store.guardarCheckIn(checkIn("c1"), Mood.NEUTRAL, pendiente = false)
        store.guardarAnimo(
            MoodEntry("a1", "u1", Mood.BIEN, Instant.parse("2026-08-03T10:00:00Z")),
            pendiente = false,
        )

        store.borrarTodo()

        assertTrue(store.checkIns.value.isEmpty())
        assertTrue(store.animos.value.isEmpty())
    }
}
