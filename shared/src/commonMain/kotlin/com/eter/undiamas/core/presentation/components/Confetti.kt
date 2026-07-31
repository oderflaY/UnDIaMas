package com.eter.undiamas.core.presentation.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import com.eter.undiamas.core.presentation.theme.AccentStats
import com.eter.undiamas.core.presentation.theme.PrimaryVioletStart
import com.eter.undiamas.core.presentation.theme.RiskGreen
import com.eter.undiamas.core.presentation.theme.RiskYellow
import kotlin.math.sin
import kotlin.random.Random

private const val PIECES = 40
private val palette = listOf(RiskGreen, RiskYellow, PrimaryVioletStart, AccentStats, Color(0xFFEC4899))

private data class Piece(val x: Float, val delay: Float, val drift: Float, val color: Color, val size: Float)

/** Lluvia de confeti que celebra un check-in en verde o amarillo. Se dibuja una sola vez. */
@Composable
fun Confetti(modifier: Modifier = Modifier, durationMillis: Int = 2200) {
    val pieces = remember {
        val random = Random(0)
        List(PIECES) {
            Piece(
                x = random.nextFloat(),
                delay = random.nextFloat() * 0.35f,
                drift = (random.nextFloat() - 0.5f) * 2f,
                color = palette[random.nextInt(palette.size)],
                size = 4f + random.nextFloat() * 6f,
            )
        }
    }
    var started by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { started = true }

    val t by animateFloatAsState(
        targetValue = if (started) 1f else 0f,
        animationSpec = tween(durationMillis, easing = LinearEasing),
        label = "confetti",
    )

    Canvas(modifier = modifier) {
        pieces.forEach { piece ->
            val local = ((t - piece.delay) / (1f - piece.delay)).coerceIn(0f, 1f)
            if (local <= 0f) return@forEach
            val y = local * size.height
            val x = piece.x * size.width + sin(local * 6f) * piece.drift * 24f
            drawRect(
                color = piece.color.copy(alpha = (1f - local).coerceIn(0f, 1f)),
                topLeft = Offset(x, y),
                size = Size(piece.size, piece.size * 1.8f),
            )
        }
    }
}
