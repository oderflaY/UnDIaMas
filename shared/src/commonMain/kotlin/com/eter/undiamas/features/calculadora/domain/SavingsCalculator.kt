package com.eter.undiamas.features.calculadora.domain

private const val SECONDS_PER_DAY = 60L * 60 * 24

class SavingsCalculator {
    fun dailySavings(previousDailyExpense: Double): Double = previousDailyExpense

    fun weeklySavings(previousDailyExpense: Double): Double = previousDailyExpense * 7

    fun monthlySavings(previousDailyExpense: Double): Double = previousDailyExpense * 30

    fun totalSavings(previousDailyExpense: Double, sobrietyStreakSeconds: Long): Double =
        previousDailyExpense * (sobrietyStreakSeconds.toDouble() / SECONDS_PER_DAY)
}
