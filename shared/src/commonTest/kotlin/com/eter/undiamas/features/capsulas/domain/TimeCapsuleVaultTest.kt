package com.eter.undiamas.features.capsulas.domain

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TimeCapsuleVaultTest {
    private val vault = TimeCapsuleVault()
    private val hoy = LocalDate(2026, 7, 30)

    private fun capsule(unlock: LocalDate, id: String = "c") = TimeCapsule(
        id = id,
        userId = "u1",
        title = "Para mí",
        message = "Aguanta.",
        createdOn = LocalDate(2026, 7, 1),
        unlockOn = unlock,
    )

    @Test
    fun `una capsula con fecha futura sigue cerrada`() {
        assertFalse(vault.isUnlocked(capsule(LocalDate(2026, 8, 30)), hoy))
    }

    @Test
    fun `una capsula se abre el mismo dia de su fecha`() {
        assertTrue(vault.isUnlocked(capsule(hoy), hoy))
    }

    @Test
    fun `una capsula con fecha pasada queda abierta`() {
        assertTrue(vault.isUnlocked(capsule(LocalDate(2026, 7, 1)), hoy))
    }

    @Test
    fun `cuenta los dias que faltan para abrirla`() {
        assertEquals(12, vault.daysUntilUnlock(capsule(LocalDate(2026, 8, 11)), hoy))
    }

    @Test
    fun `una capsula ya disponible no reporta dias pendientes`() {
        assertEquals(0, vault.daysUntilUnlock(capsule(LocalDate(2026, 7, 1)), hoy))
        assertEquals(0, vault.daysUntilUnlock(capsule(hoy), hoy))
    }

    @Test
    fun `separa las abiertas de las cerradas y ordena las cerradas por cercania`() {
        val lejana = capsule(LocalDate(2026, 12, 1), "lejana")
        val cercana = capsule(LocalDate(2026, 8, 2), "cercana")
        val abierta = capsule(LocalDate(2026, 7, 10), "abierta")

        val locked = vault.locked(listOf(lejana, cercana, abierta), hoy)
        val unlocked = vault.unlocked(listOf(lejana, cercana, abierta), hoy)

        assertEquals(listOf("cercana", "lejana"), locked.map { it.id })
        assertEquals(listOf("abierta"), unlocked.map { it.id })
    }

    @Test
    fun `sin capsulas ambas listas quedan vacias`() {
        assertTrue(vault.locked(emptyList(), hoy).isEmpty())
        assertTrue(vault.unlocked(emptyList(), hoy).isEmpty())
    }
}
