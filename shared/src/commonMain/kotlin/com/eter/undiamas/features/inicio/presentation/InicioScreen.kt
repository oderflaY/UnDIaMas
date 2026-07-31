package com.eter.undiamas.features.inicio.presentation

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.eter.undiamas.core.domain.model.Mood
import com.eter.undiamas.core.domain.model.RiskLevel
import com.eter.undiamas.core.presentation.AppState
import com.eter.undiamas.core.presentation.Navigator
import com.eter.undiamas.core.presentation.Screen
import com.eter.undiamas.core.presentation.color
import com.eter.undiamas.core.presentation.components.ActionTile
import com.eter.undiamas.core.presentation.components.GradientCard
import com.eter.undiamas.core.presentation.components.SectionCard
import com.eter.undiamas.core.presentation.components.StreakRing
import com.eter.undiamas.core.presentation.components.TrafficLight
import com.eter.undiamas.core.presentation.components.pressable
import com.eter.undiamas.core.presentation.components.shake
import com.eter.undiamas.core.presentation.emoji
import com.eter.undiamas.core.presentation.formatClock
import com.eter.undiamas.core.presentation.greetingForHour
import com.eter.undiamas.core.presentation.label
import com.eter.undiamas.core.presentation.motivationalQuotes
import com.eter.undiamas.core.presentation.rememberNow
import com.eter.undiamas.core.presentation.streakDays
import com.eter.undiamas.core.presentation.theme.AccentAhorro
import com.eter.undiamas.core.presentation.theme.AccentAsistente
import com.eter.undiamas.core.presentation.theme.AccentCheckIn
import com.eter.undiamas.core.presentation.theme.AccentDiario
import com.eter.undiamas.core.presentation.theme.EmergencyBrush
import com.eter.undiamas.core.presentation.theme.PrimaryVioletBrush
import com.eter.undiamas.core.presentation.theme.RiskGreen
import com.eter.undiamas.core.presentation.theme.SavingsGoldEnd
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@Composable
fun InicioScreen(state: AppState, navigator: Navigator) {
    val now by rememberNow()
    val streakSeconds = state.sobrietyCounter.currentStreakSeconds(state.profile, now)
    val record = state.profile.recordStreakSeconds
    val progress = if (record > 0) streakSeconds.toFloat() / record else 1f

    val timeZone = TimeZone.currentSystemDefault()
    val localNow = now.toLocalDateTime(timeZone)
    val today = localNow.date
    // El semáforo del día debe salir del registro de HOY, no del último check-in sin más:
    // si el último fue ayer, hoy vuelve a estar pendiente.
    val todayLevel = state.checkInHistory.byDay(state.checkIns, timeZone)[today]
    val cleanDays = state.checkInHistory.cleanDays(state.checkIns, timeZone)
    val todayMood = state.moodEntries.firstOrNull {
        it.registeredAt.toLocalDateTime(timeZone).date == today
    }?.mood

    var quoteIndex by remember { mutableStateOf(0) }
    val enRiesgo = todayLevel == RiskLevel.AMARILLO || todayLevel == RiskLevel.ROJO

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(greetingForHour(localNow.hour), style = MaterialTheme.typography.bodyLarge)
                Text(state.profile.displayName, style = MaterialTheme.typography.titleLarge)
            }
        }

        item {
            MoodSelector(
                selected = todayMood,
                onSelect = { mood ->
                    state.registerMood(mood)
                    state.notify("Ánimo registrado: ${mood.label}")
                },
            )
        }

        item {
            GradientCard(brush = PrimaryVioletBrush, onClick = { navigator.goTo(Screen.Sobriedad) }) {
                Text("LLEVAS SOBRIO/A", style = MaterialTheme.typography.labelMedium)
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    StreakRing(
                        progress = progress,
                        days = "${streakDays(streakSeconds)}",
                        clock = formatClock(streakSeconds),
                        ringColors = listOf(Color.White, RiskGreen, SavingsGoldEnd, Color.White),
                        trackColor = Color.White.copy(alpha = 0.22f),
                    )
                }
            }
        }

        item {
            SectionCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        if (record > 0) "🏆 Tu récord: ${streakDays(record)} días" else "🏆 Aún sin récord",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                        modifier = Modifier.pressable({ quoteIndex = (quoteIndex + 1) % motivationalQuotes.size }),
                    ) {
                        Text("🔄", modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
                    }
                }
                Text(
                    motivationalQuotes[quoteIndex],
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item {
            SectionCard(
                containerColor = (todayLevel?.color ?: MaterialTheme.colorScheme.outline).copy(alpha = 0.14f),
                onClick = { navigator.goTo(Screen.CheckIn) },
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
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
                                "✅ Día registrado · $cleanDays días limpios"
                            } else {
                                "Toca para responder tus preguntas de hoy"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    TrafficLight(level = todayLevel, modifier = Modifier.height(22.dp).width(90.dp))
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                ActionTile("✅", "Check-in", AccentCheckIn, { navigator.goTo(Screen.CheckIn) }, Modifier.weight(1f))
                ActionTile("📓", "Diario", AccentDiario, { navigator.goTo(Screen.Diario) }, Modifier.weight(1f))
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                ActionTile("💰", "Mi ahorro", AccentAhorro, { navigator.goTo(Screen.Calculadora) }, Modifier.weight(1f))
                ActionTile("💬", "Asistente", AccentAsistente, { navigator.goTo(Screen.Ia) }, Modifier.weight(1f))
            }
        }

        item {
            // El banner tiembla solo cuando el semáforo del día está en amarillo o rojo.
            GradientCard(
                brush = EmergencyBrush,
                modifier = Modifier.shake(enRiesgo),
                onClick = { navigator.goTo(Screen.Emergencia) },
            ) {
                Text("🆘 Necesito ayuda ahora", style = MaterialTheme.typography.titleLarge)
                Text(
                    "Respiración guiada, ejercicio de anclaje y tu contacto de confianza.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun MoodSelector(selected: Mood?, onSelect: (Mood) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        Mood.entries.forEach { mood ->
            val isSelected = mood == selected
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
                modifier = Modifier.pressable({ onSelect(mood) }),
            ) {
                Text(
                    mood.emoji,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                )
            }
        }
    }
}
