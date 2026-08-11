package com.eter.undiamas.features.sobriedad.domain

import com.eter.undiamas.core.domain.model.UserProfile
import kotlin.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SobrietyCounterTest {
    private val counter = SobrietyCounter()
    private val start = Instant.fromEpochSeconds(0)

    @Test
    fun `calcula la racha activa en segundos desde el inicio de sobriedad`() {
        val profile = UserProfile(userId = "u1", displayName = "Ana", sobrietyStartDate = start)
        val now = Instant.fromEpochSeconds(60 * 60 * 24 * 3)

        val streak = counter.currentStreakSeconds(profile, now)

        assertEquals(60 * 60 * 24 * 3, streak)
    }

    @Test
    fun `al registrar una recaida la racha vuelve a cero pero se conserva el record`() {
        val profile = UserProfile(
            userId = "u1",
            displayName = "Ana",
            sobrietyStartDate = start,
            recordStreakSeconds = 60 * 60 * 24 * 10,
        )
        val relapseAt = Instant.fromEpochSeconds(60 * 60 * 24 * 5)

        val updated = counter.registerRelapse(profile, relapseAt)

        assertEquals(relapseAt, updated.sobrietyStartDate)
        assertEquals(60 * 60 * 24 * 10, updated.recordStreakSeconds)
    }

    @Test
    fun `el mensaje motivacional reconoce cuando aun no se supera el record`() {
        val message = counter.motivationalMessage(
            currentStreakSeconds = 60 * 60 * 24 * 3,
            recordStreakSeconds = 60 * 60 * 24 * 10,
        )

        assertTrue(message.isNotBlank())
    }

    @Test
    fun `el record es la racha actual cuando ya la supero`() {
        val profile = UserProfile(
            userId = "u1",
            displayName = "Ana",
            sobrietyStartDate = start,
            recordStreakSeconds = 60 * 60 * 24 * 30,
        )
        val now = Instant.fromEpochSeconds(60 * 60 * 24 * 42)

        // Enseñar "42 días · mejor racha 30" sería absurdo: la mejor racha es esta.
        assertEquals(60L * 60 * 24 * 42, counter.recordStreakSeconds(profile, now))
        assertTrue(counter.isPersonalBest(profile, now))
    }

    @Test
    fun `mientras no se alcanza el record, el record guardado manda`() {
        val profile = UserProfile(
            userId = "u1",
            displayName = "Ana",
            sobrietyStartDate = start,
            recordStreakSeconds = 60 * 60 * 24 * 47,
        )
        val now = Instant.fromEpochSeconds(60 * 60 * 24 * 12)

        assertEquals(60L * 60 * 24 * 47, counter.recordStreakSeconds(profile, now))
        assertTrue(!counter.isPersonalBest(profile, now))
    }

    @Test
    fun `sin record previo, la primera racha ya es la mejor`() {
        val profile = UserProfile(userId = "u1", displayName = "Ana", sobrietyStartDate = start)
        val now = Instant.fromEpochSeconds(60 * 60 * 24 * 3)

        assertEquals(60L * 60 * 24 * 3, counter.recordStreakSeconds(profile, now))
    }

    /**
     * Un reloj movido hacia atrás, o una fecha de inicio mal metida, no puede producir una
     * racha negativa: la pantalla enseñaría "-3 días", que no significa nada.
     */
    @Test
    fun `una fecha de inicio en el futuro da racha cero, no negativa`() {
        val profile = UserProfile(
            userId = "u1",
            displayName = "Ana",
            sobrietyStartDate = Instant.fromEpochSeconds(60 * 60 * 24 * 10),
        )
        val now = Instant.fromEpochSeconds(60 * 60 * 24 * 2)

        assertEquals(0L, counter.currentStreakSeconds(profile, now))
    }
}
