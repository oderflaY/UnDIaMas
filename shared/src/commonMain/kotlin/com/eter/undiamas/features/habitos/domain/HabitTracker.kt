package com.eter.undiamas.features.habitos.domain

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.serialization.Serializable

/** Micro-hábito que sustituye la conducta anterior con una fuente de dopamina nueva. */
@Serializable
data class Habit(
    val id: String,
    val userId: String,
    val name: String,
    val targetPerDay: Int = 1,
)

/** Marca de que un hábito se cumplió un día concreto. */
@Serializable
data class HabitCompletion(
    val habitId: String,
    val date: LocalDate,
)

/**
 * Cálculos de "no romper la cadena".
 *
 * La cadena solo cuenta hacia atrás desde hoy: si ayer se rompió, la racha es 0 aunque
 * haya semanas perfectas más atrás. Es lo que hace que la cadena signifique algo.
 */
class HabitTracker {

    fun isDone(completions: List<HabitCompletion>, habitId: String, date: LocalDate): Boolean =
        completions.any { it.habitId == habitId && it.date == date }

    /** Días consecutivos cumplidos terminando hoy. */
    fun currentStreak(completions: List<HabitCompletion>, habitId: String, today: LocalDate): Int {
        val done = completions.filter { it.habitId == habitId }.map { it.date }.toSet()
        var streak = 0
        var cursor = today
        while (cursor in done) {
            streak += 1
            cursor = cursor.minus(DatePeriod(days = 1))
        }
        return streak
    }

    /** Cuántos hábitos se cumplieron ese día. */
    fun completedOn(completions: List<HabitCompletion>, habits: List<Habit>, date: LocalDate): Int =
        habits.count { isDone(completions, it.id, date) }

    /**
     * Porcentaje de hábitos cumplidos ese día, de 0 a 1.
     * Sin hábitos definidos devuelve 0: no tiene sentido decir que se cumplió el 100% de nada.
     */
    fun completionRate(
        completions: List<HabitCompletion>,
        habits: List<Habit>,
        date: LocalDate,
    ): Float {
        if (habits.isEmpty()) return 0f
        return completedOn(completions, habits, date).toFloat() / habits.size
    }

    /** La mejor cadena histórica del hábito, aunque ya se haya roto. */
    fun bestStreak(completions: List<HabitCompletion>, habitId: String): Int {
        val dates = completions.filter { it.habitId == habitId }.map { it.date }.distinct().sorted()
        if (dates.isEmpty()) return 0

        var best = 1
        var run = 1
        dates.zipWithNext { previous, current ->
            if (current == previous.plus(DatePeriod(days = 1))) {
                run += 1
                if (run > best) best = run
            } else {
                run = 1
            }
        }
        return best
    }
}
