package com.eter.undiamas.features.checkin.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.eter.undiamas.core.domain.model.CheckInEntry
import com.eter.undiamas.core.domain.model.RiskAssessment
import com.eter.undiamas.core.domain.model.RiskLevel
import com.eter.undiamas.core.presentation.AppState
import com.eter.undiamas.core.presentation.Navigator
import com.eter.undiamas.core.presentation.Screen
import com.eter.undiamas.core.presentation.color
import com.eter.undiamas.core.presentation.label
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
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text("Check-in", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

        val assessment = result
        if (assessment == null) {
            LinearProgressIndicator(
                progress = { (stepIndex + 1f) / questions.size },
                modifier = Modifier.fillMaxWidth(),
            )
            Text("Pregunta ${stepIndex + 1} de ${questions.size}", style = MaterialTheme.typography.labelMedium)

            val question = questions[stepIndex]
            Text(question.text, style = MaterialTheme.typography.titleMedium)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("si" to "Sí", "no" to "No").forEach { (value, label) ->
                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(),
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
                            }
                        },
                    ) { Text(label) }
                }
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = assessment.riskLevel.color.copy(alpha = 0.12f)),
            ) {
                Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        assessment.riskLevel.label,
                        color = assessment.riskLevel.color,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(assessment.recommendation, style = MaterialTheme.typography.bodyLarge)
                }
            }
            if (assessment.riskLevel == RiskLevel.ROJO) {
                Button(
                    onClick = { navigator.goTo(Screen.Emergencia) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                ) {
                    Text("Ir al protocolo de emergencia", fontWeight = FontWeight.Bold)
                }
            } else {
                Button(onClick = { navigator.goTo(Screen.Inicio) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Volver a inicio")
                }
            }
        }
    }
}
