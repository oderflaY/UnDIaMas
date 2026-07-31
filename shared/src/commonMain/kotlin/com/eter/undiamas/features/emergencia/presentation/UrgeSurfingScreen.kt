package com.eter.undiamas.features.emergencia.presentation

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
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.eter.undiamas.core.presentation.AppState
import com.eter.undiamas.core.presentation.Navigator
import com.eter.undiamas.core.presentation.Screen
import com.eter.undiamas.core.presentation.components.BreathingCircle
import com.eter.undiamas.core.presentation.components.BubblePopGame
import com.eter.undiamas.core.presentation.components.SectionCard
import com.eter.undiamas.core.presentation.components.SectionHeader
import com.eter.undiamas.core.presentation.components.pressable
import com.eter.undiamas.core.presentation.theme.AccentAsistente
import com.eter.undiamas.core.presentation.theme.AppIcons
import com.eter.undiamas.core.presentation.theme.EmergencyCoralEnd
import com.eter.undiamas.core.presentation.theme.EmergencyCoralStart
import com.eter.undiamas.core.presentation.theme.PrimaryVioletStart
import com.eter.undiamas.core.presentation.theme.RiskGreen
import com.eter.undiamas.features.emergencia.domain.UrgeStage
import com.eter.undiamas.features.emergencia.domain.UrgeSurfingSession
import kotlinx.coroutines.delay

/**
 * Búnker de 15 minutos para sostener un impulso agudo, dividido en tres etapas de cinco
 * minutos: respiración, reencuadre cognitivo y distracción táctil.
 */
@Composable
fun UrgeSurfingScreen(state: AppState, navigator: Navigator) {
    val session = remember { UrgeSurfingSession() }
    var elapsed by remember { mutableStateOf(0) }
    var running by remember { mutableStateOf(true) }
    var promptIndex by remember { mutableStateOf(0) }
    val haptics = LocalHapticFeedback.current

    LaunchedEffect(running) {
        while (running) {
            delay(1_000)
            elapsed += 1
        }
    }

    val stage = session.stageAt(elapsed)
    val complete = session.isComplete(elapsed)

    // Una vibración marcada al cambiar de etapa, para que se note sin mirar la pantalla.
    LaunchedEffect(stage) {
        if (elapsed > 0) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
    }
    LaunchedEffect(complete) {
        if (complete) {
            running = false
            state.notify("Lo sostuviste. El pico pasó y sigues aquí.")
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(EmergencyCoralEnd.copy(alpha = 0.32f), MaterialTheme.colorScheme.background),
                ),
            )
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            if (complete) "Lo lograste" else "Sosteniendo el impulso",
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(
            formatCountdown(session.remainingSeconds(elapsed)),
            style = MaterialTheme.typography.displaySmall,
            fontFamily = FontFamily.Monospace,
        )
        LinearProgressIndicator(
            progress = { session.progress(elapsed) },
            modifier = Modifier.fillMaxWidth().height(10.dp),
        )

        StageStrip(current = stage, complete = complete)

        if (complete) {
            SectionCard {
                SectionHeader(AppIcons.Escudo, "El pico pasó", RiskGreen)
                Text(
                    "Acabas de atravesar los quince minutos más difíciles sin consumir. " +
                        "Eso no fue suerte: fue tu decisión, sostenida minuto a minuto.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Button(
                onClick = { navigator.goTo(Screen.CheckIn) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = RiskGreen),
            ) { Text("Registrar cómo me siento ahora") }
            OutlinedButton(
                onClick = { navigator.goTo(Screen.Inicio) },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Volver al inicio") }
        } else {
            when (stage) {
                UrgeStage.RESPIRACION -> {
                    StageIntro(stage)
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        BreathingCircle(colors = listOf(EmergencyCoralStart, PrimaryVioletStart))
                    }
                }

                UrgeStage.REENCUADRE -> {
                    StageIntro(stage)
                    val prompt = session.reframingPrompts[promptIndex % session.reframingPrompts.size]
                    SectionCard {
                        SectionHeader(AppIcons.Mente, prompt.distortion, PrimaryVioletStart)
                        Text(prompt.question, style = MaterialTheme.typography.titleMedium)
                        Text(
                            "No tienes que responder en voz alta ni escribirlo. Solo dedícale un minuto.",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        OutlinedButton(
                            onClick = { promptIndex += 1 },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text("Otra pregunta") }
                    }
                }

                UrgeStage.DISTRACCION -> {
                    StageIntro(stage)
                    SectionCard {
                        SectionHeader(AppIcons.Distraccion, "Ocupa las manos", EmergencyCoralStart)
                        BubblePopGame(
                            colors = listOf(EmergencyCoralStart, PrimaryVioletStart, AccentAsistente, RiskGreen),
                            modifier = Modifier.fillMaxWidth().height(300.dp),
                        )
                    }
                }
            }

            OutlinedButton(onClick = { navigator.back() }, modifier = Modifier.fillMaxWidth()) {
                Text("Salir del búnker")
            }
        }
    }
}

@Composable
private fun StageIntro(stage: UrgeStage) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(stage.title, style = MaterialTheme.typography.titleLarge)
        Text(
            stage.subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun StageStrip(current: UrgeStage, complete: Boolean) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        UrgeStage.entries.forEach { stage ->
            val done = complete || stage.ordinal < current.ordinal
            val active = !complete && stage == current
            val color = when {
                active -> EmergencyCoralStart
                done -> RiskGreen
                else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
            }
            Surface(
                shape = RoundedCornerShape(50),
                color = color.copy(alpha = if (active || done) 0.25f else 0.12f),
                modifier = Modifier.weight(1f),
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        iconFor(stage),
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(20.dp),
                    )
                    Text(stage.title, style = MaterialTheme.typography.labelMedium, color = color)
                }
            }
        }
    }
}

private fun iconFor(stage: UrgeStage) = when (stage) {
    UrgeStage.RESPIRACION -> AppIcons.Respiracion
    UrgeStage.REENCUADRE -> AppIcons.Mente
    UrgeStage.DISTRACCION -> AppIcons.Distraccion
}

private fun formatCountdown(seconds: Int): String {
    val minutes = seconds / 60
    val rest = seconds % 60
    fun pad(v: Int) = if (v < 10) "0$v" else "$v"
    return "${pad(minutes)}:${pad(rest)}"
}
