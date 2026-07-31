package com.eter.undiamas.core.data.firebase

import com.eter.undiamas.core.domain.model.CheckInEntry
import com.eter.undiamas.core.domain.model.RiskLevel
import com.eter.undiamas.core.domain.model.SupportRole
import com.eter.undiamas.core.domain.model.Trigger
import com.eter.undiamas.core.domain.model.TrustedContact
import com.eter.undiamas.core.domain.model.UserProfile
import kotlin.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals

class FirestoreMappersTest {
    @Test
    fun `el perfil sobrevive el viaje de ida y vuelta a UsuarioDoc`() {
        val profile = UserProfile(
            userId = "user-1",
            displayName = "Ana",
            sobrietyStartDate = Instant.fromEpochSeconds(1000),
            recordStreakSeconds = 500,
            previousDailyExpense = 120.0,
            trustedContact = TrustedContact("Beto", "+525500000000", SupportRole.PADRINO),
            personalWhy = "Mis hijos",
        )

        val roundTripped = profile.toUsuarioDoc().toUserProfile(uid = "user-1")

        assertEquals(profile.displayName, roundTripped.displayName)
        assertEquals(profile.sobrietyStartDate, roundTripped.sobrietyStartDate)
        assertEquals(profile.recordStreakSeconds, roundTripped.recordStreakSeconds)
        assertEquals(profile.previousDailyExpense, roundTripped.previousDailyExpense)
        assertEquals(profile.trustedContact, roundTripped.trustedContact)
        assertEquals(profile.personalWhy, roundTripped.personalWhy)
    }

    @Test
    fun `un check-in sobrevive el viaje de ida y vuelta a CheckInDoc`() {
        val entry = CheckInEntry(
            id = "ignored",
            userId = "ignored",
            answeredAt = Instant.fromEpochSeconds(2000),
            answers = mapOf("estado_animo" to "bien", "detonante_presente" to "no"),
            riskLevel = RiskLevel.AMARILLO,
            triggers = setOf(Trigger.ESTRES, Trigger.CANSANCIO),
            urgeIntensity = 4,
            note = "dia dificil",
        )

        val roundTripped = entry.toCheckInDoc().toCheckInEntry(id = "checkin-9", uid = "user-1")

        assertEquals("checkin-9", roundTripped.id)
        assertEquals("user-1", roundTripped.userId)
        assertEquals(entry.answeredAt, roundTripped.answeredAt)
        assertEquals(entry.riskLevel, roundTripped.riskLevel)
        assertEquals(entry.triggers, roundTripped.triggers)
        assertEquals(entry.urgeIntensity, roundTripped.urgeIntensity)
        assertEquals(entry.note, roundTripped.note)
        assertEquals(entry.answers, roundTripped.answers)
    }
}
