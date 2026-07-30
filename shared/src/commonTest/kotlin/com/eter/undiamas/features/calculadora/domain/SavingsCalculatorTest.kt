package com.eter.undiamas.features.calculadora.domain

import kotlin.test.Test
import kotlin.test.assertEquals

class SavingsCalculatorTest {
    private val calculator = SavingsCalculator()

    @Test
    fun `el ahorro diario equivale al gasto previo declarado`() {
        assertEquals(50.0, calculator.dailySavings(previousDailyExpense = 50.0))
    }

    @Test
    fun `el ahorro semanal es el diario multiplicado por 7`() {
        assertEquals(350.0, calculator.weeklySavings(previousDailyExpense = 50.0))
    }

    @Test
    fun `el ahorro mensual es el diario multiplicado por 30`() {
        assertEquals(1500.0, calculator.monthlySavings(previousDailyExpense = 50.0))
    }

    @Test
    fun `el ahorro total depende de la racha de sobriedad en segundos`() {
        val threeDaysInSeconds = 60L * 60 * 24 * 3

        assertEquals(150.0, calculator.totalSavings(previousDailyExpense = 50.0, sobrietyStreakSeconds = threeDaysInSeconds))
    }
}
