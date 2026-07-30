package com.eter.undiamas.core.presentation.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.eter.undiamas.core.domain.model.RiskLevel
import com.eter.undiamas.core.presentation.theme.RiskGreen
import com.eter.undiamas.core.presentation.theme.RiskRed
import com.eter.undiamas.core.presentation.theme.RiskYellow

/**
 * Semáforo de riesgo: tres luces en fila, la activa con un halo que late.
 * Con [level] nulo (aún sin check-in) las tres quedan apagadas.
 */
@Composable
fun TrafficLight(level: RiskLevel?, modifier: Modifier = Modifier) {
    val pulse by rememberInfiniteTransition(label = "traffic").animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1400), RepeatMode.Reverse),
        label = "pulse",
    )

    val lights = listOf(RiskLevel.VERDE to RiskGreen, RiskLevel.AMARILLO to RiskYellow, RiskLevel.ROJO to RiskRed)

    Canvas(modifier = modifier) {
        val radius = size.height / 2f
        val gap = radius * 0.85f
        var cx = radius

        lights.forEach { (lightLevel, color) ->
            val isActive = lightLevel == level
            val center = Offset(cx, size.height / 2f)

            if (isActive) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(color.copy(alpha = 0.45f * pulse), Color.Transparent),
                        center = center,
                        radius = radius * 2.1f,
                    ),
                    radius = radius * 2.1f,
                    center = center,
                )
            }
            drawCircle(
                color = if (isActive) color else color.copy(alpha = 0.18f),
                radius = radius * 0.78f,
                center = center,
            )
            cx += radius * 2 + gap
        }
    }
}
