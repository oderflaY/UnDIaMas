package com.eter.undiamas.core.presentation.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

private data class Bubble(val x: Float, val phase: Float, val speed: Float, val radius: Float, val color: Color)

/**
 * Mini-juego de distracción táctil: burbujas de neón que suben lentamente y se revientan
 * al tocarlas, con una vibración corta. Sirve para desviar el foco durante un impulso.
 */
@Composable
fun BubblePopGame(
    colors: List<Color>,
    modifier: Modifier = Modifier,
    onPop: (Int) -> Unit = {},
) {
    val bubbles = remember {
        val random = Random(7)
        mutableStateListOf<Bubble>().apply {
            repeat(14) {
                add(
                    Bubble(
                        x = 0.08f + random.nextFloat() * 0.84f,
                        phase = random.nextFloat(),
                        speed = 0.6f + random.nextFloat() * 0.8f,
                        radius = 18f + random.nextFloat() * 22f,
                        color = colors[random.nextInt(colors.size)],
                    ),
                )
            }
        }
    }
    val popped = remember { mutableStateListOf<Bubble>() }
    val haptics = LocalHapticFeedback.current

    val t by rememberInfiniteTransition(label = "bubbles").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(9_000, easing = LinearEasing)),
        label = "rise",
    )

    fun centerOf(bubble: Bubble, width: Float, height: Float): Offset {
        val progress = ((t * bubble.speed + bubble.phase) % 1f)
        val y = height - progress * height
        val x = bubble.x * width + sin(progress * 8f) * 14f
        return Offset(x, y)
    }

    Canvas(
        modifier = modifier.pointerInput(Unit) {
            detectTapGestures { tap ->
                val hit = bubbles.firstOrNull { bubble ->
                    val center = centerOf(bubble, size.width.toFloat(), size.height.toFloat())
                    hypot(tap.x - center.x, tap.y - center.y) <= bubble.radius * 1.4f
                }
                if (hit != null) {
                    bubbles.remove(hit)
                    popped.add(hit)
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onPop(popped.size)
                }
            }
        },
    ) {
        bubbles.forEach { bubble ->
            val center = centerOf(bubble, size.width, size.height)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(bubble.color.copy(alpha = 0.65f), bubble.color.copy(alpha = 0.05f)),
                    center = center,
                    radius = bubble.radius * 1.6f,
                ),
                radius = bubble.radius * 1.6f,
                center = center,
            )
            drawCircle(color = bubble.color.copy(alpha = 0.5f), radius = bubble.radius, center = center)
        }
    }
}
