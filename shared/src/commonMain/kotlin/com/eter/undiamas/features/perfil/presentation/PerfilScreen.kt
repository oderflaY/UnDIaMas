package com.eter.undiamas.features.perfil.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import com.eter.undiamas.core.domain.model.SupportRole
import com.eter.undiamas.core.domain.model.TrustedContact
import com.eter.undiamas.core.presentation.AppState
import com.eter.undiamas.core.presentation.Navigator
import com.eter.undiamas.core.presentation.Screen
import com.eter.undiamas.core.presentation.components.SectionCard
import com.eter.undiamas.core.presentation.components.pressable
import com.eter.undiamas.core.presentation.rememberNow
import com.eter.undiamas.core.presentation.streakDays
import com.eter.undiamas.core.presentation.theme.AccentPerfil
import com.eter.undiamas.core.presentation.theme.PrimaryVioletBrush
import com.eter.undiamas.core.presentation.theme.RiskGreen
import com.eter.undiamas.core.presentation.theme.SavingsGoldEnd
import com.eter.undiamas.core.presentation.theme.AppIcons
import com.eter.undiamas.core.presentation.components.SectionHeader
import com.eter.undiamas.core.presentation.icon
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.Icon

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PerfilScreen(state: AppState, navigator: Navigator) {
    val now by rememberNow(intervalMillis = 60_000)
    val days = streakDays(state.sobrietyCounter.currentStreakSeconds(state.profile, now))
    val reached = state.milestones.reached(days)
    val badge = reached.lastOrNull()

    var name by remember { mutableStateOf(state.profile.displayName) }
    var contactName by remember { mutableStateOf(state.profile.trustedContact?.name.orEmpty()) }
    var contactPhone by remember { mutableStateOf(state.profile.trustedContact?.phone.orEmpty()) }
    var contactRole by remember { mutableStateOf(state.profile.trustedContact?.role ?: SupportRole.FAMILIAR) }
    var personalWhy by remember { mutableStateOf(state.profile.personalWhy) }

    var extraName by remember { mutableStateOf("") }
    var extraPhone by remember { mutableStateOf("") }
    var extraRole by remember { mutableStateOf(SupportRole.PADRINO) }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier.size(104.dp).background(PrimaryVioletBrush, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                state.profile.displayName.take(1).uppercase(),
                style = MaterialTheme.typography.displaySmall,
                color = Color.White,
            )
        }
        Text(state.profile.displayName, style = MaterialTheme.typography.headlineSmall)
        Text(
            badge?.let { "${it.title} · Nivel ${reached.size}" } ?: "Empezando el camino",
            style = MaterialTheme.typography.labelMedium,
            color = AccentPerfil,
        )

        SectionCard {
            SectionHeader(AppIcons.Insignia, "Insignias desbloqueadas", SavingsGoldEnd)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                state.milestones.all.forEach { milestone ->
                    val unlocked = days >= milestone.days
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = if (unlocked) RiskGreen.copy(alpha = 0.26f) else MaterialTheme.colorScheme.surface,
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                if (unlocked) AppIcons.Insignia else AppIcons.Bloqueado,
                                contentDescription = null,
                                tint = if (unlocked) RiskGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp),
                            )
                            Text(
                                milestone.title,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (unlocked) {
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

        SectionCard {
            SectionHeader(AppIcons.PorQue, "Mi por qué", AccentPerfil)
            Text(
                "Escribe aquí lo que quieres recordar cuando la cosa se ponga difícil.",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = personalWhy,
                onValueChange = { personalWhy = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Por mi hija, por mi salud, por mí…") },
                shape = MaterialTheme.shapes.medium,
                minLines = 3,
            )
        }

        SectionCard {
            Text("DATOS PERSONALES", style = MaterialTheme.typography.labelMedium, color = AccentPerfil)
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Tu nombre") },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                singleLine = true,
            )
            OutlinedTextField(
                value = contactName,
                onValueChange = { contactName = it },
                label = { Text("Contacto de confianza") },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                singleLine = true,
            )
            OutlinedTextField(
                value = contactPhone,
                onValueChange = { contactPhone = it },
                label = { Text("Teléfono del contacto") },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                singleLine = true,
            )
            RolePicker(contactRole) { contactRole = it }

            Button(
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = AccentPerfil),
                onClick = {
                    state.updateProfile { profile ->
                        profile.copy(
                            displayName = name.ifBlank { profile.displayName },
                            personalWhy = personalWhy,
                            trustedContact = if (contactName.isBlank()) {
                                null
                            } else {
                                TrustedContact(contactName, contactPhone, contactRole)
                            },
                        )
                    }
                    state.notify("Perfil actualizado")
                },
            ) { Text("Guardar cambios") }
        }

        SectionCard {
            SectionHeader(AppIcons.Red, "Red de soporte", AccentPerfil)
            if (state.profile.supportNetwork.isEmpty()) {
                Text(
                    "Agrega a más personas de confianza: padrino, terapeuta o familiares.",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            state.profile.supportNetwork.forEach { contact ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(contact.role.icon, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text(contact.name, style = MaterialTheme.typography.bodyMedium)
                    }
                    Text(contact.phone, style = MaterialTheme.typography.labelMedium)
                }
            }

            OutlinedTextField(
                value = extraName,
                onValueChange = { extraName = it },
                label = { Text("Nombre") },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                singleLine = true,
            )
            OutlinedTextField(
                value = extraPhone,
                onValueChange = { extraPhone = it },
                label = { Text("Teléfono") },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                singleLine = true,
            )
            RolePicker(extraRole) { extraRole = it }
            OutlinedButton(
                enabled = extraName.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    state.updateProfile { profile ->
                        profile.copy(
                            supportNetwork = profile.supportNetwork + TrustedContact(extraName, extraPhone, extraRole),
                        )
                    }
                    state.notify("Contacto agregado a tu red")
                    extraName = ""
                    extraPhone = ""
                },
            ) { Text("Agregar a mi red") }
        }

        OutlinedButton(onClick = { navigator.goTo(Screen.Configuracion) }, modifier = Modifier.fillMaxWidth()) {
            Icon(AppIcons.Configuracion, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Text("Configuración")
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RolePicker(selected: SupportRole, onSelect: (SupportRole) -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        SupportRole.entries.forEach { role ->
            val isSelected = role == selected
            Surface(
                shape = RoundedCornerShape(50),
                color = if (isSelected) {
                    AccentPerfil.copy(alpha = 0.28f)
                } else {
                    MaterialTheme.colorScheme.surface
                },
                modifier = Modifier.pressable({ onSelect(role) }),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(role.icon, contentDescription = null, modifier = Modifier.size(16.dp))
                    Text(role.label, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}
