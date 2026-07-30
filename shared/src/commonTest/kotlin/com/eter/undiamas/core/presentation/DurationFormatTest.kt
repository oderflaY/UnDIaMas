package com.eter.undiamas.core.presentation

import kotlin.test.Test
import kotlin.test.assertEquals

class DurationFormatTest {
    @Test
    fun `el reloj muestra los segundos desde el primer segundo`() {
        assertEquals("00:00:01", formatClock(1))
        assertEquals("00:00:59", formatClock(59))
    }

    @Test
    fun `el reloj acumula minutos y horas dentro del dia`() {
        assertEquals("00:01:00", formatClock(60))
        assertEquals("01:00:00", formatClock(3600))
        assertEquals("23:59:59", formatClock(86_399))
    }

    @Test
    fun `el reloj reinicia a cero al cruzar un dia completo`() {
        assertEquals("00:00:00", formatClock(86_400))
        assertEquals("00:00:05", formatClock(86_405))
    }

    @Test
    fun `los dias completos se cuentan aparte del reloj`() {
        assertEquals(0, streakDays(86_399))
        assertEquals(1, streakDays(86_400))
        assertEquals(3, streakDays(86_400 * 3 + 10))
    }

    @Test
    fun `la racha en prosa incluye dias solo cuando ya hay al menos uno`() {
        assertEquals("00:00:30", formatStreak(30))
        assertEquals("2d 01:00:00", formatStreak(86_400 * 2 + 3600))
    }

    @Test
    fun `una racha negativa por reloj desfasado no rompe el formato`() {
        assertEquals("00:00:00", formatClock(-5))
        assertEquals("00:00:00", formatStreak(-5))
    }
}
