package com.eter.undiamas.core.presentation.theme

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.ui.unit.IntOffset

const val ScreenTransitionMillis = 300

private val floatSpec = tween<Float>(ScreenTransitionMillis, easing = FastOutSlowInEasing)
private val offsetSpec = tween<IntOffset>(ScreenTransitionMillis, easing = FastOutSlowInEasing)

/** Transición de pantalla: entra deslizando desde la derecha con fundido. */
fun screenTransition(): ContentTransform =
    (slideInHorizontally(offsetSpec) { full -> full / 4 } + fadeIn(floatSpec)) togetherWith
        (slideOutHorizontally(offsetSpec) { full -> -full / 4 } + fadeOut(floatSpec))

/** Rotación estilo odómetro para cifras que cambian (reloj, ahorro). */
fun odometerTransition(): ContentTransform =
    (slideInVertically(offsetSpec) { height -> height } + fadeIn(floatSpec)) togetherWith
        (slideOutVertically(offsetSpec) { height -> -height } + fadeOut(floatSpec))

/** Entrada desde abajo, para la tarjeta de resultado del check-in. */
fun resultEnterTransition() =
    slideInVertically(offsetSpec) { height -> height / 3 } + fadeIn(floatSpec)

/** Muelle usado al presionar tarjetas (escala 0.97f). */
fun <T> pressSpring() = spring<T>(dampingRatio = Spring.DampingRatioMediumBouncy)

const val PressedScale = 0.97f
