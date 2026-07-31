package com.eter.undiamas.features.emergencia.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.eter.undiamas.core.presentation.AppState
import com.eter.undiamas.core.presentation.Navigator
import com.eter.undiamas.core.presentation.Screen
import com.eter.undiamas.core.presentation.components.BreathingCircle
import com.eter.undiamas.core.presentation.components.BubblePopGame
import com.eter.undiamas.core.presentation.components.SectionCard
import com.eter.undiamas.core.presentation.components.pressable
import com.eter.undiamas.core.presentation.rememberPhoneDialer
import com.eter.undiamas.core.presentation.theme.AccentAsistente
import com.eter.undiamas.core.presentation.theme.EmergencyCoralEnd
import com.eter.undiamas.core.presentation.theme.EmergencyCoralStart
import com.eter.undiamas.core.presentation.theme.PrimaryVioletStart
import com.eter.undiamas.core.presentation.theme.RiskGreen
import com.eter.undiamas.core.presentation.theme.AppIcons
import com.eter.undiamas.core.presentation.components.SectionHeader
import com.eter.undiamas.core.presentation.icon
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.Icon

private data class GroundingStep(val icon: ImageVector, val text: String)

private val groundingSteps = listOf(
    GroundingStep(AppIcons.Ver, "Nombra 5 cosas que puedas ver"),
    GroundingStep(AppIcons.Tocar, "Nombra 4 cosas que puedas tocar"),
    GroundingStep(AppIcons.Oir, "Nombra 3 cosas que puedas oír"),
    GroundingStep(AppIcons.Oler, "Nombra 2 cosas que puedas oler"),
    GroundingStep(AppIcons.Saborear, "Nombra 1 cosa que puedas saborear"),
)

private enum class Tool { RESPIRACION, ANCLAJE, BURBUJAS }

@Composable
fun EmergenciaScreen(state: AppState, navigator: Navigator) {
    val contact = state.profile.trustedContact
    val dial = rememberPhoneDialer()
    var tool by remember { mutableStateOf(Tool.RESPIRACION) }
    var groundingStep by remember { mutableStateOf(0) }
    var popped by remember { mutableStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        EmergencyCoralEnd.copy(alpha = 0.35f),
                        MaterialTheme.colorScheme.background,
                    ),
                ),
            )
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Estás a salvo", style = MaterialTheme.typography.headlineMedium)
        Text("Esto también pasará.", style = MaterialTheme.typography.bodyLarge)

        // Acceso al búnker de 15 minutos: la herramienta más fuerte cuando el impulso es agudo.
        Button(
            onClick = { navigator.goTo(Screen.UrgeSurfing) },
            colors = ButtonDefaults.buttonColors(
                containerColor = EmergencyCoralStart,
                contentColor = Color.White,
            ),
            modifier = Modifier.fillMaxWidth().height(64.dp),
        ) {
            Icon(AppIcons.Escudo, contentDescription = null, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(10.dp))
            Text("Sostener el impulso · 15 min", style = MaterialTheme.typography.titleMedium)
        }

        HelplineButtons(onDial = dial)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            ToolChip(AppIcons.Respiracion, "Respirar", tool == Tool.RESPIRACION, Modifier.weight(1f)) { tool = Tool.RESPIRACION }
            ToolChip(AppIcons.Calma, "Anclaje", tool == Tool.ANCLAJE, Modifier.weight(1f)) { tool = Tool.ANCLAJE }
            ToolChip(AppIcons.Distraccion, "Burbujas", tool == Tool.BURBUJAS, Modifier.weight(1f)) { tool = Tool.BURBUJAS }
        }

        when (tool) {
            Tool.RESPIRACION -> {
                Text(
                    "Sigue el círculo con tu respiración: inhala 4, sostén 4, exhala 6.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    BreathingCircle(colors = listOf(EmergencyCoralStart, PrimaryVioletStart))
                }
            }

            Tool.ANCLAJE -> {
                SectionCard {
                    SectionHeader(AppIcons.Calma, "Técnica 5-4-3-2-1", EmergencyCoralStart)
                    Text(
                        "Aterriza tus sentidos, uno a la vez. No hay prisa.",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            groundingSteps[groundingStep].icon,
                            contentDescription = null,
                            tint = EmergencyCoralStart,
                            modifier = Modifier.size(32.dp),
                        )
                        Text(groundingSteps[groundingStep].text, style = MaterialTheme.typography.titleLarge)
                    }
                    LinearProgressIndicator(
                        progress = { (groundingStep + 1f) / groundingSteps.size },
                        modifier = Modifier.fillMaxWidth().height(8.dp),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        if (groundingStep > 0) {
                            OutlinedButton(
                                onClick = { groundingStep -= 1 },
                                modifier = Modifier.weight(1f),
                            ) { Text("Anterior") }
                        }
                        Button(
                            onClick = {
                                if (groundingStep < groundingSteps.lastIndex) {
                                    groundingStep += 1
                                } else {
                                    groundingStep = 0
                                    state.notify("Lo lograste. Volviste a tu cuerpo.")
                                }
                            },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(if (groundingStep < groundingSteps.lastIndex) "Listo, siguiente" else "Terminar")
                        }
                    }
                }
            }

            Tool.BURBUJAS -> {
                SectionCard {
                    SectionHeader(AppIcons.Distraccion, "Revienta las burbujas", EmergencyCoralStart)
                    Text(
                        "Un minuto de foco en otra cosa basta para que el impulso baje.",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    BubblePopGame(
                        colors = listOf(EmergencyCoralStart, PrimaryVioletStart, AccentAsistente, RiskGreen),
                        modifier = Modifier.fillMaxWidth().height(280.dp),
                        onPop = { popped = it },
                    )
                    Text("Burbujas reventadas: $popped", style = MaterialTheme.typography.labelMedium)
                }
            }
        }

        if (contact != null) {
            SectionCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier.size(52.dp).background(EmergencyCoralStart, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            contact.name.take(1).uppercase(),
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            "TU CONTACTO DE CONFIANZA",
                            style = MaterialTheme.typography.labelMedium,
                            color = EmergencyCoralStart,
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(contact.role.icon, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text(contact.name, style = MaterialTheme.typography.titleMedium)
                        }
                        Text(contact.phone, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            Button(
                onClick = { dial(contact.phone) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = EmergencyCoralStart,
                    contentColor = Color.White,
                ),
                modifier = Modifier.fillMaxWidth().height(60.dp),
            ) {
                Icon(AppIcons.Llamar, contentDescription = null, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(10.dp))
                Text("Llamar a ${contact.name}", style = MaterialTheme.typography.titleMedium)
            }
            Text(
                "Se abrirá tu app de teléfono con el número listo; tú decides cuándo marcar.",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            state.profile.supportNetwork.forEach { extra ->
                OutlinedButton(onClick = { dial(extra.phone) }, modifier = Modifier.fillMaxWidth()) {
                    Icon(extra.role.icon, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("Llamar a ${extra.name}")
                }
            }
        } else {
            Text(
                "Aún no registras un contacto de confianza. Agrégalo en tu perfil para tenerlo aquí a la mano.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                "Este momento pasará. Ya has llegado hasta aquí antes.",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(20.dp),
            )
        }
    }
}

@Composable
private fun ToolChip(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = if (selected) EmergencyCoralStart.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier.pressable(onClick),
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Text(label, style = MaterialTheme.typography.labelMedium)
        }
    }
}
