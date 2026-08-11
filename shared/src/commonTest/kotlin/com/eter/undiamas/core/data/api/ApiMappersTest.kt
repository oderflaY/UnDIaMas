package com.eter.undiamas.core.data.api

import com.eter.undiamas.core.domain.model.Mood
import com.eter.undiamas.core.domain.model.RiskLevel
import com.eter.undiamas.core.domain.model.SupportRole
import com.eter.undiamas.core.domain.model.Trigger
import com.eter.undiamas.core.domain.model.TrustedContact
import com.eter.undiamas.core.domain.model.UserProfile
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

class ApiMappersTest {

    @Test
    fun `lee las fechas RFC3339 con desfase horario que manda el backend`() {
        val parsed = "2026-08-03T11:44:48.898645-06:00".toInstantOrNull()

        assertEquals(Instant.parse("2026-08-03T17:44:48.898645Z"), parsed)
    }

    @Test
    fun `una fecha ilegible no revienta el mapeo`() {
        assertNull("no es una fecha".toInstantOrNull())
        assertNull(null.toInstantOrNull())
    }

    /**
     * Lo más importante de este archivo: ante un valor corrupto o desconocido el semáforo
     * cae a VERDE. Inventar un ROJO dispararía el protocolo de emergencia de alguien sin
     * que haya pasado nada.
     */
    @Test
    fun `un nivel de riesgo desconocido cae a verde, nunca a rojo`() {
        assertEquals(RiskLevel.VERDE, "MORADO".toRiskLevel())
        assertEquals(RiskLevel.VERDE, "".toRiskLevel())
        assertEquals(RiskLevel.VERDE, null.toRiskLevel())
    }

    @Test
    fun `traduce los tres niveles del semaforo en ambos sentidos`() {
        assertEquals(RiskLevel.ROJO, "ROJO".toRiskLevel())
        assertEquals(RiskLevel.AMARILLO, "amarillo".toRiskLevel())
        assertEquals(RiskLevel.VERDE, "VERDE".toRiskLevel())
    }

    @Test
    fun `descarta detonantes desconocidos y conserva los validos`() {
        val triggers = listOf("ESTRES", "OVNIS", "SOLEDAD").toTriggers()

        assertEquals(setOf(Trigger.ESTRES, Trigger.SOLEDAD), triggers)
    }

    @Test
    fun `un animo desconocido cae a neutral`() {
        assertEquals(Mood.NEUTRAL, "EUFORICO".toMood())
        assertEquals(Mood.MUY_MAL, "MUY_MAL".toMood())
    }

    @Test
    fun `convierte un check-in del servidor al modelo de la app`() {
        val dto = CheckInDto(
            id = "abc",
            riskLevel = "AMARILLO",
            cravingLevel = 6,
            mood = "MAL",
            triggers = listOf("ESTRES"),
            note = "día difícil",
            answers = mapOf("impulso_consumo" to "no"),
            createdAt = "2026-08-03T11:45:11.117468-06:00",
        )

        val entry = dto.toDomain("user-1")

        assertEquals("abc", entry.id)
        assertEquals("user-1", entry.userId)
        assertEquals(RiskLevel.AMARILLO, entry.riskLevel)
        assertEquals(6, entry.urgeIntensity)
        assertEquals(setOf(Trigger.ESTRES), entry.triggers)
        assertEquals("día difícil", entry.note)
        assertEquals(mapOf("impulso_consumo" to "no"), entry.answers)
    }

    @Test
    fun `la peticion de check-in no lleva el id del usuario`() {
        val entry = CheckInDto(riskLevel = "ROJO", cravingLevel = 9).toDomain("user-1")

        val request = entry.toRequest()

        assertEquals("ROJO", request.riskLevel)
        assertEquals(9, request.cravingLevel)
        // El servidor rechaza con 400 cualquier campo que no conozca, y el id del usuario
        // lo saca del token: mandarlo sería, además de inútil, un 400 en cada check-in.
        val encoded = apiJson.encodeToString(CreateCheckInRequest.serializer(), request)
        assertTrue("userId" !in encoded, "la petición no debe llevar userId: $encoded")
    }

    @Test
    fun `arma el perfil juntando la ruta de usuario y la de tracker`() {
        val user = UserDto(
            id = "u1",
            displayName = "Ana",
            porQuePersonal = "por mi hija",
            recordRachaSegundos = 900_000,
            contactosEmergencia = listOf(
                ContactDto("Luis", "555-1", "PADRINO"),
                ContactDto("Mar", "555-2", "AMISTAD"),
            ),
        )
        val tracker = TrackerDto(
            startDate = "2026-07-01T00:00:00Z",
            dailySavingsRate = 120.5,
            rachaSegundos = 2_915_111,
        )

        val profile = buildProfile(user, tracker)

        assertEquals("Ana", profile.displayName)
        assertEquals("por mi hija", profile.personalWhy)
        assertEquals(900_000, profile.recordStreakSeconds)
        assertEquals(120.5, profile.previousDailyExpense)
        // El primero es el contacto de confianza: es a quien llama el protocolo de emergencia.
        assertEquals("Luis", profile.trustedContact?.name)
        assertEquals(SupportRole.PADRINO, profile.trustedContact?.role)
        assertEquals(listOf("Mar"), profile.supportNetwork.map { it.name })
    }

    @Test
    fun `un refresco del perfil no borra lo que el backend todavia no guarda`() {
        val previous = UserProfile(
            userId = "u1",
            displayName = "Ana",
            sobrietyStartDate = Instant.parse("2026-07-01T00:00:00Z"),
            addiction = com.eter.undiamas.core.domain.model.AddictionType.ALCOHOL,
        )

        val refreshed = buildProfile(UserDto(id = "u1", displayName = "Ana"), TrackerDto(), previous)

        assertEquals(com.eter.undiamas.core.domain.model.AddictionType.ALCOHOL, refreshed.addiction)
    }

    @Test
    fun `los contactos salen en el orden que espera el backend`() {
        val profile = UserProfile(
            userId = "u1",
            displayName = "Ana",
            sobrietyStartDate = Instant.parse("2026-07-01T00:00:00Z"),
            trustedContact = TrustedContact("Luis", "555-1", SupportRole.PADRINO),
            supportNetwork = listOf(TrustedContact("Mar", "555-2", SupportRole.AMISTAD)),
        )

        val dtos = profile.toContactDtos()

        assertEquals(listOf("Luis", "Mar"), dtos.map { it.nombre })
        assertEquals("PADRINO", dtos.first().rol)
    }
}
