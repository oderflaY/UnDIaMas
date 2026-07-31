package com.eter.undiamas.features.calculadora.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.eter.undiamas.core.domain.model.SavingsGoal
import com.eter.undiamas.core.presentation.AppState
import com.eter.undiamas.core.presentation.components.GradientCard
import com.eter.undiamas.core.presentation.components.OdometerText
import com.eter.undiamas.core.presentation.components.SectionCard
import com.eter.undiamas.core.presentation.components.pressable
import com.eter.undiamas.core.presentation.formatStreak
import com.eter.undiamas.core.presentation.rememberNow
import com.eter.undiamas.core.presentation.theme.AccentAhorro
import com.eter.undiamas.core.presentation.theme.SavingsBrush
import kotlin.math.roundToInt
import com.eter.undiamas.core.presentation.theme.AppIcons
import com.eter.undiamas.core.presentation.components.SectionHeaderLarge
import com.eter.undiamas.core.presentation.components.SectionHeader
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon

private const val PRECIO_CAFE = 45.0
private const val PRECIO_LIBRO = 250.0
private const val PRECIO_CONCIERTO = 900.0
private const val PRECIO_VIAJE = 8_000.0
private const val TASA_ANUAL_REFERENCIA = 0.10

private enum class Period(val label: String) { DIA("Día"), SEMANA("Semana"), MES("Mes"), ANIO("Año") }

@Composable
fun CalculadoraScreen(state: AppState) {
    var expenseText by remember { mutableStateOf(state.profile.previousDailyExpense.toString()) }
    var period by remember { mutableStateOf(Period.MES) }
    var goalTitle by remember { mutableStateOf(state.profile.savingsGoal?.title.orEmpty()) }
    var goalAmount by remember { mutableStateOf(state.profile.savingsGoal?.targetAmount?.toString().orEmpty()) }

    val expense = expenseText.toDoubleOrNull() ?: 0.0
    val now by rememberNow()
    val streakSeconds = state.sobrietyCounter.currentStreakSeconds(state.profile, now)
    val calculator = state.savingsCalculator

    val total = calculator.totalSavings(expense, streakSeconds)
    val periodValue = when (period) {
        Period.DIA -> calculator.dailySavings(expense)
        Period.SEMANA -> calculator.weeklySavings(expense)
        Period.MES -> calculator.monthlySavings(expense)
        Period.ANIO -> calculator.yearlySavings(expense)
    }
    val goal = state.profile.savingsGoal
    val goalProgress = calculator.goalProgress(total, goal)
    val compound = calculator.compoundProjection(
        monthlyContribution = calculator.monthlySavings(expense),
        annualRate = TASA_ANUAL_REFERENCIA,
        years = 5,
    )

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        SectionHeaderLarge(AppIcons.Ahorro, "Tu ahorro")

        GradientCard(brush = SavingsBrush) {
            Text("AHORRO TOTAL", style = MaterialTheme.typography.labelMedium)
            OdometerText(
                "$${total.roundToInt()}",
                style = MaterialTheme.typography.displayMedium,
            )
            Text(
                "En ${formatStreak(streakSeconds)} sin gastar en tu consumo previo.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        OutlinedTextField(
            value = expenseText,
            onValueChange = { expenseText = it.filter { c -> c.isDigit() || c == '.' } },
            label = { Text("¿Cuánto gastabas al día aproximadamente?") },
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            singleLine = true,
        )

        SectionCard {
            SectionHeader(AppIcons.Tendencia, "Proyección", AccentAhorro)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Period.entries.forEach { option ->
                    val selected = option == period
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = if (selected) {
                            AccentAhorro.copy(alpha = 0.3f)
                        } else {
                            MaterialTheme.colorScheme.surface
                        },
                        modifier = Modifier.weight(1f).pressable({ period = option }),
                    ) {
                        Text(
                            option.label,
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(vertical = 10.dp),
                        )
                    }
                }
            }
            OdometerText(
                "$${periodValue.roundToInt()}",
                style = MaterialTheme.typography.headlineMedium,
            )
        }

        SectionCard {
            SectionHeader(AppIcons.Cartera, "Eso ya equivale a…", AccentAhorro)
            Equivalence(AppIcons.Cafe, "cafés", (total / PRECIO_CAFE).roundToInt())
            Equivalence(AppIcons.Libro, "libros", (total / PRECIO_LIBRO).roundToInt())
            Equivalence(AppIcons.Concierto, "conciertos", (total / PRECIO_CONCIERTO).roundToInt())
            Equivalence(AppIcons.Viaje, "viajes", (total / PRECIO_VIAJE).roundToInt())
        }

        SectionCard {
            SectionHeader(AppIcons.Meta, "Mi meta", AccentAhorro)
            if (goal != null && goalProgress != null) {
                Text(
                    "${goal.title} · $${total.roundToInt()} de $${goal.targetAmount.roundToInt()}",
                    style = MaterialTheme.typography.bodyMedium,
                )
                LinearProgressIndicator(
                    progress = { goalProgress },
                    modifier = Modifier.fillMaxWidth().height(10.dp),
                )
                Text(
                    "${(goalProgress * 100).roundToInt()}% alcanzado",
                    style = MaterialTheme.typography.labelMedium,
                    color = AccentAhorro,
                )
            }
            OutlinedTextField(
                value = goalTitle,
                onValueChange = { goalTitle = it },
                label = { Text("¿Qué te quieres regalar?") },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                singleLine = true,
            )
            OutlinedTextField(
                value = goalAmount,
                onValueChange = { goalAmount = it.filter { c -> c.isDigit() || c == '.' } },
                label = { Text("Costo aproximado") },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                singleLine = true,
            )
            Button(
                enabled = goalTitle.isNotBlank() && goalAmount.toDoubleOrNull() != null,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = AccentAhorro),
                onClick = {
                    state.updateProfile {
                        it.copy(
                            previousDailyExpense = expense,
                            savingsGoal = SavingsGoal(goalTitle, goalAmount.toDouble()),
                        )
                    }
                    state.notify("Meta guardada")
                },
            ) { Text("Guardar meta") }
        }

        SectionCard {
            SectionHeader(AppIcons.Tendencia, "Si lo invirtieras", AccentAhorro)
            Text(
                "$${compound.roundToInt()}",
                style = MaterialTheme.typography.headlineMedium,
                fontFamily = FontFamily.Monospace,
            )
            Text(
                "Es lo que tendrías en 5 años aportando cada mes lo que dejas de gastar, " +
                    "a una tasa anual del 10%. Es una estimación ilustrativa, no una asesoría financiera.",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun Equivalence(icon: ImageVector, unit: String, count: Int) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = AccentAhorro, modifier = Modifier.size(20.dp))
            Text(unit, style = MaterialTheme.typography.bodyMedium)
        }
        Text("≈ $count", style = MaterialTheme.typography.titleMedium, color = AccentAhorro)
    }
}
