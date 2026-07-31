package com.eter.undiamas.features.capsulas.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.eter.undiamas.core.presentation.AppState
import com.eter.undiamas.core.presentation.components.GradientCard
import com.eter.undiamas.core.presentation.components.GuardedBack
import com.eter.undiamas.core.presentation.components.InterceptBack
import com.eter.undiamas.core.presentation.components.SectionCard
import com.eter.undiamas.core.presentation.components.SectionHeaderLarge
import com.eter.undiamas.core.presentation.components.pressable
import com.eter.undiamas.core.presentation.rememberNow
import com.eter.undiamas.core.presentation.theme.AppIcons
import com.eter.undiamas.core.presentation.theme.PrimaryVioletBrush
import com.eter.undiamas.core.presentation.theme.PrimaryVioletStart
import com.eter.undiamas.core.presentation.theme.RiskGreen
import com.eter.undiamas.features.capsulas.domain.TimeCapsule
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CapsulasScreen(state: AppState) {
    val now by rememberNow(intervalMillis = 60_000)
    val today = now.toLocalDateTime(TimeZone.currentSystemDefault()).date
    val vault = state.capsuleVault

    var composing by remember { mutableStateOf(false) }
    var reading by remember { mutableStateOf<TimeCapsule?>(null) }

    val locked = vault.locked(state.capsules, today)
    val unlocked = vault.unlocked(state.capsules, today)

    // El visor a pantalla completa se cierra con atrás en vez de salir de la sección.
    reading?.let { capsule ->
        InterceptBack { reading = null }
        CapsuleReader(capsule) { reading = null }
        return
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        SectionHeaderLarge(AppIcons.Capsula, "Cápsulas del tiempo")
        Text(
            "Escríbete hoy, que estás bien, para el día en que te cueste recordarlo.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Button(
            onClick = { composing = true },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryVioletStart),
        ) {
            Icon(AppIcons.Capsula, contentDescription = null, modifier = Modifier.size(20.dp))
            Text("  Escribir una cápsula")
        }

        if (state.capsules.isEmpty()) {
            EmptyCapsules()
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(unlocked, key = { "u${it.id}" }) { capsule ->
                CapsuleTile(capsule, daysLeft = 0, onClick = { reading = capsule })
            }
            items(locked, key = { "l${it.id}" }) { capsule ->
                CapsuleTile(
                    capsule,
                    daysLeft = vault.daysUntilUnlock(capsule, today),
                    onClick = { state.notify("Esta cápsula todavía no se puede abrir") },
                )
            }
        }
    }

    if (composing) {
        CapsuleComposer(
            today = today,
            onDismiss = { composing = false },
            onSave = { title, message, unlockOn ->
                state.addCapsule(title, message, today, unlockOn)
                state.notify("Cápsula guardada")
                composing = false
            },
        )
    }
}

@Composable
private fun CapsuleTile(capsule: TimeCapsule, daysLeft: Int, onClick: () -> Unit) {
    val open = daysLeft == 0
    Surface(
        shape = MaterialTheme.shapes.large,
        color = if (open) RiskGreen.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth().pressable(onClick),
    ) {
        Column(
            modifier = Modifier.heightIn(min = 130.dp).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                if (open) AppIcons.Desbloqueado else AppIcons.Bloqueado,
                contentDescription = null,
                tint = if (open) RiskGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp),
            )
            Text(
                capsule.title,
                style = MaterialTheme.typography.titleMedium,
                color = if (open) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                if (open) "Lista para abrir" else "Faltan $daysLeft días",
                style = MaterialTheme.typography.labelMedium,
                color = if (open) RiskGreen else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun EmptyCapsules() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            AppIcons.Capsula,
            contentDescription = null,
            tint = PrimaryVioletStart,
            modifier = Modifier.size(48.dp),
        )
        Text("Todavía no te has escrito", style = MaterialTheme.typography.titleMedium)
        Text(
            "La primera cápsula es la que más vale: escríbela mientras te sientes fuerte.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun CapsuleReader(capsule: TimeCapsule, onClose: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        GradientCard(brush = PrimaryVioletBrush) {
            Text("CÁPSULA ABIERTA", style = MaterialTheme.typography.labelMedium)
            Text(capsule.title, style = MaterialTheme.typography.headlineSmall)
            Text("Escrita el ${capsule.createdOn}", style = MaterialTheme.typography.labelMedium)
        }
        SectionCard {
            Text(capsule.message, style = MaterialTheme.typography.bodyLarge)
        }
        OutlinedButton(onClick = onClose, modifier = Modifier.fillMaxWidth()) {
            Text("Cerrar")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CapsuleComposer(
    today: LocalDate,
    onDismiss: () -> Unit,
    onSave: (String, String, LocalDate) -> Unit,
) {
    var title by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    // Por defecto se abre en 30 días: el primer hito grande de la app.
    var unlockOn by remember { mutableStateOf(LocalDate.fromEpochDays(today.toEpochDays() + 30)) }
    var pickingDate by remember { mutableStateOf(false) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val hasContent = title.isNotBlank() || message.isNotBlank()

    // Si ya escribió algo, atrás pide confirmación antes de descartar.
    GuardedBack(
        enabled = hasContent,
        title = "¿Salir sin guardar?",
        message = "Perderás lo que llevas escrito en esta cápsula.",
        confirmLabel = "Descartar",
        onConfirmExit = onDismiss,
    )

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Nueva cápsula", style = MaterialTheme.typography.headlineSmall)

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Título") },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                singleLine = true,
            )
            OutlinedTextField(
                value = message,
                onValueChange = { message = it },
                label = { Text("¿Qué quieres recordarte?") },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                minLines = 5,
            )

            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth().pressable({ pickingDate = true }),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(AppIcons.Calendario, contentDescription = null, modifier = Modifier.size(20.dp))
                    Column {
                        Text("Se abre el", style = MaterialTheme.typography.labelMedium)
                        Text("$unlockOn", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }

            Button(
                enabled = title.isNotBlank() && message.isNotBlank(),
                onClick = { onSave(title, message, unlockOn) },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Sellar cápsula") }

            Box(modifier = Modifier.padding(bottom = 16.dp))
        }
    }

    if (pickingDate) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = unlockOn.toEpochDays() * 86_400_000L,
        )
        DatePickerDialog(
            onDismissRequest = { pickingDate = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        val picked = LocalDate.fromEpochDays((millis / 86_400_000L).toInt())
                        // Una cápsula que se abre hoy o antes no tendría sentido.
                        unlockOn = if (picked > today) picked else today
                    }
                    pickingDate = false
                }) { Text("Elegir") }
            },
            dismissButton = { TextButton(onClick = { pickingDate = false }) { Text("Cancelar") } },
        ) {
            DatePicker(state = pickerState)
        }
    }
}
