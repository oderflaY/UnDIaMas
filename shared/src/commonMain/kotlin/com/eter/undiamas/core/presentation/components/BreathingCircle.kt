package com.eter.undiamas.core.presentation.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp

private const val INHALE_END = 4f
private const val HOLD_END = 8f
private const val CYCLE = 14f

/**
 * Guía de respiración 4-4-6 del protocolo de emergencia: el círculo crece al inhalar,
 * se sostiene y decrece al exhalar. Cada cambio de fase dispara una vibración suave para
 * que se pueda seguir el ritmo con los ojos cerrados.
 */
@Composable
fun BreathingCircle(
    colors: List<Color>,
    modifier: Modifier = Modifier,
    size: Int = 240,
    hapticsEnabled: Boolean = true,
) {
    val t by rememberInfiniteTransition(label = "breath").animateFloat(
        initialValue = 0f,
        targetValue = CYCLE,
        animationSpec = infiniteRepeatable(tween(14_000, easing = LinearEasing), RepeatMode.Restart),
        label = "cycle",
    )

    val scale = when {
        t < INHALE_END -> 0.5f + 0.5f * (t / INHALE_END)
        t < HOLD_END -> 1f
        else -> 1f - 0.5f * ((t - HOLD_END) / (CYCLE - HOLD_END))
    }
    val phase = when {
        t < INHALE_END -> "Inhala suavemente…"
        t < HOLD_END -> "Sostén el aire…"
        else -> "Exhala lentamente…"
    }

    val haptics = LocalHapticFeedback.current
    var lastPhase by remember { mutableStateOf("") }
    LaunchedEffect(phase) {
        if (hapticsEnabled && lastPhase.isNotEmpty()) {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        }
        lastPhase = phase
    }

    Box(modifier = modifier.size(size.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(size.dp)) {
            val maxRadius = this.size.minDimension / 2f
            // Halo exterior fijo que da el marco del ejercicio.
            drawCircle(
                brush = Brush.radialGradient(listOf(colors.first().copy(alpha = 0.30f), Color.Transparent)),
                radius = maxRadius,
            )
            // Anillo intermedio que acompaña el movimiento a menor amplitud.
            drawCircle(
                color = colors.last().copy(alpha = 0.18f),
                radius = maxRadius * (0.55f + scale * 0.3f),
            )
            drawCircle(
                brush = Brush.linearGradient(colors),
                radius = maxRadius * scale * 0.68f,
            )
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(phase, style = MaterialTheme.typography.titleMedium, color = Color.White)
        }
    }
}
