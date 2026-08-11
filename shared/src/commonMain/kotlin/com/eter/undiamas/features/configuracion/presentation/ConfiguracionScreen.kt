package com.eter.undiamas.features.configuracion.presentation

import androidx.compose.foundation.BorderStroke
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
import com.eter.undiamas.core.presentation.ThemeMode
import com.eter.undiamas.core.presentation.closeApp
import androidx.compose.material3.OutlinedTextField
import com.eter.undiamas.core.data.api.ApiConfig
import com.eter.undiamas.core.data.api.isValidBaseUrl
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
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

@Composable
fun ConfiguracionScreen(state: AppState) {
    val settings = state.settings
    var showLogout by remember { mutableStateOf(false) }
    var showPurge by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

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

            // Explica el trato antes de pedir el permiso: cuántos avisos y por qué cambian.
            Text(
                "Cuántas veces te escribimos depende de tu semáforo: en verde un par al " +
                    "día, en amarillo unos cuantos, y en rojo estamos encima. Hablamos de " +
                    "tu racha, tus motivos y lo que llevas ahorrado.",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = { state.activarAvisos() },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(AppIcons.Notificaciones, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Activar avisos")
                }
                OutlinedButton(
                    onClick = { state.avisoDePrueba() },
                    modifier = Modifier.weight(1f),
                ) { Text("Ver un ejemplo") }
            }
        }

        SectionCard {
            SectionHeader(AppIcons.TemaOscuro, "Apariencia", RiskYellow)
            Text(
                "Por defecto la app sigue lo que tengas puesto en el teléfono.",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ThemeMode.entries.forEach { modo ->
                    OpcionDeTema(
                        modo = modo,
                        seleccionado = settings.themeMode == modo,
                        modifier = Modifier.weight(1f),
                        onClick = { state.updateSettings { it.copy(themeMode = modo) } },
                    )
                }
            }
        }

        SectionCard {
            SectionHeader(AppIcons.Perfil, "Cuenta", AccentDiario)
            Text(
                state.session?.let { "Sesión activa: ${it.email}" }
                    ?: "Sin sesión activa.",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "Tu historial vive en el servidor, así que sigue contigo si cambias de " +
                    "teléfono. Cerrar sesión aquí la cierra en todos tus dispositivos.",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // Solo en debug, para probar en un teléfono físico contra el servidor de otra
        // máquina de la wifi. En la app publicada no aparece: dejar que alguien reapunte la
        // app a otro servidor es dejar que le roben la sesión y el historial.
        if (ApiConfig.permiteCambiarServidor) SectionCard {
            SectionHeader(AppIcons.Escudo, "Servidor", RiskYellow)
            var serverUrl by remember { mutableStateOf(ApiConfig.baseUrl) }
            OutlinedTextField(
                value = serverUrl,
                onValueChange = { serverUrl = it.trim() },
                label = { Text("Dirección del backend") },
                isError = serverUrl.isNotBlank() && !isValidBaseUrl(serverUrl),
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                singleLine = true,
            )
            Button(
                onClick = {
                    state.useServerUrl(serverUrl)
                    state.notify("Conectando con $serverUrl")
                },
                enabled = isValidBaseUrl(serverUrl) && serverUrl != ApiConfig.baseUrl,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Usar esta dirección") }
        }

        SectionCard {
            SectionHeader(AppIcons.Bloqueado, "Privacidad del diario", AccentDiario)
            SettingRow(AppIcons.Bloqueado, "Bloquear el diario", AccentDiario, settings.diaryLocked) { checked ->
                state.updateSettings { it.copy(diaryLocked = checked) }
            }
        }

        OutlinedButton(onClick = { showLogout = true }, modifier = Modifier.fillMaxWidth()) {
            Icon(AppIcons.CerrarSesion, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Text("Cerrar sesión")
        }

        // Zona de Seguridad: enmarcada y al final, para que nada aquí se toque por accidente.
        Surface(
            shape = MaterialTheme.shapes.large,
            color = RiskRed.copy(alpha = 0.07f),
            border = BorderStroke(1.5.dp, RiskRed.copy(alpha = 0.55f)),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                SectionHeader(AppIcons.Escudo, "Zona de Seguridad", RiskRed)
                Text(
                    "Para cuando necesitas que nadie más vea lo que hay aquí.",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                SettingRow(AppIcons.Camuflaje, "Modo camuflaje", RiskYellow, settings.stealthMode) { checked ->
                    state.updateSettings { it.copy(stealthMode = checked) }
                    state.notify(
                        if (checked) "Modo camuflaje activado" else "Modo camuflaje desactivado",
                    )
                }
                Text(
                    "Con el camuflaje activo, la app oculta sus textos sensibles y se muestra neutra. " +
                        "Cambiar el icono y el nombre en el lanzador del teléfono requiere configuración " +
                        "adicional de Android que todavía no está integrada.",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Button(
                    onClick = { showPurge = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = RiskRed),
                ) {
                    Icon(AppIcons.Borrar, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("Borrado de emergencia")
                }
                Text(
                    "Borra tu diario y cierra la sesión en todos tus dispositivos. No se " +
                        "puede deshacer.",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
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
                        state.signOut()
                        state.notify("Sesión cerrada")
                    },
                ) { Text("Cerrar sesión") }
            },
            dismissButton = { TextButton(onClick = { showLogout = false }) { Text("Cancelar") } },
        )
    }

    if (showPurge) {
        AlertDialog(
            onDismissRequest = { showPurge = false },
            title = { Text("¿Borrar y cerrar sesión?") },
            text = {
                Text(
                    "Se elimina tu diario del servidor y todo lo guardado en este teléfono, " +
                        "y se cierra la sesión. Tus check-ins y tu historial de recaídas " +
                        "siguen en el servidor: eliminarlos hay que pedirlo aparte.",
                )
            },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = RiskRed),
                    onClick = {
                        showPurge = false
                        scope.launch { state.clearPersisted() }
                        state.purgeAllData()
                    },
                ) { Text("Sí, borrar todo") }
            },
            dismissButton = { TextButton(onClick = { showPurge = false }) { Text("Cancelar") } },
        )
    }
}

