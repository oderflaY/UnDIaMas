package com.eter.undiamas.core.presentation.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp

/**
 * Reloj circular masivo del búnker: el anillo de luz se va vaciando conforme corre el tiempo
 * y late muy suavemente para que la pantalla se sienta viva sin distraer.
 */
@Composable
fun CountdownRing(
    remainingFraction: Float,
    countdown: String,
    caption: String,
    ringColors: List<Color>,
    modifier: Modifier = Modifier,
    size: Int = 300,
) {
    val animated by animateFloatAsState(
        targetValue = remainingFraction.coerceIn(0f, 1f),
        animationSpec = tween(900, easing = LinearEasing),
        label = "countdown",
    )
    val glow by rememberInfiniteTransition(label = "glow").animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2600), RepeatMode.Reverse),
        label = "glow-alpha",
    )

    Box(modifier = modifier.size(size.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(size.dp)) {
            val stroke = this.size.minDimension * 0.055f
            val inset = stroke / 2
            val arcSize = Size(this.size.width - stroke, this.size.height - stroke)
            val topLeft = Offset(inset, inset)
            val center = Offset(this.size.width / 2f, this.size.height / 2f)

            // Halo tenue detrás del anillo.
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(ringColors.first().copy(alpha = 0.16f * glow), Color.Transparent),
                    center = center,
                    radius = this.size.minDimension / 2f,
                ),
                radius = this.size.minDimension / 2f,
                center = center,
            )
            // Pista completa, muy apagada.
            drawArc(
                color = Color.White.copy(alpha = 0.08f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
            // Luz restante: se vacía en sentido horario conforme baja el tiempo.
            drawArc(
                brush = Brush.sweepGradient(ringColors),
                startAngle = -90f,
                sweepAngle = 360f * animated,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                countdown,
                style = MaterialTheme.typography.displayLarge,
                fontFamily = FontFamily.Monospace,
                color = Color.White,
            )
            Text(
                caption,
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.6f),
            )
        }
    }
}
