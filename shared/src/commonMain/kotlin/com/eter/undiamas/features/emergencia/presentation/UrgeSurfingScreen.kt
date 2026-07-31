package com.eter.undiamas.features.emergencia.presentation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.eter.undiamas.core.presentation.AppState
import com.eter.undiamas.core.presentation.Navigator
import com.eter.undiamas.core.presentation.Screen
import com.eter.undiamas.core.presentation.components.BilateralFocus
import com.eter.undiamas.core.presentation.components.BreathingCircle
import com.eter.undiamas.core.presentation.components.BubblePopGame
import com.eter.undiamas.core.presentation.components.CountdownRing
import com.eter.undiamas.core.presentation.components.pressable
import com.eter.undiamas.core.presentation.theme.AccentAsistente
import com.eter.undiamas.core.presentation.theme.AppIcons
import com.eter.undiamas.core.presentation.theme.EmergencyCoralStart
import com.eter.undiamas.core.presentation.theme.PrimaryVioletStart
import com.eter.undiamas.core.presentation.theme.RiskGreen
import com.eter.undiamas.features.emergencia.domain.UrgeStage
import com.eter.undiamas.features.emergencia.domain.UrgeSurfingSession
import kotlinx.coroutines.delay

/** Casi negro: la pantalla debe desaparecer para que solo quede el reloj. */
private val BunkerBackground = Color(0xFF07070C)
private val BilateralCyan = Color(0xFF22D3EE)

/** Frases que se relevan lentamente bajo el reloj. */
private val holdingPhrases = listOf(
    "La ola está en su punto más alto. Solo respira.",
    "No tienes que hacer nada más que quedarte aquí.",
    "Esto que sientes es un pico, y los picos bajan.",
    "El impulso está cediendo. Aguanta un poco más.",
    "Cada minuto que pasa juega a tu favor.",
    "Tu cuerpo ya empezó a calmarse, aunque aún no lo notes.",
    "Ya has sostenido cosas más difíciles que esta.",
)

private enum class Herramienta(val label: String, val icon: ImageVector) {
    RESPIRAR("Respirar", AppIcons.Respiracion),
    REENCUADRE("Reencuadre", AppIcons.Mente),
    LUZ("Seguir la luz", AppIcons.Ver),
    BURBUJAS("Burbujas", AppIcons.Distraccion),
}

/**
 * Búnker de 15 minutos para sostener un impulso agudo. La interfaz se reduce al mínimo
 * (fondo casi negro, sin barra de navegación) para no competir por la atención.
 */
