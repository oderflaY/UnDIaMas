package com.eter.undiamas.features.calculadora.domain

import com.eter.undiamas.core.domain.model.SavingsGoal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SavingsProjectionTest {
    private val calculator = SavingsCalculator()

    @Test
    fun `el ahorro anual es el diario por 365`() {
        assertEquals(18_250.0, calculator.yearlySavings(previousDailyExpense = 50.0))
    }

    @Test
    fun `el progreso hacia la meta es la fraccion ahorrada`() {
        val goal = SavingsGoal(title = "Laptop", targetAmount = 10_000.0)

        assertEquals(0.25f, calculator.goalProgress(saved = 2_500.0, goal = goal))
    }

    @Test
    fun `el progreso se limita a uno aunque se supere la meta`() {
        val goal = SavingsGoal(title = "Laptop", targetAmount = 10_000.0)

        assertEquals(1f, calculator.goalProgress(saved = 25_000.0, goal = goal))
    }

    @Test
    fun `una meta sin monto no produce progreso divisible`() {
        val goal = SavingsGoal(title = "Sin monto", targetAmount = 0.0)

        assertEquals(0f, calculator.goalProgress(saved = 500.0, goal = goal))
    }

    @Test
    fun `sin meta declarada no hay progreso que mostrar`() {
        assertNull(calculator.goalProgress(saved = 500.0, goal = null))
    }

    @Test
    fun `el interes compuesto supera lo aportado tras varios anos`() {
        val aportacionMensual = 1_000.0
        val proyeccion = calculator.compoundProjection(
            monthlyContribution = aportacionMensual,
            annualRate = 0.10,
            years = 5,
        )

        assertTrue(proyeccion > aportacionMensual * 12 * 5)
    }

    @Test
    fun `sin aportacion la proyeccion es cero`() {
        assertEquals(0.0, calculator.compoundProjection(monthlyContribution = 0.0, annualRate = 0.10, years = 5))
    }
}
