package com.eter.undiamas.features.sobriedad.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MilestonesTest {
    private val milestones = Milestones()

    @Test
    fun `el proximo hito es el primero que aun no se alcanza`() {
        val next = milestones.next(currentDays = 26)

        assertEquals(30, next?.days)
        assertEquals(4, milestones.daysUntilNext(currentDays = 26))
    }

    @Test
    fun `justo al alcanzar un hito el proximo pasa a ser el siguiente`() {
        assertEquals(60, milestones.next(currentDays = 30)?.days)
    }

    @Test
    fun `los hitos alcanzados incluyen todos los que ya se superaron`() {
        val reached = milestones.reached(currentDays = 35).map { it.days }

        assertEquals(listOf(1, 7, 14, 30), reached)
    }

    @Test
    fun `sin dias todavia no hay hitos alcanzados`() {
        assertTrue(milestones.reached(currentDays = 0).isEmpty())
    }

    @Test
    fun `superado el ultimo hito ya no hay siguiente`() {
        val ultimo = milestones.all.last().days.toLong()

        assertNull(milestones.next(currentDays = ultimo))
        assertNull(milestones.daysUntilNext(currentDays = ultimo))
    }
}
