package com.eter.undiamas.features.diario.presentation

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.eter.undiamas.core.presentation.AppState
import com.eter.undiamas.core.presentation.components.pressable
import com.eter.undiamas.core.presentation.theme.AccentAsistente
import com.eter.undiamas.core.presentation.theme.AccentCheckIn
import com.eter.undiamas.core.presentation.theme.AccentDiario
import com.eter.undiamas.core.presentation.theme.AccentPerfil
import com.eter.undiamas.core.presentation.theme.RiskGreen
import com.eter.undiamas.features.diario.domain.DiaryEntry
import com.eter.undiamas.features.diario.domain.countWords
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

private val entryAccents = listOf(RiskGreen, AccentDiario, AccentCheckIn, AccentPerfil, AccentAsistente)

@Composable
fun DiarioScreen(state: AppState) {
    var draft by remember { mutableStateOf("") }
    var query by remember { mutableStateOf("") }
    val locked = state.settings.diaryLocked

    val visible = remember(query, state.diaryEntries.size) {
        if (query.isBlank()) {
            state.diaryEntries.toList()
        } else {
            state.diaryEntries.filter { it.text.contains(query, ignoreCase = true) }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("📓 Diario", style = MaterialTheme.typography.headlineMedium)
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.pressable({
                    state.updateSettings { it.copy(diaryLocked = !it.diaryLocked) }
                    state.notify(if (locked) "Diario desbloqueado" else "Diario bloqueado")
                }),
            ) {
                Text(
                    if (locked) "🔒" else "🔓",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                )
            }
        }

        if (locked) {
            LockedNotice()
            return@Column
        }

        OutlinedTextField(
            value = draft,
            onValueChange = { draft = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("¿Cómo estuvo tu día?") },
            shape = MaterialTheme.shapes.medium,
            minLines = 3,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                "${countWords(draft)} palabras",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (draft.isNotBlank()) {
                val sentiment = state.sentimentAnalyzer.analyze(draft)
                Text(
                    "${sentiment.emoji} ${sentiment.label}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Button(
            enabled = draft.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = AccentDiario),
            onClick = {
                state.addDiaryEntry(
                    DiaryEntry(id = "", userId = state.profile.userId, createdAt = Clock.System.now(), text = draft),
                )
                state.notify("✅ Entrada guardada")
                draft = ""
            },
        ) { Text("Guardar entrada") }

        if (state.diaryEntries.isNotEmpty()) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("🔎 Buscar en tus entradas") },
                shape = MaterialTheme.shapes.medium,
                singleLine = true,
            )
        }

        if (state.diaryEntries.isEmpty()) {
            EmptyDiary()
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            itemsIndexed(visible) { index, entry ->
                val accent = entryAccents[index % entryAccents.size]
                val sentiment = state.sentimentAnalyzer.analyze(entry.text)
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = accent.copy(alpha = 0.10f),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    // height(IntrinsicSize.Min) acota la fila para que la franja de acento
                    // pueda usar fillMaxHeight dentro del LazyColumn (altura no acotada).
                    Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                        Box(
                            modifier = Modifier
                                .width(5.dp)
                                .fillMaxHeight()
                                .background(accent, RoundedCornerShape(topStart = 18.dp, bottomStart = 18.dp)),
                        )
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            val local = entry.createdAt.toLocalDateTime(TimeZone.currentSystemDefault())
                            Text(
                                "${local.date} · ${pad(local.hour)}:${pad(local.minute)} · ${sentiment.emoji}",
                                style = MaterialTheme.typography.labelMedium,
                                color = accent,
                            )
                            Text(entry.text, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}

private fun pad(value: Int) = if (value < 10) "0$value" else "$value"

@Composable
private fun EmptyDiary() {
    val sway by rememberInfiniteTransition(label = "seed").animateFloat(
        initialValue = -8f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(tween(2200), RepeatMode.Reverse),
        label = "sway",
    )
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            "🌱",
            style = MaterialTheme.typography.displaySmall,
            modifier = Modifier.graphicsLayer { rotationZ = sway },
        )
        Text("Tu diario está vacío", style = MaterialTheme.typography.titleMedium, color = AccentDiario)
        Text(
            "Empieza escribiendo una reflexión de hoy.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun LockedNotice() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("🔒", style = MaterialTheme.typography.displaySmall)
        Text("Diario bloqueado", style = MaterialTheme.typography.titleMedium)
        Text(
            "Toca el candado para volver a mostrar tus entradas.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
