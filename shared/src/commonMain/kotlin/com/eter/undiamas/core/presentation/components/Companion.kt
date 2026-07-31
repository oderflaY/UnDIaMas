package com.eter.undiamas.core.presentation.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Formas que el compañero va tomando conforme se sostiene la racha. */
enum class CompanionShape {
    SEMILLA, // día 0-6: un punto de luz
    LOTO, // día 7-29: flor geométrica
    POLIEDRO, // día 30+: figura compleja
}

/**
 * Elige la forma según los días de racha. Tras una recaída la figura vuelve a SEMILLA,
 * pero quien la dibuja añade el anillo protector: la experiencia no se pierde, cambia de forma.
 */
fun companionShapeFor(days: Long): CompanionShape = when {
    days >= 30 -> CompanionShape.POLIEDRO
    days >= 7 -> CompanionShape.LOTO
    else -> CompanionShape.SEMILLA
}

/**
 * Compañero abstracto que respira dentro del anillo de racha.
 * [hasHistory] dibuja el anillo exterior protector para quien ya tuvo un récord previo.
 */
@Composable
fun EvolvingCompanion(
    days: Long,
    hasHistory: Boolean,
    color: Color,
    modifier: Modifier = Modifier,
    size: Int = 64,
) {
    val breath by rememberInfiniteTransition(label = "companion").animateFloat(
        initialValue = 0.9f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(tween(3200), RepeatMode.Reverse),
        label = "breath",
    )
    val shape = companionShapeFor(days)

    Canvas(modifier = modifier.size(size.dp)) {
        val center = Offset(this.size.width / 2f, this.size.height / 2f)
        val radius = this.size.minDimension / 2f * breath

        drawCircle(
            brush = Brush.radialGradient(
                listOf(color.copy(alpha = 0.55f), Color.Transparent),
                center = center,
                radius = radius * 1.5f,
            ),
            radius = radius * 1.5f,
            center = center,
        )

        when (shape) {
            CompanionShape.SEMILLA -> drawCircle(color = color, radius = radius * 0.34f, center = center)
            CompanionShape.LOTO -> drawLotus(center, radius * 0.78f, color)
            CompanionShape.POLIEDRO -> drawPolyhedron(center, radius * 0.82f, color)
        }

        // Anillo protector: marca que ya hubo camino recorrido antes de esta racha.
        if (hasHistory) {
            drawCircle(
                color = color.copy(alpha = 0.5f),
                radius = radius * 0.98f,
                center = center,
                style = Stroke(width = radius * 0.07f),
            )
        }
    }
}

private fun DrawScope.drawLotus(center: Offset, radius: Float, color: Color) {
    val petals = 6
    repeat(petals) { index ->
        val angle = (2 * PI * index / petals).toFloat()
        val tip = Offset(center.x + radius * cos(angle), center.y + radius * sin(angle))
        val side = angle + (PI / petals).toFloat()
        val control = Offset(
            center.x + radius * 0.62f * cos(side),
            center.y + radius * 0.62f * sin(side),
        )
        val path = Path().apply {
            moveTo(center.x, center.y)
            quadraticBezierTo(control.x, control.y, tip.x, tip.y)
        }
        drawPath(path, color = color, style = Stroke(width = radius * 0.11f, cap = StrokeCap.Round))
    }
    drawCircle(color = color, radius = radius * 0.2f, center = center)
}

private fun DrawScope.drawPolyhedron(center: Offset, radius: Float, color: Color) {
    val vertices = 6
    val points = List(vertices) { index ->
        val angle = (2 * PI * index / vertices - PI / 2).toFloat()
        Offset(center.x + radius * cos(angle), center.y + radius * sin(angle))
    }
    // Contorno.
    val outline = Path().apply {
        moveTo(points.first().x, points.first().y)
        points.drop(1).forEach { lineTo(it.x, it.y) }
        close()
    }
    drawPath(outline, color = color, style = Stroke(width = radius * 0.09f, cap = StrokeCap.Round))
    // Aristas internas: dan la sensación de volumen.
    points.forEachIndexed { index, point ->
        val opposite = points[(index + 2) % vertices]
        drawLine(
            color = color.copy(alpha = 0.55f),
            start = point,
            end = opposite,
            strokeWidth = radius * 0.05f,
            cap = StrokeCap.Round,
        )
    }
    drawCircle(color = color, radius = radius * 0.16f, center = center)
}
