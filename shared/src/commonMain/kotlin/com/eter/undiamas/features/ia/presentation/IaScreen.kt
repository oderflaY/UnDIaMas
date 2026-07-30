package com.eter.undiamas.features.ia.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.eter.undiamas.core.domain.model.AiMessage
import com.eter.undiamas.core.domain.model.AiMessageRole
import com.eter.undiamas.core.domain.model.RiskLevel
import com.eter.undiamas.core.presentation.AppState
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

@Composable
fun IaScreen(state: AppState) {
    var draft by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val lastRiskLevel = state.checkIns.firstOrNull()?.riskLevel

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Asistente", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

        if (state.aiMessages.isEmpty()) {
            Text(
                "Cuéntame cómo te sientes hoy. Estoy aquí para escucharte.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        LazyColumn(modifier = Modifier.fillMaxHeight(0.8f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(state.aiMessages) { message -> ChatBubble(message) }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Cuéntame cómo te sientes") },
            )
            Button(
                enabled = draft.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    val prompt = draft
                    val userMessage = AiMessage(
                        id = "user-${state.aiMessages.size}",
                        userId = state.profile.userId,
                        role = AiMessageRole.USUARIO,
                        content = prompt,
                        sentAt = Clock.System.now(),
                    )
                    state.aiMessages.add(0, userMessage)
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
            ) { Text("Enviar") }
        }
    }
}

@Composable
private fun ChatBubble(message: AiMessage) {
    val isUser = message.role == AiMessageRole.USUARIO
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (isUser) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.secondaryContainer
                },
            ),
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    if (isUser) "Tú" else "Asistente",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(message.content, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
