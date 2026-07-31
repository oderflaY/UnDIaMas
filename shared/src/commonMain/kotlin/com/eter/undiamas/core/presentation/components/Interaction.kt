package com.eter.undiamas.core.presentation.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import com.eter.undiamas.core.presentation.theme.PressedScale
import com.eter.undiamas.core.presentation.theme.odometerTransition
import com.eter.undiamas.core.presentation.theme.pressSpring

/**
 * Clic con retroalimentación háptica y contracción elástica a 0.97f mientras se presiona.
 * [longPress] se usa en acciones de guardado, donde la vibración debe sentirse más marcada.
 */
fun Modifier.pressable(
    onClick: () -> Unit,
    longPress: Boolean = false,
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) PressedScale else 1f,
        animationSpec = pressSpring(),
        label = "press-scale",
    )
    val haptics = LocalHapticFeedback.current

    Modifier
        .scale(scale)
        .clickable(interactionSource = interactionSource, indication = null) {
            haptics.performHapticFeedback(
                if (longPress) HapticFeedbackType.LongPress else HapticFeedbackType.TextHandleMove,
            )
            onClick()
        }
}

/** Temblor sutil para llamar la atención sin alarmar (banner de emergencia en riesgo). */
fun Modifier.shake(active: Boolean): Modifier = composed {
    if (!active) {
        Modifier
    } else {
        val offset by rememberInfiniteTransition(label = "shake").animateFloat(
            initialValue = -4f,
            targetValue = 4f,
            animationSpec = infiniteRepeatable(tween(110), RepeatMode.Reverse),
            label = "shake-offset",
        )
        Modifier.graphicsLayer { translationX = offset }
    }
}

/** Cifra que rota estilo odómetro cada vez que cambia su valor. */
@Composable
fun OdometerText(
    value: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
) {
    AnimatedContent(
        targetState = value,
        transitionSpec = { odometerTransition() },
        modifier = modifier,
        label = "odometer",
    ) { current ->
        Text(current, style = style, color = color)
    }
}
