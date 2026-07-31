package com.eter.undiamas.core.presentation.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Punto de luz que viaja de un borde al otro a ritmo constante, para seguir solo con los ojos.
 *
 * Es un ejercicio de foco atencional bilateral. Deliberadamente NO se presenta como
 * "terapia EMDR": el EMDR clínico es un protocolo completo que aplica un profesional
 * entrenado, y hacer reprocesamiento de trauma sin acompañamiento puede desestabilizar
 * a alguien con trauma complejo. Aquí el objetivo es acotado: redirigir la atención.
 */
@Composable
fun BilateralFocus(
    dotColor: Color,
    modifier: Modifier = Modifier,
    cycleMillis: Int = 1_100,
) {
    val position by rememberInfiniteTransition(label = "bilateral").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            // FastOutSlowIn hace que el punto desacelere en los extremos, como un péndulo.
            animation = tween(cycleMillis, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "dot-position",
    )

    Box(modifier = modifier.background(Color.Black)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val margin = size.minDimension * 0.14f
            val travel = size.width - margin * 2
            val center = Offset(margin + travel * position, size.height / 2f)
            val radius = size.minDimension * 0.075f

            drawCircle(
                brush = Brush.radialGradient(
                    listOf(dotColor.copy(alpha = 0.55f), Color.Transparent),
                    center = center,
                    radius = radius * 3.4f,
                ),
                radius = radius * 3.4f,
                center = center,
            )
            drawCircle(color = dotColor, radius = radius, center = center)
        }
    }
}
