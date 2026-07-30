package com.eter.undiamas.core.presentation

private const val SECONDS_PER_DAY = 60L * 60 * 24

private fun pad(value: Long): String = if (value < 10) "0$value" else "$value"

/** Días completos de la racha. */
fun streakDays(totalSeconds: Long): Long = totalSeconds / SECONDS_PER_DAY

/** Horas:minutos:segundos transcurridos dentro del día en curso, para el contador vivo. */
fun formatClock(totalSeconds: Long): String {
    val safe = totalSeconds.coerceAtLeast(0)
    val hours = (safe % SECONDS_PER_DAY) / 3600
    val minutes = (safe % 3600) / 60
    val seconds = safe % 60
    return "${pad(hours)}:${pad(minutes)}:${pad(seconds)}"
}

/** Racha completa en prosa corta, con segundos incluidos. */
fun formatStreak(totalSeconds: Long): String {
    val safe = totalSeconds.coerceAtLeast(0)
    val days = streakDays(safe)
    return if (days > 0) "${days}d ${formatClock(safe)}" else formatClock(safe)
}
