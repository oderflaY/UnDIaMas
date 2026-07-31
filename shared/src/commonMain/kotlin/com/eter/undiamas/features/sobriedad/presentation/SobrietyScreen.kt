package com.eter.undiamas.features.sobriedad.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
import com.eter.undiamas.core.domain.model.Trigger
import com.eter.undiamas.core.presentation.AppState
import com.eter.undiamas.core.presentation.components.GradientCard
import com.eter.undiamas.core.presentation.components.SectionCard
import com.eter.undiamas.core.presentation.components.StreakRing
import com.eter.undiamas.core.presentation.components.pressable
import com.eter.undiamas.core.presentation.formatClock
import com.eter.undiamas.core.presentation.rememberNow
import com.eter.undiamas.core.presentation.streakDays
import com.eter.undiamas.core.presentation.theme.AccentCheckIn
import com.eter.undiamas.core.presentation.theme.PrimaryVioletBrush
import com.eter.undiamas.core.presentation.theme.RiskGreen
import com.eter.undiamas.core.presentation.theme.SavingsGoldEnd
import kotlin.time.Clock
import com.eter.undiamas.core.presentation.theme.AppIcons
import com.eter.undiamas.core.presentation.components.SectionHeader
import com.eter.undiamas.core.presentation.icon
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SobrietyScreen(state: AppState) {
    var showConfirm by remember { mutableStateOf(false) }
    var showPostMortem by remember { mutableStateOf(false) }
    var postMortemTriggers by remember { mutableStateOf(emptySet<Trigger>()) }

    val now by rememberNow()
    val streakSeconds = state.sobrietyCounter.currentStreakSeconds(state.profile, now)
    val record = state.profile.recordStreakSeconds
    val days = streakDays(streakSeconds)
    val progress = if (record > 0) streakSeconds.toFloat() / record else 1f

    val nextMilestone = state.milestones.next(days)
    val daysToNext = state.milestones.daysUntilNext(days)

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        GradientCard(brush = PrimaryVioletBrush) {
            Text("TU RACHA ACTUAL", style = MaterialTheme.typography.labelMedium)
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                StreakRing(
                    progress = progress,
                    days = "$days",
                    clock = formatClock(streakSeconds),
                    ringColors = listOf(Color.White, RiskGreen, SavingsGoldEnd, Color.White),
                    trackColor = Color.White.copy(alpha = 0.22f),
                    size = 220,
                )
            }
            Text(
                state.sobrietyCounter.motivationalMessage(streakSeconds, record),
                style = MaterialTheme.typography.bodyLarge,
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            SectionCard(modifier = Modifier.weight(1f)) {
                SectionHeader(AppIcons.Record, "Récord", SavingsGoldEnd)
                Text("${streakDays(record)} días", style = MaterialTheme.typography.titleLarge)
            }
            SectionCard(modifier = Modifier.weight(1f)) {
                SectionHeader(AppIcons.Calendario, "Días totales", RiskGreen)
                Text("$days", style = MaterialTheme.typography.titleLarge)
            }
        }

        SectionCard {
            SectionHeader(AppIcons.Insignia, "Próximo hito", SavingsGoldEnd)
            if (nextMilestone != null && daysToNext != null) {
                Text(
                    "Te faltan $daysToNext días para tu placa de ${nextMilestone.title}",
                    style = MaterialTheme.typography.bodyMedium,
                )
                LinearProgressIndicator(
                    progress = { (days.toFloat() / nextMilestone.days).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(8.dp),
                )
            } else {
                Text("Alcanzaste todos los hitos registrados.", style = MaterialTheme.typography.bodyMedium)
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                state.milestones.all.forEach { milestone ->
                    val reached = days >= milestone.days
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = if (reached) RiskGreen.copy(alpha = 0.28f) else MaterialTheme.colorScheme.surface,
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                if (reached) AppIcons.Insignia else AppIcons.Bloqueado,
                                contentDescription = null,
                                tint = if (reached) RiskGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp),
                            )
                            Text(
                                "${milestone.days}d",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (reached) {
                                    MaterialTheme.colorScheme.onSurface
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                            )
                        }
                    }
                }
            }
        }

        OutlinedButton(onClick = { showConfirm = true }, modifier = Modifier.fillMaxWidth()) {
            Text("Registrar recaída")
        }
    }

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            title = { Text("¿Reiniciar el contador?") },
            text = {
                Text(
                    "Caer es parte del proceso de aprendizaje. Esto no borra tu esfuerzo " +
                        "ni tu récord histórico. ¿Deseas reiniciar el contador?",
                )
            },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    onClick = {
                        state.updateProfile { profile ->
                            state.sobrietyCounter.registerRelapse(profile, Clock.System.now())
                        }
                        showConfirm = false
                        showPostMortem = true
                    },
                ) { Text("Sí, reiniciar") }
            },
            dismissButton = { TextButton(onClick = { showConfirm = false }) { Text("Cancelar") } },
        )
    }

    if (showPostMortem) {
        AlertDialog(
            onDismissRequest = { showPostMortem = false },
            title = { Text("¿Qué crees que lo detonó?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Reconocerlo no es culparte: es información para reforzar tu plan.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Trigger.entries.forEach { trigger ->
                            val selected = trigger in postMortemTriggers
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = if (selected) {
                                    AccentCheckIn.copy(alpha = 0.3f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                },
                                modifier = Modifier.pressable({
                                    postMortemTriggers = if (selected) {
                                        postMortemTriggers - trigger
                                    } else {
                                        postMortemTriggers + trigger
                                    }
                                }),
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(trigger.icon, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Text(trigger.label, style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    showPostMortem = false
                    state.notify("Tu récord se conserva. Empezamos de nuevo, juntos.")
                    postMortemTriggers = emptySet()
                }) { Text("Guardar y continuar") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showPostMortem = false
                    postMortemTriggers = emptySet()
                }) { Text("Ahora no") }
            },
        )
    }
}
