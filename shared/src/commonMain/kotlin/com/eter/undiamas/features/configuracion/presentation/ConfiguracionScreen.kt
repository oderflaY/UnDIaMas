package com.eter.undiamas.features.configuracion.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import com.eter.undiamas.core.presentation.components.SectionCard
import com.eter.undiamas.core.presentation.components.pressable
import com.eter.undiamas.core.presentation.theme.AccentDiario
import com.eter.undiamas.core.presentation.theme.RiskGreen
import com.eter.undiamas.core.presentation.theme.RiskRed
import com.eter.undiamas.core.presentation.theme.RiskYellow
import com.eter.undiamas.core.presentation.theme.AppIcons
import com.eter.undiamas.core.presentation.components.SectionHeaderLarge
import com.eter.undiamas.core.presentation.components.SectionHeader
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.Icon

@Composable
fun ConfiguracionScreen(state: AppState) {
    val settings = state.settings
    var showLogout by remember { mutableStateOf(false) }
    var showPurge by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        SectionHeaderLarge(AppIcons.Configuracion, "Configuración")

        SectionCard {
            SectionHeader(AppIcons.Notificaciones, "Notificaciones", RiskGreen)
            SettingRow(AppIcons.Notificaciones, "Recordatorios diarios", RiskGreen, settings.dailyReminders) { checked ->
                state.updateSettings { it.copy(dailyReminders = checked) }
            }
            if (settings.dailyReminders) {
                HourPicker(settings.reminderHour) { hour ->
                    state.updateSettings { it.copy(reminderHour = hour) }
                }
            }
            SettingRow(AppIcons.Resumen, "Resumen semanal", AccentDiario, settings.weeklySummary) { checked ->
                state.updateSettings { it.copy(weeklySummary = checked) }
            }
        }

        SectionCard {
            SectionHeader(AppIcons.TemaOscuro, "Apariencia", RiskYellow)
            SettingRow(AppIcons.TemaOscuro, "Tema oscuro", RiskYellow, settings.darkTheme) { checked ->
                state.updateSettings { it.copy(darkTheme = checked) }
            }
        }

        SectionCard {
            SectionHeader(AppIcons.Escudo, "Seguridad y datos", AccentDiario)
            SettingRow(AppIcons.Bloqueado, "Bloquear el diario", AccentDiario, settings.diaryLocked) { checked ->
                state.updateSettings { it.copy(diaryLocked = checked) }
            }
            SettingRow(AppIcons.Camuflaje, "Modo camuflaje", RiskYellow, settings.stealthMode) { checked ->
                state.updateSettings { it.copy(stealthMode = checked) }
                state.notify(
                    if (checked) {
                        "Modo camuflaje activado dentro de la app"
                    } else {
                        "Modo camuflaje desactivado"
                    },
                )
            }
            Text(
                "El modo camuflaje oculta los textos sensibles dentro de la app. Cambiar el icono " +
                    "del lanzador requiere configuración adicional de Android que aún no está integrada.",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        OutlinedButton(onClick = { showLogout = true }, modifier = Modifier.fillMaxWidth()) {
            Icon(AppIcons.CerrarSesion, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Text("Cerrar sesión")
        }

        Button(
            onClick = { showPurge = true },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = RiskRed),
        ) {
            Icon(AppIcons.Borrar, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Text("Borrar todos mis datos")
        }
    }

    if (showLogout) {
        AlertDialog(
            onDismissRequest = { showLogout = false },
            title = { Text("¿Cerrar sesión?") },
            text = { Text("Tus datos siguen en este dispositivo. Podrás volver a entrar cuando quieras.") },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = RiskRed),
                    onClick = {
                        showLogout = false
                        state.notify("Sesión cerrada (pendiente de Firebase Auth)")
                    },
                ) { Text("Cerrar sesión") }
            },
            dismissButton = { TextButton(onClick = { showLogout = false }) { Text("Cancelar") } },
        )
    }

    if (showPurge) {
        AlertDialog(
            onDismissRequest = { showPurge = false },
            title = { Text("¿Borrar todo de forma permanente?") },
            text = {
                Text(
                    "Se eliminarán tu perfil, tus check-ins, tu diario y tus conversaciones. " +
                        "Esta acción no se puede deshacer.",
                )
            },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = RiskRed),
                    onClick = {
                        showPurge = false
                        state.purgeAllData()
                    },
                ) { Text("Sí, borrar todo") }
            },
            dismissButton = { TextButton(onClick = { showPurge = false }) { Text("Cancelar") } },
        )
    }
}

@Composable
private fun SettingRow(
    icon: ImageVector,
    label: String,
    accent: Color,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(22.dp))
            Text(label, style = MaterialTheme.typography.bodyLarge)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedTrackColor = accent),
        )
    }
}

/** Selector de hora en bloques de tres horas, suficiente para un recordatorio diario. */
@Composable
private fun HourPicker(selected: Int, onSelect: (Int) -> Unit) {
    val options = listOf(6, 9, 12, 15, 18, 21)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            "Hora del recordatorio",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            options.forEach { hour ->
                val isSelected = hour == selected
                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (isSelected) RiskGreen.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surface,
                    modifier = Modifier.weight(1f).pressable({ onSelect(hour) }),
                ) {
                    Text(
                        "${if (hour < 10) "0$hour" else "$hour"}:00",
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(vertical = 10.dp),
                    )
                }
            }
        }
        Text(
            "La notificación real llega con Firebase Cloud Messaging en la Fase 3.",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
