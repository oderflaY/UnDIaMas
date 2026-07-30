package com.eter.undiamas.features.ia.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.eter.undiamas.core.presentation.theme.AccentAsistente
import com.eter.undiamas.core.presentation.theme.Violet60
import com.eter.undiamas.core.presentation.theme.accentBrush
import kotlinx.coroutines.launch
import kotlin.time.Clock

@Composable
fun IaScreen(state: AppState) {
    var draft by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val lastRiskLevel = state.checkIns.firstOrNull()?.riskLevel

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(modifier = Modifier.weight(1f)) {
            if (state.aiMessages.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("💬", style = MaterialTheme.typography.displayMedium)
                    Text(
                        "Cuéntame cómo te sientes hoy",
                        style = MaterialTheme.typography.titleMedium,
                        color = AccentAsistente,
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
                colors = ButtonDefaults.buttonColors(containerColor = AccentAsistente),
                onClick = {
                    val prompt = draft
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
                        val response = state.aiConversationService.respond(
                            prompt = prompt,
                            riskLevel = lastRiskLevel ?: RiskLevel.VERDE,
                            history = state.aiMessages,
                        )
                        state.aiMessages.add(0, response)
                    }
                },
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
        if (isUser) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.82f)
                    .background(accentBrush(Violet60), shape)
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text("TÚ", style = MaterialTheme.typography.labelMedium, color = Color.White)
                Text(message.content, style = MaterialTheme.typography.bodyMedium, color = Color.White)
            }
        } else {
            Surface(
                shape = shape,
                color = AccentAsistente.copy(alpha = 0.13f),
                modifier = Modifier.fillMaxWidth(0.82f),
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("ASISTENTE", style = MaterialTheme.typography.labelMedium, color = AccentAsistente)
                    Text(message.content, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
