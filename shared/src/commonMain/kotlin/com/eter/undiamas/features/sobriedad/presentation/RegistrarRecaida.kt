package com.eter.undiamas.features.sobriedad.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import com.eter.undiamas.core.domain.model.Trigger
import com.eter.undiamas.core.presentation.AppState
import com.eter.undiamas.core.presentation.components.pressable
import com.eter.undiamas.core.presentation.icon
import com.eter.undiamas.core.presentation.theme.AppIcons
import com.eter.undiamas.core.presentation.theme.InkMuted
import com.eter.undiamas.core.presentation.theme.RiskYellow

/**
 * Acceso a registrar una recaída, para poner en la pantalla principal.
 *
 * Dos cosas tiran en direcciones opuestas y hay que sostener las dos:
 *
 * - **Tiene que estar a la vista.** Esconderla en un submenú le dice a la persona que
 *   recaer es algo que se oculta. Quien acaba de recaer necesita poder registrarlo sin
 *   buscar, en el momento en que menos ganas tiene de navegar por una app.
 * - **No puede pulsarse sin querer.** Reinicia el contador, y perder una racha por un
 *   roce sería cruel. Por eso es un enlace discreto y no un botón grande, y por eso
 *   siempre pasa por confirmación.
 *
 * El resultado: visible, tranquilo, y a dos toques.
 */
@Composable
fun EnlaceDeRecaida(state: AppState, modifier: Modifier = Modifier) {
    var abierto by remember { mutableStateOf(false) }

    Row(
        modifier = modifier.fillMaxWidth().pressable({ abierto = true }).padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            AppIcons.Refrescar,
            contentDescription = null,
            tint = InkMuted,
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            "Hoy no pude · Registrar una recaída",
            style = MaterialTheme.typography.labelLarge,
            color = InkMuted,
        )
    }

    if (abierto) {
        DialogoDeRecaida(state = state, onCerrar = { abierto = false })
    }
}

/**
 * Confirmación y detonantes, en un solo paso.
 *
 * Antes eran dos diálogos encadenados. Uno solo es mejor aquí: encadenar pantallas a alguien
 * que acaba de recaer se siente como un interrogatorio, y los detonantes son opcionales.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DialogoDeRecaida(state: AppState, onCerrar: () -> Unit) {
    var detonantes by remember { mutableStateOf(emptySet<Trigger>()) }

    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text("Registrar una recaída") },
        text = {
            androidx.compose.foundation.layout.Column(
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(
                    "Caer es parte del proceso. Esto no borra tu esfuerzo ni tu récord: " +
                        "tu mejor racha se queda contigo.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    "Si quieres, marca qué crees que lo detonó. Reconocerlo no es culparte, " +
                        "es información para tu plan.",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Trigger.entries.forEach { detonante ->
                        val activo = detonante in detonantes
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (activo) {
                                RiskYellow.copy(alpha = 0.18f)
                            } else {
                                MaterialTheme.colorScheme.surface
                            },
                            border = BorderStroke(
                                width = if (activo) 1.5.dp else 1.dp,
                                color = if (activo) RiskYellow else MaterialTheme.colorScheme.outline,
                            ),
                            modifier = Modifier.pressable({
                                detonantes = if (activo) detonantes - detonante else detonantes + detonante
                            }),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    detonante.icon,
                                    contentDescription = null,
                                    tint = if (activo) RiskYellow else InkMuted,
                                    modifier = Modifier.size(15.dp),
                                )
                                Text(
                                    detonante.label,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (activo) {
                                        MaterialTheme.colorScheme.onSurface
                                    } else {
                                        InkMuted
                                    },
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                // Ámbar y no rojo: el rojo de esta app es el del semáforo y el de
                // emergencia. Registrar una recaída no es una emergencia ni un error del
                // sistema, y pintarlo de rojo lo convertiría en una alarma.
                colors = ButtonDefaults.buttonColors(containerColor = RiskYellow),
                onClick = {
                    state.registerRelapse(triggers = detonantes.map { it.name })
                    state.notify("Registrado. Tu récord se conserva. Empezamos de nuevo.")
                    onCerrar()
                },
            ) { Text("Registrar") }
        },
        dismissButton = { TextButton(onClick = onCerrar) { Text("Cancelar") } },
    )
}
