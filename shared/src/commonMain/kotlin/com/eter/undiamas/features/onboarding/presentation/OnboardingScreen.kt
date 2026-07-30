package com.eter.undiamas.features.onboarding.presentation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.eter.undiamas.core.presentation.AppState
import com.eter.undiamas.core.presentation.components.GradientCard
import com.eter.undiamas.core.presentation.theme.HeroBrush
import com.eter.undiamas.core.presentation.theme.Violet60

private const val TOTAL_STEPS = 5

@Composable
fun OnboardingScreen(state: AppState) {
    var step by remember { mutableStateOf(0) }

    var name by remember { mutableStateOf("") }
    var daysSober by remember { mutableStateOf("0") }
    var recordDays by remember { mutableStateOf("0") }
    var dailyExpense by remember { mutableStateOf("") }
    var contactName by remember { mutableStateOf("") }
    var contactPhone by remember { mutableStateOf("") }

    val canAdvance = when (step) {
        0 -> name.isNotBlank()
        1 -> daysSober.toLongOrNull() != null
        2 -> recordDays.toLongOrNull() != null
        3 -> dailyExpense.toDoubleOrNull() != null
        else -> true
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        GradientCard(brush = HeroBrush) {
            Text("BIENVENIDO/A A UN DÍA MÁS", style = MaterialTheme.typography.labelMedium)
            Text(
                "Cuéntanos un poco de ti para acompañarte mejor",
                style = MaterialTheme.typography.headlineSmall,
            )
        }

        // Progreso por segmentos, uno por pregunta.
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            repeat(TOTAL_STEPS) { index ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp)
                        .background(
                            if (index <= step) Violet60 else Violet60.copy(alpha = 0.18f),
                            RoundedCornerShape(3.dp),
                        ),
                )
            }
        }
        Text("PASO ${step + 1} DE $TOTAL_STEPS", style = MaterialTheme.typography.labelMedium, color = Violet60)

        AnimatedContent(
            targetState = step,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "onboarding-step",
        ) { current ->
            when (current) {
                0 -> QuestionStep(
                    emoji = "👋",
                    question = "¿Cómo quieres que te llamemos?",
                    hint = "Puede ser tu nombre o un apodo.",
                ) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Tu nombre") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                    )
                }

                1 -> QuestionStep(
                    emoji = "🌱",
                    question = "¿Cuántos días llevas sobrio/a?",
                    hint = "Si empiezas hoy, deja 0. El contador arranca desde ahí.",
                ) {
                    NumberField(daysSober, { daysSober = it }, "Días")
                }

                2 -> QuestionStep(
                    emoji = "🏆",
                    question = "¿Cuál es tu récord anterior?",
                    hint = "Tu mejor racha hasta hoy, en días. Si es la primera vez, deja 0.",
                ) {
                    NumberField(recordDays, { recordDays = it }, "Días de récord")
                }

                3 -> QuestionStep(
                    emoji = "💰",
                    question = "¿Cuánto gastabas al día?",
                    hint = "Nos sirve para calcular cuánto llevas ahorrado.",
                ) {
                    NumberField(dailyExpense, { dailyExpense = it }, "Gasto diario (MXN)", decimal = true)
                }

                else -> QuestionStep(
                    emoji = "🤝",
                    question = "¿A quién llamamos si estás en riesgo?",
                    hint = "Tu contacto de confianza aparecerá en el protocolo de emergencia. Puedes dejarlo en blanco y agregarlo después.",
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = contactName,
                            onValueChange = { contactName = it },
                            label = { Text("Nombre del contacto") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium,
                        )
                        OutlinedTextField(
                            value = contactPhone,
                            onValueChange = { contactPhone = it },
                            label = { Text("Teléfono") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium,
                        )
                    }
                }
            }
        }

        Button(
            enabled = canAdvance,
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                if (step < TOTAL_STEPS - 1) {
                    step += 1
                } else {
                    state.completeOnboarding(
                        displayName = name,
                        daysSober = daysSober.toLongOrNull() ?: 0,
                        recordDays = recordDays.toLongOrNull() ?: 0,
                        previousDailyExpense = dailyExpense.toDoubleOrNull() ?: 0.0,
                        contactName = contactName,
                        contactPhone = contactPhone,
                    )
                }
            },
        ) {
            Text(if (step < TOTAL_STEPS - 1) "Continuar" else "Comenzar 🎉")
        }

        if (step > 0) {
            TextButton(onClick = { step -= 1 }, modifier = Modifier.fillMaxWidth()) {
                Text("Atrás")
            }
        }
    }
}

@Composable
private fun QuestionStep(
    emoji: String,
    question: String,
    hint: String,
    field: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(emoji, style = MaterialTheme.typography.displaySmall)
        Text(question, style = MaterialTheme.typography.headlineSmall)
        Text(
            hint,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        field()
    }
}

@Composable
private fun NumberField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    decimal: Boolean = false,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { input ->
            // Solo dígitos (y un punto cuando aplica) para que el campo no acepte basura.
            val filtered = input.filter { it.isDigit() || (decimal && it == '.') }
            onValueChange(filtered)
        },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Number,
        ),
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
    )
}