@Composable
fun UrgeSurfingScreen(state: AppState, navigator: Navigator) {
    val session = remember { UrgeSurfingSession() }
    var elapsed by remember { mutableStateOf(0) }
    var running by remember { mutableStateOf(true) }
    var promptIndex by remember { mutableStateOf(0) }
    var herramienta by remember { mutableStateOf(Herramienta.RESPIRAR) }
    val haptics = LocalHapticFeedback.current

    LaunchedEffect(running) {
        while (running) {
            delay(1_000)
            elapsed += 1
        }
    }

    val stage = session.stageAt(elapsed)
    val complete = session.isComplete(elapsed)
    val phrase = holdingPhrases[(elapsed / 25) % holdingPhrases.size]

    // La herramienta sugerida sigue a la etapa, pero la persona puede cambiarla cuando quiera.
    LaunchedEffect(stage) {
        herramienta = when (stage) {
            UrgeStage.RESPIRACION -> Herramienta.RESPIRAR
            UrgeStage.REENCUADRE -> Herramienta.REENCUADRE
            UrgeStage.DISTRACCION -> Herramienta.BURBUJAS
        }
        if (elapsed > 0) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
    }
    LaunchedEffect(complete) {
        if (complete) {
            running = false
            state.registerUrgeOvercome()
            state.notify("Lo sostuviste. El pico pasó y sigues aquí.")
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BunkerBackground)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (complete) {
            CompletionBlock(navigator)
            return@Column
        }

        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            CountdownRing(
                remainingFraction = 1f - session.progress(elapsed),
                countdown = formatCountdown(session.remainingSeconds(elapsed)),
                caption = stage.title.uppercase(),
                ringColors = listOf(EmergencyCoralStart, PrimaryVioletStart, BilateralCyan, EmergencyCoralStart),
            )
        }

        AnimatedContent(
            targetState = phrase,
            transitionSpec = { fadeIn(tween(900)) togetherWith fadeOut(tween(900)) },
            label = "phrase",
        ) { current ->
            Text(
                current,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White.copy(alpha = 0.78f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Herramienta.entries.forEach { option ->
                ToolTab(option, option == herramienta, Modifier.weight(1f)) { herramienta = option }
            }
        }

        when (herramienta) {
            Herramienta.RESPIRAR -> Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                BreathingCircle(colors = listOf(EmergencyCoralStart, PrimaryVioletStart))
            }

            Herramienta.REENCUADRE -> {
                val prompt = session.reframingPrompts[promptIndex % session.reframingPrompts.size]
                DarkCard {
                    Text(
                        prompt.distortion.uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        color = PrimaryVioletStart,
                    )
                    Text(prompt.question, style = MaterialTheme.typography.titleMedium, color = Color.White)
                    Text(
                        "No hace falta que lo respondas en voz alta. Solo dedícale un minuto.",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.6f),
                    )
                    OutlinedButton(
                        onClick = { promptIndex += 1 },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Otra pregunta") }
                }
            }

            Herramienta.LUZ -> {
                Text(
                    "Sigue la luz solo con tus ojos. No muevas la cabeza.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.55f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                BilateralFocus(
                    dotColor = BilateralCyan,
                    modifier = Modifier.fillMaxWidth().height(220.dp),
                )
                Text(
                    "Se ve mejor con el teléfono en horizontal.",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.4f),
                )
            }

            Herramienta.BURBUJAS -> DarkCard {
                Text(
                    "Ocupa las manos. Revienta todas las que puedas.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.7f),
                )
                BubblePopGame(
                    colors = listOf(EmergencyCoralStart, PrimaryVioletStart, AccentAsistente, RiskGreen),
                    modifier = Modifier.fillMaxWidth().height(300.dp),
                )
            }
        }

        OutlinedButton(onClick = { navigator.back() }, modifier = Modifier.fillMaxWidth()) {
            Text("Salir del búnker", color = Color.White.copy(alpha = 0.7f))
        }
    }
}

@Composable
private fun CompletionBlock(navigator: Navigator) {
    Icon(
        AppIcons.Escudo,
        contentDescription = null,
        tint = RiskGreen,
        modifier = Modifier.size(72.dp),
    )
    Text("Lo lograste", style = MaterialTheme.typography.displaySmall, color = Color.White)
    Text(
        "Acabas de atravesar los quince minutos más difíciles sin consumir. " +
            "Eso no fue suerte: fue tu decisión, sostenida minuto a minuto.",
        style = MaterialTheme.typography.bodyLarge,
        color = Color.White.copy(alpha = 0.78f),
        textAlign = TextAlign.Center,
    )
    Button(
        onClick = { navigator.goTo(Screen.CheckIn) },
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = RiskGreen),
    ) { Text("Registrar cómo me siento ahora") }
    OutlinedButton(
        onClick = { navigator.goTo(Screen.Inicio) },
        modifier = Modifier.fillMaxWidth(),
    ) { Text("Volver al inicio", color = Color.White.copy(alpha = 0.7f)) }
}

@Composable
private fun DarkCard(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = Color.White.copy(alpha = 0.06f),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content,
        )
    }
}

@Composable
private fun ToolTab(
    herramienta: Herramienta,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val tint = if (selected) Color.White else Color.White.copy(alpha = 0.38f)
    Surface(
        shape = RoundedCornerShape(50),
        color = Color.White.copy(alpha = if (selected) 0.16f else 0.05f),
        modifier = modifier.pressable(onClick),
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(herramienta.icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
            Text(herramienta.label, style = MaterialTheme.typography.labelMedium, color = tint)
        }
    }
}

private fun formatCountdown(seconds: Int): String {
    val minutes = seconds / 60
    val rest = seconds % 60
    fun pad(v: Int) = if (v < 10) "0$v" else "$v"
    return "${pad(minutes)}:${pad(rest)}"
}