/**
 * Una de las tres opciones de tema.
 *
 * Se distingue por borde y por color de texto, no solo por relleno: en una pantalla a
 * plena luz —que es cuando alguien busca justo este ajuste— el relleno solo no basta.
 */
@Composable
private fun OpcionDeTema(
    modo: ThemeMode,
    seleccionado: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val icono = when (modo) {
        ThemeMode.SISTEMA -> AppIcons.Configuracion
        ThemeMode.CLARO -> AppIcons.Dia
        ThemeMode.OSCURO -> AppIcons.TemaOscuro
    }
    val etiqueta = when (modo) {
        ThemeMode.SISTEMA -> "Sistema"
        ThemeMode.CLARO -> "Claro"
        ThemeMode.OSCURO -> "Oscuro"
    }

    Surface(
        shape = MaterialTheme.shapes.medium,
        color = if (seleccionado) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
        } else {
            MaterialTheme.colorScheme.surface
        },
        border = BorderStroke(
            width = if (seleccionado) 1.5.dp else 1.dp,
            color = if (seleccionado) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.outline
            },
        ),
        modifier = modifier.pressable(onClick),
    ) {
        Column(
            modifier = Modifier.padding(vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                icono,
                contentDescription = null,
                tint = if (seleccionado) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.size(22.dp),
            )
            Text(
                etiqueta,
                style = MaterialTheme.typography.labelLarge,
                color = if (seleccionado) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
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
            "Los avisos llegan del servidor mientras la app está abierta. Si estaba " +
                "cerrada, los verás al volver a entrar.",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
