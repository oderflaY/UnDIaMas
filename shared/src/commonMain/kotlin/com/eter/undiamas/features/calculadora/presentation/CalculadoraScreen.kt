package com.eter.undiamas.features.calculadora.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.eter.undiamas.core.presentation.AppState
import com.eter.undiamas.core.presentation.components.GradientCard
import com.eter.undiamas.core.presentation.formatStreak
import com.eter.undiamas.core.presentation.theme.AccentAhorro
import com.eter.undiamas.core.presentation.theme.AccentDiario
import com.eter.undiamas.core.presentation.theme.AccentStats
import com.eter.undiamas.core.presentation.theme.SavingsBrush
import kotlinx.datetime.Clock
import kotlin.math.roundToInt

private const val PRECIO_CAFE = 45.0
private const val PRECIO_LIBRO = 250.0
private const val PRECIO_CONCIERTO = 900.0

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
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("💰 Tu ahorro", style = MaterialTheme.typography.headlineMedium)

        GradientCard(brush = SavingsBrush) {
            Text("AHORRO TOTAL", style = MaterialTheme.typography.labelMedium)
            Text("$${total.roundToInt()}", style = MaterialTheme.typography.displayMedium)
            Text(
                "En ${formatStreak(streakSeconds)} sin gastar en tu consumo previo.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        OutlinedTextField(
            value = expenseText,
            onValueChange = { expenseText = it },
            label = { Text("Gasto diario previo (MXN)") },
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
        )

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            MoneyTile("DÍA", daily, AccentAhorro, Modifier.weight(1f))
            MoneyTile("SEMANA", weekly, AccentStats, Modifier.weight(1f))
            MoneyTile("MES", monthly, AccentDiario, Modifier.weight(1f))
        }

        Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceVariant) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Eso ya equivale a…", style = MaterialTheme.typography.titleMedium)
                Equivalence("☕", "cafés", (total / PRECIO_CAFE).roundToInt())
                Equivalence("📚", "libros", (total / PRECIO_LIBRO).roundToInt())
                Equivalence("🎟️", "conciertos", (total / PRECIO_CONCIERTO).roundToInt())
            }
        }
    }
}

@Composable
private fun MoneyTile(period: String, amount: Double, accent: Color, modifier: Modifier = Modifier) {
    Surface(shape = MaterialTheme.shapes.medium, color = accent.copy(alpha = 0.13f), modifier = modifier) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(period, style = MaterialTheme.typography.labelMedium, color = accent)
            Text("$${amount.roundToInt()}", style = MaterialTheme.typography.titleLarge)
        }
    }
}

@Composable
private fun Equivalence(emoji: String, unit: String, count: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text("$emoji  $unit", style = MaterialTheme.typography.bodyMedium)
        Text("≈ $count", style = MaterialTheme.typography.titleMedium, color = AccentAhorro)
    }
}
