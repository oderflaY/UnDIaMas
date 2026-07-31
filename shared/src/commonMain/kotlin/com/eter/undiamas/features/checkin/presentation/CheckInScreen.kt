package com.eter.undiamas.features.checkin.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.eter.undiamas.core.domain.model.CheckInEntry
import com.eter.undiamas.core.domain.model.RiskAssessment
import com.eter.undiamas.core.domain.model.RiskLevel
import com.eter.undiamas.core.domain.model.Trigger
import com.eter.undiamas.core.presentation.AppState
import com.eter.undiamas.core.presentation.Navigator
import com.eter.undiamas.core.presentation.Screen
import com.eter.undiamas.core.presentation.brush
import com.eter.undiamas.core.presentation.components.Confetti
import com.eter.undiamas.core.presentation.components.GradientCard
import com.eter.undiamas.core.presentation.components.TrafficLight
import com.eter.undiamas.core.presentation.components.pressable
import com.eter.undiamas.core.presentation.label
import com.eter.undiamas.core.presentation.theme.AccentCheckIn
import com.eter.undiamas.core.presentation.theme.AnswerBlue
import com.eter.undiamas.core.presentation.theme.CheckInProgressBrush
import com.eter.undiamas.core.presentation.theme.resultEnterTransition
import kotlin.math.roundToInt
import kotlin.time.Clock
import com.eter.undiamas.core.presentation.theme.AppIcons
import com.eter.undiamas.core.presentation.icon
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.Icon

private data class Question(val key: String, val text: String, val kind: String)

/**
 * Cuestionario adaptativo: si el último check-in detectó detonante o quedó en amarillo/rojo,
 * se omiten las preguntas introductorias y se va directo al impulso actual y su causa raíz.
 */
private fun nextQuestions(state: AppState): List<Question> {
    val ultimo = state.checkIns.firstOrNull()
    val veniaEnRiesgo = ultimo != null &&
        (ultimo.riskLevel != RiskLevel.VERDE || ultimo.answers["detonante_presente"] == "si")

    return if (veniaEnRiesgo) {
        listOf(Question("impulso_consumo", "¿Sientes impulso de consumir en este momento?", "IMPULSO ACTUAL"))
    } else {
        listOf(
            Question("detonante_presente", "¿Apareció algún detonante desde tu último check-in?", "CONTEXTO"),
            Question("impulso_consumo", "¿Sientes impulso de consumir en este momento?", "IMPULSO ACTUAL"),
        )
    }
}

