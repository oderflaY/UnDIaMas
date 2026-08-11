package com.eter.undiamas.features.onboarding.presentation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.eter.undiamas.core.presentation.theme.AccentOrange
import com.eter.undiamas.core.presentation.theme.BrandPurple
import com.eter.undiamas.core.presentation.theme.InkMuted
import com.eter.undiamas.core.presentation.theme.InkStrong

private const val TOTAL_SLIDES = 2

/**
 * Las dos diapositivas de bienvenida.
 *
 * Solo explican de qué va la app; no piden ni un dato. El cuestionario que sí pregunta
 * cosas viene después, y separarlos importa: la primera pantalla que ve alguien que está
 * intentando dejar algo no debería ser un formulario.
 */
@Composable
fun IntroSlides(onTerminar: () -> Unit) {
    var slide by remember { mutableStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 24.dp),
    ) {
        // "Omitir" arriba a la derecha: quien ya sabe de qué va la app no tiene por qué leerlo.
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onTerminar) {
                Text(
                    "Omitir",
                    style = MaterialTheme.typography.labelLarge,
                    color = InkMuted,
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        AnimatedContent(
            targetState = slide,
            transitionSpec = {
                (fadeIn(tween(280)) togetherWith fadeOut(tween(200)))
            },
            modifier = Modifier.weight(1f),
            label = "slide",
        ) { actual ->
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                // Círculo blanco con sombra suave: la ilustración vive dentro, nunca suelta
                // sobre el fondo, para que las dos diapositivas tengan el mismo peso visual.
                Surface(
                    shape = CircleShape,
                    color = Color.White,
                    shadowElevation = 12.dp,
                    modifier = Modifier.size(220.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Canvas(modifier = Modifier.size(132.dp)) {
                            if (actual == 0) dibujarAmanecer() else dibujarProgreso()
                        }
                    }
                }

                Spacer(Modifier.height(48.dp))

                Text(
                    text = if (actual == 0) "Un día a la vez" else "Registra tu avance",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = InkStrong,
                    textAlign = TextAlign.Center,
                )

                Spacer(Modifier.height(16.dp))

                Text(
                    text = if (actual == 0) {
                        "Cada jornada que superas es un logro. Aquí celebramos cada día, " +
                            "sin importar el pasado."
                    } else {
                        "Lleva un seguimiento de tu racha, observa tu progreso y descubre " +
                            "cuánto has crecido."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = InkMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(0.8f),
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.Center,
        ) {
            repeat(TOTAL_SLIDES) { indice ->
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .size(width = if (indice == slide) 22.dp else 8.dp, height = 8.dp)
                        .background(
                            color = if (indice == slide) BrandPurple else InkMuted.copy(alpha = 0.4f),
                            shape = CircleShape,
                        ),
                )
            }
        }

        Button(
            onClick = { if (slide < TOTAL_SLIDES - 1) slide++ else onTerminar() },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .padding(bottom = 0.dp),
        ) {
            Text("Continuar", style = MaterialTheme.typography.titleSmall)
        }

        Spacer(Modifier.height(32.dp))
    }
}

/**
 * Sol naciente sobre el mar.
 *
 * Se dibuja en Canvas en vez de traer un PNG: son cuatro formas planas, pesan cero y se
 * ven igual de nítidas en cualquier densidad de pantalla.
 */
private fun DrawScope.dibujarAmanecer() {
    val ancho = size.width
    val alto = size.height
    val horizonte = alto * 0.62f

    // Halo del sol, apenas perceptible, para que el círculo no quede pegado al blanco.
    drawCircle(
        brush = Brush.radialGradient(
            listOf(AccentOrange.copy(alpha = 0.28f), Color.Transparent),
        ),
        radius = ancho * 0.42f,
        center = Offset(ancho / 2f, horizonte),
    )

    drawCircle(
        brush = Brush.verticalGradient(
            listOf(Color(0xFFFFD166), AccentOrange),
            startY = horizonte - ancho * 0.26f,
            endY = horizonte,
        ),
        radius = ancho * 0.24f,
        center = Offset(ancho / 2f, horizonte),
    )

    // Mar: dos bandas turquesa que se van aclarando hacia el fondo.
    drawRect(
        brush = Brush.verticalGradient(
            listOf(Color(0xFF48DBC5), Color(0xFF17A2B8)),
            startY = horizonte,
            endY = alto,
        ),
        topLeft = Offset(0f, horizonte),
        size = Size(ancho, alto - horizonte),
    )

    // Reflejo del sol en el agua.
    listOf(0.16f, 0.30f, 0.44f).forEachIndexed { indice, factor ->
        val y = horizonte + (alto - horizonte) * factor
        val medio = ancho * (0.20f - indice * 0.045f)
        drawLine(
            color = Color.White.copy(alpha = 0.55f - indice * 0.14f),
            start = Offset(ancho / 2f - medio, y),
            end = Offset(ancho / 2f + medio, y),
            strokeWidth = alto * 0.035f,
            cap = StrokeCap.Round,
        )
    }
}

/**
 * Gráfico de línea ascendente sobre una cuadrícula muy tenue.
 *
 * Sube de izquierda a derecha con un dientecito de bajada: una recta perfecta sería una
 * promesa que la recuperación no hace, y esta pantalla habla justo de progreso real.
 */
private fun DrawScope.dibujarProgreso() {
    val ancho = size.width
    val alto = size.height

    repeat(4) { fila ->
        val y = alto * (fila + 1) / 5f
        drawLine(
            color = InkMuted.copy(alpha = 0.22f),
            start = Offset(0f, y),
            end = Offset(ancho, y),
            strokeWidth = 1.dp.toPx(),
        )
    }
    repeat(4) { columna ->
        val x = ancho * (columna + 1) / 5f
        drawLine(
            color = InkMuted.copy(alpha = 0.22f),
            start = Offset(x, 0f),
            end = Offset(x, alto),
            strokeWidth = 1.dp.toPx(),
        )
    }

    val puntos = listOf(
        Offset(ancho * 0.06f, alto * 0.82f),
        Offset(ancho * 0.28f, alto * 0.58f),
        Offset(ancho * 0.46f, alto * 0.68f),
        Offset(ancho * 0.68f, alto * 0.34f),
        Offset(ancho * 0.94f, alto * 0.12f),
    )

    val linea = Path().apply {
        moveTo(puntos.first().x, puntos.first().y)
        puntos.drop(1).forEach { lineTo(it.x, it.y) }
    }
    drawPath(
        path = linea,
        color = Color(0xFFFF4757),
        style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round),
    )

    puntos.forEach { punto ->
        drawCircle(color = Color.White, radius = 5.dp.toPx(), center = punto)
        drawCircle(
            color = Color(0xFFFF4757),
            radius = 5.dp.toPx(),
            center = punto,
            style = Stroke(width = 2.5f.dp.toPx()),
        )
    }
}
