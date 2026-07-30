package com.eter.undiamas.core.presentation

fun formatStreak(totalSeconds: Long): String {
    val days = totalSeconds / (60 * 60 * 24)
    val hours = (totalSeconds % (60 * 60 * 24)) / (60 * 60)
    return when {
        days > 0 -> "$days ${if (days == 1L) "día" else "días"}, $hours h"
        else -> "$hours h"
    }
}
