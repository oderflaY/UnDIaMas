package com.eter.undiamas.features.sobriedad.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
        Text("Tu racha de sobriedad", style = MaterialTheme.typography.headlineSmall)
        Text(formatStreak(streakSeconds), style = MaterialTheme.typography.displayMedium)
        Text(
            "Récord histórico: ${formatStreak(state.profile.recordStreakSeconds)}",
            style = MaterialTheme.typography.bodyLarge,
        )
        Text(
            state.sobrietyCounter.motivationalMessage(streakSeconds, state.profile.recordStreakSeconds),
            style = MaterialTheme.typography.bodyMedium,
        )
        OutlinedButton(onClick = { showConfirm = true }) {
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
                TextButton(onClick = {
                    state.updateProfile { profile -> state.sobrietyCounter.registerRelapse(profile, Clock.System.now()) }
                    showConfirm = false
                }) { Text("Confirmar") }
            },
            dismissButton = {
                Button(onClick = { showConfirm = false }) { Text("Cancelar") }
            },
        )
    }
}
