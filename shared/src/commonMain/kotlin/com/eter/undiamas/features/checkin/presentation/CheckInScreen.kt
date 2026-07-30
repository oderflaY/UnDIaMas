package com.eter.undiamas.features.checkin.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import com.eter.undiamas.core.domain.model.CheckInEntry
import com.eter.undiamas.core.domain.model.RiskAssessment
import com.eter.undiamas.core.domain.model.RiskLevel
import com.eter.undiamas.core.presentation.AppState
import com.eter.undiamas.core.presentation.Navigator
import com.eter.undiamas.core.presentation.Screen
import com.eter.undiamas.core.presentation.brush
import com.eter.undiamas.core.presentation.components.GradientCard
import com.eter.undiamas.core.presentation.components.TrafficLight
import com.eter.undiamas.core.presentation.emoji
import com.eter.undiamas.core.presentation.label
import com.eter.undiamas.core.presentation.theme.AccentCheckIn
import kotlinx.datetime.Clock

private data class Question(val key: String, val text: String)

// El cuestionario adaptativo: si ya hubo un detonante en un check-in previo,
// se pregunta directo por el impulso de consumo en vez de repetir lo básico.
private fun nextQuestions(state: AppState): List<Question> {
    val huboDetonantePrevio = state.checkIns.firstOrNull()?.answers?.get("detonante_presente") == "si"
    return if (huboDetonantePrevio) {
        listOf(
            Question("impulso_consumo", "¿Sientes impulso de consumir en este momento?"),
        )
    } else {
        listOf(
            Question("detonante_presente", "¿Apareció algún detonante desde tu último check-in?"),
            Question("impulso_consumo", "¿Sientes impulso de consumir en este momento?"),
        )
    }
}

@Composable
fun CheckInScreen(state: AppState, navigator: Navigator) {
    val questions = remember { nextQuestions(state) }
    var stepIndex by remember { mutableStateOf(0) }
    val answers = remember { mutableMapOf<String, String>() }
    var result by remember { mutableStateOf<RiskAssessment?>(null) }

    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        val assessment = result
        if (assessment == null) {
            Text("Check-in de hoy", style = MaterialTheme.typography.headlineMedium)

            // Barra de progreso por segmentos, uno por pregunta.
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                questions.indices.forEach { index ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp)
                            .background(
                                if (index <= stepIndex) AccentCheckIn else AccentCheckIn.copy(alpha = 0.18f),
                                RoundedCornerShape(3.dp),
                            ),
                    )
                }
            }
            Text(
                "PREGUNTA ${stepIndex + 1} DE ${questions.size}",
                style = MaterialTheme.typography.labelMedium,
                color = AccentCheckIn,
            )

            val question = questions[stepIndex]
            Text(question.text, style = MaterialTheme.typography.headlineSmall)

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                listOf("si" to "Sí", "no" to "No").forEach { (value, label) ->
                    AnswerTile(
                        label = label,
                        accent = if (value == "si") MaterialTheme.colorScheme.error else AccentCheckIn,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            answers[question.key] = value
                            if (stepIndex < questions.lastIndex) {
                                stepIndex += 1
                            } else {
                                val assessed = state.riskAssessor.assess(answers)
                                result = assessed
                                state.registerCheckIn(
                                    CheckInEntry(
                                        id = "",
                                        userId = state.profile.userId,
                                        answeredAt = Clock.System.now(),
                                        answers = answers.toMap(),
                                        riskLevel = assessed.riskLevel,
                                    ),
                                )
                                state.notify("Check-in guardado")
                            }
                        },
                    )
                }
            }
        } else {
            AnimatedVisibility(visible = true, enter = fadeIn() + scaleIn(initialScale = 0.9f)) {
                GradientCard(brush = assessment.riskLevel.brush) {
                    Text("RESULTADO DE TU CHECK-IN", style = MaterialTheme.typography.labelMedium)
                    Text(
                        "${assessment.riskLevel.emoji}  ${assessment.riskLevel.label}",
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    Text(assessment.recommendation, style = MaterialTheme.typography.bodyLarge)
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
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                ) {
                    Text("🆘 Ir al protocolo de emergencia")
                }
            } else {
                Button(onClick = { navigator.goTo(Screen.Inicio) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Volver a inicio")
                }
            }
        }
    }
}

@Composable
private fun AnswerTile(label: String, accent: Color, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = accent.copy(alpha = 0.13f),
        contentColor = accent,
        modifier = modifier.height(88.dp).clickable(onClick = onClick),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(label, style = MaterialTheme.typography.headlineSmall)
        }
    }
}
