package com.eter.undiamas.features.calculadora.domain

import com.eter.undiamas.core.domain.model.SavingsGoal
import kotlin.math.pow

private const val SECONDS_PER_DAY = 60L * 60 * 24

class SavingsCalculator {
    fun dailySavings(previousDailyExpense: Double): Double = previousDailyExpense

    fun weeklySavings(previousDailyExpense: Double): Double = previousDailyExpense * 7

    fun monthlySavings(previousDailyExpense: Double): Double = previousDailyExpense * 30

    fun yearlySavings(previousDailyExpense: Double): Double = previousDailyExpense * 365

    fun totalSavings(previousDailyExpense: Double, sobrietyStreakSeconds: Long): Double =
        previousDailyExpense * (sobrietyStreakSeconds.toDouble() / SECONDS_PER_DAY)

    /** Fracción 0..1 de la meta ya cubierta, o null si aún no hay meta declarada. */
    fun goalProgress(saved: Double, goal: SavingsGoal?): Float? {
        if (goal == null) return null
        if (goal.targetAmount <= 0) return 0f
        return (saved / goal.targetAmount).coerceIn(0.0, 1.0).toFloat()
    }

    /**
     * Valor futuro de una anualidad: qué tendría la persona si invirtiera cada mes lo que
     * deja de gastar, a una tasa anual dada. Es una estimación ilustrativa, no una asesoría.
     */
    fun compoundProjection(monthlyContribution: Double, annualRate: Double, years: Int): Double {
        if (monthlyContribution <= 0 || years <= 0) return 0.0
        val monthlyRate = annualRate / 12
        val months = years * 12
        if (monthlyRate == 0.0) return monthlyContribution * months
        return monthlyContribution * (((1 + monthlyRate).pow(months) - 1) / monthlyRate)
    }
}
