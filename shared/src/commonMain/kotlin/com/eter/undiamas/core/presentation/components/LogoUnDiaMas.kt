package com.eter.undiamas.core.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Los colores de la marca, tal cual salen del logo. */
object LogoColors {
    val Noche = Color(0xFF0F172A)
    val Sol = Color(0xFFF59E0B)
    val Luz = Color(0xFF60A5FA)
    val Horizonte = Color(0xFF3B82F6)
}

/**
 * El logo de la app: amanecer, arco de luz, horizonte y el signo de más.
 *
 * Está dibujado a mano en vez de cargado como recurso porque esto es código común: un
 * drawable de Android no existe en iOS, y tener el mismo logo dos veces es tenerlo mal una
 * de las dos. Se dibuja sobre el mismo lienzo de 108 que el icono del lanzador, así que las
 * proporciones son idénticas y no hay dos versiones que se puedan separar con el tiempo.
 *
 * [monocromo] lo pinta todo del color que se le pase, para cuando va encima de un fondo de
 * color —el degradado del splash— donde la paleta completa se pelearía con el fondo.
 */
@Composable
fun LogoUnDiaMas(
    size: Dp,
    modifier: Modifier = Modifier,
    monocromo: Color? = null,
) {
    Canvas(modifier = modifier.size(size)) {
        dibujarLogo(monocromo)
    }
}

/** Un cuarto del lienzo de 108, que es la unidad en la que están definidas las formas. */
private fun DrawScope.dibujarLogo(monocromo: Color?) {
    val u = this.size.minDimension / 108f
    fun x(v: Float) = v * u
    fun color(propio: Color) = monocromo ?: propio

    // Sol
    drawCircle(
        color = color(LogoColors.Sol),
        radius = x(16f),
        center = Offset(x(54f), x(50f)),
    )

    // Arco de luz: media circunferencia superior de radio 22 centrada en (54, 48).
    drawArc(
        color = color(LogoColors.Luz),
        startAngle = 180f,
        sweepAngle = 180f,
        useCenter = false,
        topLeft = Offset(x(32f), x(26f)),
        size = Size(x(44f), x(44f)),
        style = Stroke(width = x(3.5f), cap = StrokeCap.Round),
    )

    // Ola / horizonte
    val ola = Path().apply {
        moveTo(x(30f), x(66f))
        quadraticTo(x(42f), x(61f), x(54f), x(66f))
        // El reflejo del punto de control anterior, que es lo que hace la `T` del SVG.
        quadraticTo(x(66f), x(71f), x(78f), x(66f))
        lineTo(x(78f), x(78f))
        lineTo(x(30f), x(78f))
        close()
    }
    drawPath(ola, color(LogoColors.Horizonte))

    // Signo de más
    val blanco = monocromo ?: Color.White
    drawLine(
        color = blanco,
        start = Offset(x(68f), x(34f)),
        end = Offset(x(76f), x(34f)),
        strokeWidth = x(3f),
        cap = StrokeCap.Round,
    )
    drawLine(
        color = blanco,
        start = Offset(x(72f), x(30f)),
        end = Offset(x(72f), x(38f)),
        strokeWidth = x(3f),
        cap = StrokeCap.Round,
    )
}
