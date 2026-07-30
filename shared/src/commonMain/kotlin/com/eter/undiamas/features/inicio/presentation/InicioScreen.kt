package com.eter.undiamas.features.inicio.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.eter.undiamas.core.presentation.AppState
import com.eter.undiamas.core.presentation.Navigator
import com.eter.undiamas.core.presentation.Screen
import com.eter.undiamas.core.presentation.color
import com.eter.undiamas.core.presentation.components.ActionTile
import com.eter.undiamas.core.presentation.components.GradientCard
import com.eter.undiamas.core.presentation.components.StreakRing
import com.eter.undiamas.core.presentation.components.TrafficLight
import com.eter.undiamas.core.presentation.emoji
import com.eter.undiamas.core.presentation.formatClock
import com.eter.undiamas.core.presentation.formatStreak
import com.eter.undiamas.core.presentation.label
import com.eter.undiamas.core.presentation.rememberNow
import com.eter.undiamas.core.presentation.streakDays
import com.eter.undiamas.core.presentation.theme.AccentAhorro
import com.eter.undiamas.core.presentation.theme.AccentAsistente
import com.eter.undiamas.core.presentation.theme.AccentCheckIn
import com.eter.undiamas.core.presentation.theme.AccentDiario
import com.eter.undiamas.core.presentation.theme.EmergencyBrush
import com.eter.undiamas.core.presentation.theme.HeroBrush
import com.eter.undiamas.core.presentation.theme.Mint60
import com.eter.undiamas.core.presentation.theme.Violet80
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@Composable
fun InicioScreen(state: AppState, navigator: Navigator) {
    val now by rememberNow()
    val streakSeconds = state.sobrietyCounter.currentStreakSeconds(state.profile, now)
    val record = state.profile.recordStreakSeconds
    val message = state.sobrietyCounter.motivationalMessage(streakSeconds, record)
    val progress = if (record > 0) streakSeconds.toFloat() / record else 1f

    val timeZone = TimeZone.currentSystemDefault()
    val today = now.toLocalDateTime(timeZone).date
    // El semáforo del día debe salir del registro de HOY, no del último check-in sin más:
    // si el último fue ayer, hoy vuelve a estar pendiente.
    val todayLevel = state.checkInHistory.byDay(state.checkIns, timeZone)[today]
    val cleanDays = state.checkInHistory.cleanDays(state.checkIns, timeZone)

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Text("Hola, ${state.profile.displayName} 👋", style = MaterialTheme.typography.headlineMedium)
        }

        item {
            GradientCard(brush = HeroBrush, onClick = { navigator.goTo(Screen.Sobriedad) }) {
                Text("LLEVAS SOBRIO/A", style = MaterialTheme.typography.labelMedium)
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    StreakRing(
                        progress = progress,
                        label = "${streakDays(streakSeconds)} d",
                        caption = formatClock(streakSeconds),
                        ringColors = listOf(Color.White, Mint60, Violet80, Color.White),
                        trackColor = Color.White.copy(alpha = 0.25f),
                    )
                }
                Text(
                    if (record > 0) "🏆 Récord: ${formatStreak(record)}" else "🏆 Aún sin récord: este es el primero",
                    style = MaterialTheme.typography.labelMedium,
                )
                Text(message, style = MaterialTheme.typography.bodyMedium)
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth().clickable { navigator.goTo(Screen.CheckIn) },
                colors = CardDefaults.cardColors(
                    containerColor = (todayLevel?.color ?: MaterialTheme.colorScheme.outline)
                        .copy(alpha = 0.12f),
                ),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("SEMÁFORO DE HOY", style = MaterialTheme.typography.labelMedium)
                        Text(
                            todayLevel?.let { "${it.emoji} ${it.label}" } ?: "Registra tu día",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            if (todayLevel != null) {
                                "✅ Día registrado · $cleanDays días limpios en total"
                            } else {
                                "Toca para responder tus preguntas de hoy"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    TrafficLight(
                        level = todayLevel,
                        modifier = Modifier.height(22.dp).width(90.dp),
                    )
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                ActionTile(
                    emoji = "✅",
                    title = "Check-in",
                    accent = AccentCheckIn,
                    onClick = { navigator.goTo(Screen.CheckIn) },
                    modifier = Modifier.weight(1f),
                )
                ActionTile(
                    emoji = "📓",
                    title = "Diario",
                    accent = AccentDiario,
                    onClick = { navigator.goTo(Screen.Diario) },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                ActionTile(
                    emoji = "💰",
                    title = "Mi ahorro",
                    accent = AccentAhorro,
                    onClick = { navigator.goTo(Screen.Calculadora) },
                    modifier = Modifier.weight(1f),
                )
                ActionTile(
                    emoji = "💬",
                    title = "Asistente",
                    accent = AccentAsistente,
                    onClick = { navigator.goTo(Screen.Ia) },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        item {
            GradientCard(brush = EmergencyBrush, onClick = { navigator.goTo(Screen.Emergencia) }) {
                Text("🆘 Necesito ayuda ahora", style = MaterialTheme.typography.titleLarge)
                Text(
                    "Respiración guiada y tu contacto de confianza, a un toque.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}
