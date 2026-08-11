package com.eter.undiamas.features.splash.presentation

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eter.undiamas.core.presentation.theme.AppIcons
import com.eter.undiamas.core.presentation.theme.SplashBrush
import kotlinx.coroutines.delay

/** Cuántos puntos tiene el indicador inferior. */
private const val PUNTOS = 3

/**
 * Pantalla de bienvenida.
 *
 * Dura lo que tarda la app en recuperar la sesión y abrir la base local, con un mínimo de
 * [duracionMinimaMillis] para que no aparezca y desaparezca de golpe. No es una espera
 * artificial: si la carga tarda más, se queda hasta que termine.
 *
 * Los tres puntos de abajo no son una barra de progreso real —no habría nada que medir— y
 * por eso ninguno avanza: solo el primero está sólido, los otros al 30%.
 */
@Composable
fun SplashScreen(
    duracionMinimaMillis: Long = 1_200,
    onTerminar: () -> Unit,
) {
    var visible by remember { mutableStateOf(false) }

    // El logo entra creciendo un poco y apareciendo. Es el único movimiento de la pantalla:
    // más animación aquí solo retrasaría a quien abre la app con prisa.
    val escala by animateFloatAsState(
        targetValue = if (visible) 1f else 0.82f,
        animationSpec = tween(durationMillis = 520),
        label = "escala",
    )
    val opacidad by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 420),
        label = "opacidad",
    )

    LaunchedEffect(Unit) {
        visible = true
        delay(duracionMinimaMillis)
        onTerminar()
    }

    Box(
        modifier = Modifier.fillMaxSize().background(SplashBrush),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.alpha(opacidad).scale(escala),
        ) {
            // Squircle translúcido: el mismo blanco del degradado, apenas separado del fondo.
            Box(
                modifier = Modifier
                    .size(104.dp)
                    .background(
                        color = Color.White.copy(alpha = 0.22f),
                        shape = RoundedCornerShape(32.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = AppIcons.Sol,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(52.dp),
                )
            }

            Text(
                text = "Un Día Más",
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 24.dp),
            )
            Text(
                text = "Tu acompañante de cada día",
                color = Color.White.copy(alpha = 0.88f),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp),
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 56.dp)
                .alpha(opacidad),
        ) {
            repeat(PUNTOS) { indice ->
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(
                            color = Color.White.copy(alpha = if (indice == 0) 1f else 0.3f),
                            shape = CircleShape,
                        ),
                )
            }
        }
    }
}
