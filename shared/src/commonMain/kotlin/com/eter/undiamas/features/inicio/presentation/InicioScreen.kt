package com.eter.undiamas.features.inicio.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
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
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Text(
                "Hola, ${state.profile.displayName}",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth().clickable { navigator.goTo(Screen.Sobriedad) },
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            ) {
                Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Llevas sobrio/a", style = MaterialTheme.typography.labelLarge)
                    Text(
                        formatStreak(streakSeconds),
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(message, style = MaterialTheme.typography.bodyMedium)
                    Text("Toca para ver el detalle →", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
        item {
            val riskColor = lastCheckIn?.riskLevel?.color ?: MaterialTheme.colorScheme.outline
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = riskColor.copy(alpha = 0.12f)),
                border = BorderStroke(1.dp, riskColor.copy(alpha = 0.4f)),
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Semáforo de hoy", style = MaterialTheme.typography.labelLarge)
                    Text(
                        lastCheckIn?.riskLevel?.label ?: "Aún no haces tu check-in de hoy",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
        items(
            listOf(
                "Hacer check-in" to Screen.CheckIn,
                "Escribir en el diario" to Screen.Diario,
                "Calcular mi ahorro" to Screen.Calculadora,
                "Hablar con el asistente" to Screen.Ia,
            ),
        ) { (label, screen) ->
            OutlinedButton(onClick = { navigator.goTo(screen) }, modifier = Modifier.fillMaxWidth()) { Text(label) }
        }
        item {
            Button(
                onClick = { navigator.goTo(Screen.Emergencia) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                ),
            ) {
                Text("Necesito ayuda ahora", fontWeight = FontWeight.Bold)
            }
        }
    }
}
