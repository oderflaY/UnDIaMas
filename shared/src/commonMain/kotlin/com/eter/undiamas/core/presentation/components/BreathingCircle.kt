package com.eter.undiamas.core.presentation.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private const val INHALE = 4f
private const val HOLD = 8f
private const val CYCLE = 14f

/**
 * Guía de respiración 4-4-6 del protocolo de emergencia: el círculo crece al inhalar,
 * se sostiene y decrece al exhalar, con la instrucción sincronizada al centro.
 */
@Composable
fun BreathingCircle(colors: List<Color>, modifier: Modifier = Modifier, size: Int = 220) {
    val t by rememberInfiniteTransition(label = "breath").animateFloat(
        initialValue = 0f,
        targetValue = CYCLE,
        animationSpec = infiniteRepeatable(tween(14_000, easing = LinearEasing), RepeatMode.Restart),
        label = "cycle",
    )

    val scale = when {
        t < INHALE -> 0.55f + 0.45f * (t / INHALE)
        t < HOLD -> 1f
        else -> 1f - 0.45f * ((t - HOLD) / (CYCLE - HOLD))
    }
    val phase = when {
        t < INHALE -> "Inhala"
        t < HOLD -> "Sostén"
        else -> "Exhala"
    }

    Box(modifier = modifier.size(size.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(size.dp)) {
            val maxRadius = this.size.minDimension / 2f
            drawCircle(
                brush = Brush.radialGradient(listOf(colors.first().copy(alpha = 0.30f), Color.Transparent)),
                radius = maxRadius,
            )
            drawCircle(
                brush = Brush.linearGradient(colors),
                radius = maxRadius * scale * 0.72f,
            )
        }
        Text(phase, style = MaterialTheme.typography.titleLarge, color = Color.White)
    }
}
