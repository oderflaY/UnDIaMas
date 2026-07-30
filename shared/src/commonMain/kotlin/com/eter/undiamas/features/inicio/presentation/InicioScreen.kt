package com.eter.undiamas.features.inicio.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.eter.undiamas.core.presentation.AppState
import com.eter.undiamas.core.presentation.Navigator
import com.eter.undiamas.core.presentation.Screen
import com.eter.undiamas.core.presentation.color
import com.eter.undiamas.core.presentation.formatStreak
import com.eter.undiamas.core.presentation.label
import kotlinx.datetime.Clock

@Composable
fun InicioScreen(state: AppState, navigator: Navigator) {
    val now = Clock.System.now()
    val streakSeconds = state.sobrietyCounter.currentStreakSeconds(state.profile, now)
    val message = state.sobrietyCounter.motivationalMessage(streakSeconds, state.profile.recordStreakSeconds)
    val lastCheckIn = state.checkIns.firstOrNull()

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Text("Hola, ${state.profile.displayName}", style = MaterialTheme.typography.headlineSmall)
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth().clickable { navigator.goTo(Screen.Sobriedad) },
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Llevas sobrio/a", style = MaterialTheme.typography.labelLarge)
                    Text(formatStreak(streakSeconds), style = MaterialTheme.typography.displaySmall)
                    Text(message, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = (lastCheckIn?.riskLevel?.color ?: Color(0xFFB0BEC5)).copy(alpha = 0.15f),
                ),
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Semáforo de hoy", style = MaterialTheme.typography.labelLarge)
                    Text(
                        lastCheckIn?.riskLevel?.label ?: "Aún no haces tu check-in de hoy",
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
        }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(
                    listOf(
                        "Hacer check-in" to Screen.CheckIn,
                        "Escribir en el diario" to Screen.Diario,
                        "Calcular mi ahorro" to Screen.Calculadora,
                        "Hablar con el asistente" to Screen.Ia,
                    ),
                ) { (label, screen) ->
                    Button(onClick = { navigator.goTo(screen) }) { Text(label) }
                }
            }
        }
        item {
            Button(onClick = { navigator.goTo(Screen.Emergencia) }) {
                Text("Necesito ayuda ahora")
            }
        }
    }
}
