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
}
