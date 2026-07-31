package com.eter.undiamas.core.presentation.components

import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private const val PARTICLE_COUNT = 7

/**
 * Anillo de racha: trazo con degradado que gira lentamente, partículas flotando sobre
 * la órbita y el contador vivo al centro (días grandes + reloj con efecto odómetro).
 */
@Composable
fun StreakRing(
    progress: Float,
    days: String,
    clock: String,
    ringColors: List<Color>,
    trackColor: Color,
    modifier: Modifier = Modifier,
    size: Int = 200,
    contentColor: Color = Color.White,
    companion: (@Composable () -> Unit)? = null,
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 900),
        label = "ring-progress",
    )
    val transition = rememberInfiniteTransition(label = "ring")
    val spin by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(18_000, easing = LinearEasing)),
        label = "spin",
    )
    val drift by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(6_000, easing = LinearEasing)),
        label = "drift",
    )

    Box(modifier = modifier.size(size.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(size.dp)) {
            val stroke = this.size.minDimension * 0.085f
            val inset = stroke / 2
            val arcSize = Size(this.size.width - stroke, this.size.height - stroke)
            val topLeft = Offset(inset, inset)
            val radius = (this.size.minDimension - stroke) / 2f
            val center = Offset(this.size.width / 2f, this.size.height / 2f)

            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
            // El degradado gira, el progreso no: así el anillo "respira" sin mentir sobre el avance.
            rotate(degrees = spin, pivot = center) {
                drawArc(
                    brush = Brush.sweepGradient(ringColors),
                    startAngle = -90f,
                    sweepAngle = 360f * animatedProgress,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
            }

            // Partículas flotantes sobre la órbita del anillo.
            repeat(PARTICLE_COUNT) { index ->
                val base = (2 * PI * index / PARTICLE_COUNT).toFloat()
                val angle = base + drift
                val wobble = sin(drift * 2 + index) * stroke * 0.55f
                val r = radius + wobble
                val particleCenter = Offset(
                    center.x + r * cos(angle),
                    center.y + r * sin(angle),
                )
                val alpha = 0.25f + 0.55f * ((sin(drift * 1.5f + index) + 1f) / 2f)
                drawCircle(
                    color = Color.White.copy(alpha = alpha),
                    radius = stroke * 0.16f,
                    center = particleCenter,
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            companion?.invoke()
            OdometerText(
                value = days,
                style = MaterialTheme.typography.displaySmall,
                color = contentColor,
            )
            Text(
                "DÍAS",
                style = MaterialTheme.typography.labelMedium,
                color = contentColor.copy(alpha = 0.85f),
                textAlign = TextAlign.Center,
            )
            OdometerText(
                value = clock,
                style = MaterialTheme.typography.titleMedium,
                color = contentColor.copy(alpha = 0.9f),
            )
        }
    }
}
