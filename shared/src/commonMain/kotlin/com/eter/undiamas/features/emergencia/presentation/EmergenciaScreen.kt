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

private val groundingSteps = listOf(
    "👀 Nombra 5 cosas que puedas ver",
    "✋ Nombra 4 cosas que puedas tocar",
    "👂 Nombra 3 cosas que puedas oír",
    "👃 Nombra 2 cosas que puedas oler",
    "👅 Nombra 1 cosa que puedas saborear",
)

private enum class Tool { RESPIRACION, ANCLAJE, BURBUJAS }

@Composable
fun EmergenciaScreen(state: AppState) {
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

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            ToolChip("🫁 Respirar", tool == Tool.RESPIRACION, Modifier.weight(1f)) { tool = Tool.RESPIRACION }
            ToolChip("🧭 Anclaje", tool == Tool.ANCLAJE, Modifier.weight(1f)) { tool = Tool.ANCLAJE }
            ToolChip("🫧 Burbujas", tool == Tool.BURBUJAS, Modifier.weight(1f)) { tool = Tool.BURBUJAS }
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
                    Text("Técnica 5-4-3-2-1", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Aterriza tus sentidos, uno a la vez. No hay prisa.",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(groundingSteps[groundingStep], style = MaterialTheme.typography.headlineSmall)
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
                    Text("Revienta las burbujas", style = MaterialTheme.typography.titleMedium)
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
                        Text("${contact.role.emoji} ${contact.name}", style = MaterialTheme.typography.titleMedium)
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
                Text("📞  Llamar a ${contact.name}", style = MaterialTheme.typography.titleMedium)
            }
            Text(
                "Se abrirá tu app de teléfono con el número listo; tú decides cuándo marcar.",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            state.profile.supportNetwork.forEach { extra ->
                OutlinedButton(onClick = { dial(extra.phone) }, modifier = Modifier.fillMaxWidth()) {
                    Text("${extra.role.emoji}  Llamar a ${extra.name}")
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
private fun ToolChip(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(50),
        color = if (selected) EmergencyCoralStart.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier.pressable(onClick),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(vertical = 12.dp),
        )
    }
}
