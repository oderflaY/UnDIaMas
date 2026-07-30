package com.eter.undiamas.features.sobriedad.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.eter.undiamas.core.presentation.AppState
import com.eter.undiamas.core.presentation.components.GradientCard
import com.eter.undiamas.core.presentation.components.StreakRing
import com.eter.undiamas.core.presentation.formatClock
import com.eter.undiamas.core.presentation.formatStreak
import com.eter.undiamas.core.presentation.rememberNow
import com.eter.undiamas.core.presentation.streakDays
import com.eter.undiamas.core.presentation.theme.HeroBrush
import com.eter.undiamas.core.presentation.theme.Mint60
import com.eter.undiamas.core.presentation.theme.Violet80
import kotlinx.datetime.Clock

@Composable
fun SobrietyScreen(state: AppState) {
    var showConfirm by remember { mutableStateOf(false) }
    val now by rememberNow()
    val streakSeconds = state.sobrietyCounter.currentStreakSeconds(state.profile, now)
    val record = state.profile.recordStreakSeconds
    val progress = if (record > 0) streakSeconds.toFloat() / record else 1f

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        GradientCard(brush = HeroBrush) {
            Text("TU RACHA ACTUAL", style = MaterialTheme.typography.labelMedium)
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                StreakRing(
                    progress = progress,
                    label = "${streakDays(streakSeconds)} d",
                    caption = formatClock(streakSeconds),
                    ringColors = listOf(Color.White, Mint60, Violet80, Color.White),
                    trackColor = Color.White.copy(alpha = 0.25f),
                    size = 220,
                )
            }
            Text(
                state.sobrietyCounter.motivationalMessage(streakSeconds, record),
                style = MaterialTheme.typography.bodyLarge,
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("🏆 RÉCORD", style = MaterialTheme.typography.labelMedium)
                    Text(formatStreak(record), style = MaterialTheme.typography.titleLarge)
                }
            }
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("📅 DÍAS", style = MaterialTheme.typography.labelMedium)
                    Text("${streakDays(streakSeconds)}", style = MaterialTheme.typography.titleLarge)
                }
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
