package com.eter.undiamas.features.sobriedad.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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

@Composable
fun SobrietyScreen(state: AppState) {
    var showConfirm by remember { mutableStateOf(false) }
    val now = Clock.System.now()
    val streakSeconds = state.sobrietyCounter.currentStreakSeconds(state.profile, now)

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Tu racha de sobriedad", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        ) {
            Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(formatStreak(streakSeconds), style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold)
                Text(
                    state.sobrietyCounter.motivationalMessage(streakSeconds, state.profile.recordStreakSeconds),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Récord histórico", style = MaterialTheme.typography.labelLarge)
                Text(formatStreak(state.profile.recordStreakSeconds), style = MaterialTheme.typography.titleLarge)
            }
        }

        OutlinedButton(onClick = { showConfirm = true }, modifier = Modifier.fillMaxWidth()) {
            Text("Registrar una recaída")
        }
    }

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            title = { Text("¿Registrar recaída?") },
            text = {
                Text(
                    "Tu racha actual se reinicia, pero tu récord histórico se conserva. " +
                        "No hay culpa aquí, solo un nuevo punto de partida.",
                )
            },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    onClick = {
                        state.updateProfile { profile -> state.sobrietyCounter.registerRelapse(profile, Clock.System.now()) }
                        state.notify("Recaída registrada. Tu récord se conserva.")
                        showConfirm = false
                    },
                ) { Text("Confirmar") }
            },
            dismissButton = {
                TextButton(onClick = { showConfirm = false }) { Text("Cancelar") }
            },
        )
    }
}
