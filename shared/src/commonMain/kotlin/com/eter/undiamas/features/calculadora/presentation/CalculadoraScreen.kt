package com.eter.undiamas.features.calculadora.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.eter.undiamas.core.presentation.AppState
import com.eter.undiamas.core.presentation.formatStreak
import kotlinx.datetime.Clock
import kotlin.math.roundToInt

private const val PRECIO_REFERENCIA_CAFE = 45.0

@Composable
fun CalculadoraScreen(state: AppState) {
    var expenseText by remember { mutableStateOf(state.profile.previousDailyExpense.toString()) }
    val previousDailyExpense = expenseText.toDoubleOrNull() ?: 0.0
    val streakSeconds = state.sobrietyCounter.currentStreakSeconds(state.profile, Clock.System.now())

    val daily = state.savingsCalculator.dailySavings(previousDailyExpense)
    val weekly = state.savingsCalculator.weeklySavings(previousDailyExpense)
    val monthly = state.savingsCalculator.monthlySavings(previousDailyExpense)
    val total = state.savingsCalculator.totalSavings(previousDailyExpense, streakSeconds)

    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Calculadora de ahorro", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(
            "Llevas ${formatStreak(streakSeconds)} sin gastar en tu consumo previo.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        OutlinedTextField(
            value = expenseText,
            onValueChange = { expenseText = it },
            label = { Text("Gasto diario previo (MXN)") },
            modifier = Modifier.fillMaxWidth(),
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Ahorro total desde tu inicio", style = MaterialTheme.typography.labelLarge)
                Text("$${total.roundToInt()}", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
            }
        }

        listOf(
            "Ahorro diario" to daily,
            "Ahorro semanal" to weekly,
            "Ahorro mensual" to monthly,
        ).forEach { (titulo, monto) ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(titulo, style = MaterialTheme.typography.labelLarge)
                    Text("$${monto.roundToInt()}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Eso equivale a", style = MaterialTheme.typography.labelLarge)
                Text(
                    "≈ ${(total / PRECIO_REFERENCIA_CAFE).roundToInt()} cafés",
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
    }
}