@Composable
fun CheckInScreen(state: AppState, navigator: Navigator) {
    val questions = remember { nextQuestions(state) }
    var stepIndex by remember { mutableStateOf(0) }
    val answers = remember { mutableMapOf<String, String>() }
    var selectedTriggers by remember { mutableStateOf(emptySet<Trigger>()) }
    var intensity by remember { mutableStateOf(0f) }
    var showTriggers by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<RiskAssessment?>(null) }

    val assessment = result

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            if (assessment == null) {
                Text("Check-in de hoy", style = MaterialTheme.typography.headlineMedium)

                SegmentedProgress(total = questions.size, current = stepIndex)

                val question = questions[stepIndex]
                Text(
                    "PREGUNTA ${stepIndex + 1} DE ${questions.size} · ${question.kind}",
                    style = MaterialTheme.typography.labelMedium,
                    color = AccentCheckIn,
                )
                Text(question.text, style = MaterialTheme.typography.headlineSmall)

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    AnswerTile(AppIcons.Alerta, "Sí", MaterialTheme.colorScheme.error, Modifier.weight(1f)) {
                        answers[question.key] = "si"
                        showTriggers = true
                        if (stepIndex < questions.lastIndex) stepIndex += 1
                    }
                    AnswerTile(AppIcons.CheckIn, "No", AnswerBlue, Modifier.weight(1f)) {
                        answers[question.key] = "no"
                        if (stepIndex < questions.lastIndex) {
                            stepIndex += 1
                        } else {
                            result = finish(state, answers, selectedTriggers, intensity)
                        }
                    }
                }

                // Al reconocer un detonante o un impulso, se pide contexto antes de cerrar.
                AnimatedVisibility(visible = showTriggers) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("¿Qué lo detonó?", style = MaterialTheme.typography.titleMedium)
                        TriggerCloud(selectedTriggers) { trigger ->
                            selectedTriggers = if (trigger in selectedTriggers) {
                                selectedTriggers - trigger
                            } else {
                                selectedTriggers + trigger
                            }
                        }

                        Text("Intensidad del impulso", style = MaterialTheme.typography.titleMedium)
                        IntensitySlider(intensity) { intensity = it }

                        Button(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { result = finish(state, answers, selectedTriggers, intensity) },
                        ) { Text("Terminar check-in") }
                    }
                }
            } else {
                AnimatedVisibility(visible = true, enter = resultEnterTransition()) {
                    GradientCard(brush = assessment.riskLevel.brush) {
                        Text("RESULTADO DE TU CHECK-IN", style = MaterialTheme.typography.labelMedium)
                        Text(
                            assessment.riskLevel.label,
                            style = MaterialTheme.typography.headlineSmall,
                        )
                        Text(assessment.recommendation, style = MaterialTheme.typography.bodyLarge)
                        if (selectedTriggers.isNotEmpty()) {
                            Text(
                                "Detonantes: ${selectedTriggers.joinToString { it.label }}",
                                style = MaterialTheme.typography.labelMedium,
                            )
                        }
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            TrafficLight(
                                level = assessment.riskLevel,
                                modifier = Modifier.height(28.dp).fillMaxWidth(0.45f),
                            )
                        }
                    }
                }

                if (assessment.riskLevel == RiskLevel.ROJO) {
                    Button(
                        onClick = { navigator.goTo(Screen.Emergencia) },
                        modifier = Modifier.fillMaxWidth().height(64.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    ) {
                        Icon(AppIcons.Emergencia, contentDescription = null, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(10.dp))
                        Text("Ir al protocolo de emergencia", style = MaterialTheme.typography.titleMedium)
                    }
                } else {
                    Button(onClick = { navigator.goTo(Screen.Inicio) }, modifier = Modifier.fillMaxWidth()) {
                        Text("Volver al inicio")
                    }
                }
            }
        }

        // El confeti celebra solo cuando el día cerró fuera de crisis.
        if (assessment != null && assessment.riskLevel != RiskLevel.ROJO) {
            Confetti(modifier = Modifier.fillMaxSize())
        }
    }
}

private fun finish(
    state: AppState,
    answers: Map<String, String>,
    triggers: Set<Trigger>,
    intensity: Float,
): RiskAssessment {
    val assessed = state.riskAssessor.assess(answers)
    state.registerCheckIn(
        CheckInEntry(
            id = "",
            userId = state.profile.userId,
            answeredAt = Clock.System.now(),
            answers = answers.toMap(),
            riskLevel = assessed.riskLevel,
            triggers = triggers,
            urgeIntensity = intensity.roundToInt(),
        ),
    )
    state.notify("Check-in guardado")
    return assessed
}

@Composable
private fun SegmentedProgress(total: Int, current: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
        repeat(total) { index ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(6.dp)
                    .background(
                        if (index <= current) CheckInProgressBrush else accentTrack(),
                        RoundedCornerShape(3.dp),
                    ),
            )
        }
    }
}

@Composable
private fun accentTrack() = androidx.compose.ui.graphics.SolidColor(AccentCheckIn.copy(alpha = 0.18f))

@Composable
private fun AnswerTile(
    icon: ImageVector,
    label: String,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = accent.copy(alpha = 0.15f),
        contentColor = accent,
        modifier = modifier.height(104.dp).pressable(onClick),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(30.dp))
            Text(label, style = MaterialTheme.typography.headlineSmall)
        }
    }
}

@Composable
private fun TriggerCloud(selected: Set<Trigger>, onToggle: (Trigger) -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Trigger.entries.forEach { trigger ->
            val isSelected = trigger in selected
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                color = if (isSelected) AccentCheckIn.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.pressable({ onToggle(trigger) }),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(trigger.icon, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(trigger.label, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
private fun IntensitySlider(value: Float, onChange: (Float) -> Unit) {
    val haptics = LocalHapticFeedback.current
    var lastStep by remember { mutableStateOf(value.roundToInt()) }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Slider(
            value = value,
            onValueChange = { new ->
                // Una vibración por cada escalón: la intensidad también se siente en la mano.
                val step = new.roundToInt()
                if (step != lastStep) {
                    lastStep = step
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
                onChange(new)
            },
            valueRange = 0f..10f,
            steps = 9,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.error,
                activeTrackColor = MaterialTheme.colorScheme.error,
            ),
        )
        Text(
            "Nivel ${value.roundToInt()} de 10",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
