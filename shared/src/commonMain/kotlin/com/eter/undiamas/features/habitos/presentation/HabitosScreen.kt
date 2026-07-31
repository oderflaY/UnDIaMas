package com.eter.undiamas.features.habitos.presentation

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.eter.undiamas.core.presentation.AppState
import com.eter.undiamas.core.presentation.components.GradientCard
import com.eter.undiamas.core.presentation.components.GuardedBack
import com.eter.undiamas.core.presentation.components.SectionHeaderLarge
import com.eter.undiamas.core.presentation.components.pressable
import com.eter.undiamas.core.presentation.rememberNow
import com.eter.undiamas.core.presentation.theme.AppIcons
import com.eter.undiamas.core.presentation.theme.RiskGreen
import com.eter.undiamas.core.presentation.theme.SavingsBrush
import com.eter.undiamas.features.habitos.domain.Habit
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime

private val diaCorto = listOf("L", "M", "X", "J", "V", "S", "D")

@Composable
fun HabitosScreen(state: AppState) {
    val now by rememberNow(intervalMillis = 60_000)
    val today = now.toLocalDateTime(TimeZone.currentSystemDefault()).date
    var selectedDate by remember { mutableStateOf(today) }
    var creating by remember { mutableStateOf(false) }

    val tracker = state.habitTracker
    val rate = tracker.completionRate(state.habitCompletions, state.habits, selectedDate)
    val doneCount = tracker.completedOn(state.habitCompletions, state.habits, selectedDate)

    // Últimos 7 días, del más antiguo al de hoy.
    val week = remember(today) { (6 downTo 0).map { today.minus(DatePeriod(days = it)) } }

    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        SectionHeaderLarge(AppIcons.Habitos, "Laboratorio de hábitos")

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            items(week) { date ->
                DayChip(
                    date = date,
                    selected = date == selectedDate,
                    isToday = date == today,
                    onClick = { selectedDate = date },
                )
            }
        }

        GradientCard(brush = SavingsBrush) {
            Text("PROGRESO DEL DÍA", style = MaterialTheme.typography.labelMedium)
            Text(
                if (state.habits.isEmpty()) "Sin hábitos aún" else "$doneCount de ${state.habits.size} cumplidos",
                style = MaterialTheme.typography.headlineSmall,
            )
            LinearProgressIndicator(
                progress = { rate },
                modifier = Modifier.fillMaxWidth().height(10.dp),
            )
        }

        Button(
            onClick = { creating = true },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = RiskGreen),
        ) {
            Icon(AppIcons.Habitos, contentDescription = null, modifier = Modifier.size(20.dp))
            Text("  Nuevo hábito")
        }

        if (state.habits.isEmpty()) {
            Text(
                "Empieza con algo tan pequeño que sea difícil fallar: un vaso de agua, diez páginas, " +
                    "cinco respiraciones.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            items(state.habits, key = { it.id }) { habit ->
                HabitCard(
                    habit = habit,
                    done = tracker.isDone(state.habitCompletions, habit.id, selectedDate),
                    streak = tracker.currentStreak(state.habitCompletions, habit.id, today),
                    best = tracker.bestStreak(state.habitCompletions, habit.id),
                    onToggle = { state.toggleHabit(habit.id, selectedDate) },
                    onDelete = { state.removeHabit(habit.id) },
                )
            }
        }
    }

    if (creating) {
        HabitCreator(
            onDismiss = { creating = false },
            onSave = { name ->
                state.addHabit(name)
                state.notify("Hábito creado")
                creating = false
            },
        )
    }
}

@Composable
private fun DayChip(date: LocalDate, selected: Boolean, isToday: Boolean, onClick: () -> Unit) {
    // dayOfWeek.ordinal va de 0 (lunes) a 6 (domingo) en kotlinx-datetime.
    val label = diaCorto[date.dayOfWeek.ordinal]
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.width(52.dp).pressable(onClick),
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "${date.dayOfMonth}",
                style = MaterialTheme.typography.titleMedium,
                color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
            )
            if (isToday) {
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .let { m ->
                            m.then(
                                Modifier.padding(0.dp),
                            )
                        },
                ) {
                    Surface(
                        shape = MaterialTheme.shapes.extraSmall,
                        color = if (selected) MaterialTheme.colorScheme.onPrimary else RiskGreen,
                        modifier = Modifier.size(5.dp),
                    ) {}
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HabitCard(
    habit: Habit,
    done: Boolean,
    streak: Int,
    best: Int,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    val fill by animateFloatAsState(
        targetValue = if (done) 1f else 0f,
        animationSpec = tween(420),
        label = "habit-fill",
    )
    var confirmDelete by remember { mutableStateOf(false) }

    Surface(
        shape = MaterialTheme.shapes.medium,
        color = if (done) RiskGreen.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onToggle()
                },
                onLongClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    confirmDelete = true
                },
            ),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(habit.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        if (streak > 0) "Cadena de $streak días · récord $best" else "Sin cadena · récord $best",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Icon(
                    if (done) AppIcons.CheckIn else AppIcons.CheckInVacio,
                    contentDescription = if (done) "Cumplido" else "Pendiente",
                    tint = if (done) RiskGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(30.dp),
                )
            }
            LinearProgressIndicator(
                progress = { fill },
                modifier = Modifier.fillMaxWidth().height(6.dp),
            )
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("¿Eliminar «${habit.name}»?") },
            text = { Text("También se borrará su historial de cumplimiento.") },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    onClick = {
                        confirmDelete = false
                        onDelete()
                    },
                ) { Text("Eliminar") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancelar") } },
        )
    }
}

@Composable
private fun HabitCreator(onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var name by remember { mutableStateOf("") }

    // Con texto escrito, atrás confirma antes de descartar.
    GuardedBack(
        enabled = name.isNotBlank(),
        title = "¿Salir sin guardar?",
        message = "Perderás el hábito que estabas creando.",
        confirmLabel = "Descartar",
        onConfirmExit = onDismiss,
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nuevo hábito") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("¿Qué quieres sostener?") },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                singleLine = true,
            )
        },
        confirmButton = {
            Button(enabled = name.isNotBlank(), onClick = { onSave(name) }) { Text("Crear") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}
