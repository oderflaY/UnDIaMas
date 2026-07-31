package com.eter.undiamas.features.ia.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.eter.undiamas.core.domain.model.AiMessage
import com.eter.undiamas.core.domain.model.AiMessageRole
import com.eter.undiamas.core.domain.model.RiskLevel
import com.eter.undiamas.core.presentation.AppState
import com.eter.undiamas.core.presentation.components.TypingIndicator
import com.eter.undiamas.core.presentation.components.pressable
import com.eter.undiamas.core.presentation.theme.AssistantBrush
import com.eter.undiamas.core.presentation.theme.AssistantMagentaStart
import com.eter.undiamas.core.presentation.theme.PrimaryVioletBrush
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Clock

private val quickReplies = listOf(
    "Tengo un impulso fuerte",
    "Necesito una distracción",
    "Ejercicio de gratitud",
    "Ensayar decir que NO",
)

@Composable
fun IaScreen(state: AppState) {
    var draft by remember { mutableStateOf("") }
    var isTyping by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val lastRiskLevel = state.checkIns.firstOrNull()?.riskLevel ?: RiskLevel.VERDE

    fun send(prompt: String) {
        if (prompt.isBlank()) return
        state.aiMessages.add(
            0,
            AiMessage(
                id = "user-${state.aiMessages.size}",
                userId = state.profile.userId,
                role = AiMessageRole.USUARIO,
                content = prompt,
                sentAt = Clock.System.now(),
            ),
        )
        draft = ""
        scope.launch {
            isTyping = true
            // Pausa breve para que el indicador de escritura sea perceptible.
            delay(700)
            val response = state.aiConversationService.respond(
                prompt = prompt,
                riskLevel = lastRiskLevel,
                history = state.aiMessages,
            )
            state.aiMessages.add(0, response)
            isTyping = false
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            quickReplies.forEach { reply ->
                Surface(
                    shape = RoundedCornerShape(50),
                    color = AssistantMagentaStart.copy(alpha = 0.16f),
                    modifier = Modifier.pressable({ send(reply) }),
                ) {
                    Text(
                        reply,
                        style = MaterialTheme.typography.labelMedium,
                        color = AssistantMagentaStart,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    )
                }
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            if (state.aiMessages.isEmpty() && !isTyping) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("💬", style = MaterialTheme.typography.displayMedium)
                    Text(
                        "Cuéntame cómo te sientes hoy",
                        style = MaterialTheme.typography.titleMedium,
                        color = AssistantMagentaStart,
                    )
                    Text(
                        "Estoy aquí para escucharte, sin juicios.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(
                    reverseLayout = true,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    if (isTyping) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
                                Surface(
                                    shape = MaterialTheme.shapes.large,
                                    color = AssistantMagentaStart.copy(alpha = 0.14f),
                                ) {
                                    TypingIndicator(color = AssistantMagentaStart)
                                }
                            }
                        }
                    }
                    items(state.aiMessages) { message -> ChatBubble(message) }
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Escribe aquí…") },
                shape = MaterialTheme.shapes.large,
            )
            Button(
                enabled = draft.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = AssistantMagentaStart),
                onClick = { send(draft) },
            ) { Text("➤") }
        }
    }
}

@Composable
private fun ChatBubble(message: AiMessage) {
    val isUser = message.role == AiMessageRole.USUARIO
    val shape = MaterialTheme.shapes.large

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.84f)
                .background(if (isUser) PrimaryVioletBrush else AssistantBrush, shape)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                if (isUser) "TÚ" else "🤖 ASISTENTE",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.85f),
            )
            Text(message.content, style = MaterialTheme.typography.bodyMedium, color = Color.White)
        }
    }
}
