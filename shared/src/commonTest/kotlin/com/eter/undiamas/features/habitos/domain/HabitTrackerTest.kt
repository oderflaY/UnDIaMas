package com.eter.undiamas.features.habitos.domain

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HabitTrackerTest {
    private val tracker = HabitTracker()
    private val hoy = LocalDate(2026, 7, 30)

    private fun day(offset: Int) = LocalDate.fromEpochDays(hoy.toEpochDays() + offset)
    private fun done(habitId: String, offset: Int) = HabitCompletion(habitId, day(offset))

    private val agua = Habit("agua", "u1", "Tomar agua")
    private val leer = Habit("leer", "u1", "Leer 10 páginas")

    @Test
    fun `marca si un habito se cumplio un dia dado`() {
        val completions = listOf(done("agua", 0))

        assertTrue(tracker.isDone(completions, "agua", hoy))
        assertFalse(tracker.isDone(completions, "leer", hoy))
        assertFalse(tracker.isDone(completions, "agua", day(-1)))
    }

    @Test
    fun `la cadena cuenta los dias consecutivos hasta hoy`() {
        val completions = listOf(done("agua", 0), done("agua", -1), done("agua", -2))

        assertEquals(3, tracker.currentStreak(completions, "agua", hoy))
    }

    @Test
    fun `si hoy no esta cumplido la cadena es cero aunque ayer si lo estuviera`() {
        val completions = listOf(done("agua", -1), done("agua", -2))

        assertEquals(0, tracker.currentStreak(completions, "agua", hoy))
    }

    @Test
    fun `un hueco intermedio corta la cadena actual`() {
        // Cumplido hoy y anteayer, pero ayer no: la cadena vale 1.
        val completions = listOf(done("agua", 0), done("agua", -2), done("agua", -3))

        assertEquals(1, tracker.currentStreak(completions, "agua", hoy))
    }

    @Test
    fun `la mejor cadena historica sobrevive aunque la actual se haya roto`() {
        val completions = listOf(
            done("agua", -10), done("agua", -9), done("agua", -8), done("agua", -7),
            done("agua", 0),
        )

        assertEquals(4, tracker.bestStreak(completions, "agua"))
        assertEquals(1, tracker.currentStreak(completions, "agua", hoy))
    }

    @Test
    fun `el porcentaje del dia refleja cuantos habitos se cumplieron`() {
        val habits = listOf(agua, leer)
        val completions = listOf(done("agua", 0))

        assertEquals(1, tracker.completedOn(completions, habits, hoy))
        assertEquals(0.5f, tracker.completionRate(completions, habits, hoy))
    }

    @Test
    fun `sin habitos definidos el porcentaje es cero y no se divide entre cero`() {
        assertEquals(0f, tracker.completionRate(emptyList(), emptyList(), hoy))
    }

    @Test
    fun `sin registros no hay cadena ni mejor cadena`() {
        assertEquals(0, tracker.currentStreak(emptyList(), "agua", hoy))
        assertEquals(0, tracker.bestStreak(emptyList(), "agua"))
    }

    @Test
    fun `un dia registrado dos veces no infla la cadena`() {
        val completions = listOf(done("agua", 0), done("agua", 0), done("agua", -1))

        assertEquals(2, tracker.currentStreak(completions, "agua", hoy))
        assertEquals(2, tracker.bestStreak(completions, "agua"))
    }
}
