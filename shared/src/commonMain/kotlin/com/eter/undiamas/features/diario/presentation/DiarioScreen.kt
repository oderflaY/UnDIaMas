package com.eter.undiamas.features.diario.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.IntrinsicSize
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.eter.undiamas.core.presentation.AppState
import com.eter.undiamas.core.presentation.theme.AccentAhorro
import com.eter.undiamas.core.presentation.theme.AccentAsistente
import com.eter.undiamas.core.presentation.theme.AccentCheckIn
import com.eter.undiamas.core.presentation.theme.AccentDiario
import com.eter.undiamas.core.presentation.theme.AccentPerfil
import com.eter.undiamas.features.diario.domain.DiaryEntry
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

private val entryAccents = listOf(AccentDiario, AccentAsistente, AccentCheckIn, AccentAhorro, AccentPerfil)

@Composable
fun DiarioScreen(state: AppState) {
    var draft by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("📓 Diario", style = MaterialTheme.typography.headlineMedium)

        OutlinedTextField(
            value = draft,
            onValueChange = { draft = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("¿Cómo estuvo tu día?") },
            shape = MaterialTheme.shapes.medium,
        )
        Button(
            enabled = draft.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = AccentDiario),
            onClick = {
                state.addDiaryEntry(
                    DiaryEntry(id = "", userId = state.profile.userId, createdAt = Clock.System.now(), text = draft),
                )
                state.notify("Entrada guardada ✨")
                draft = ""
            },
        ) { Text("Guardar entrada") }

        if (state.diaryEntries.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text("🌱", style = MaterialTheme.typography.displaySmall)
                Text("Tu primera entrada te espera", style = MaterialTheme.typography.titleMedium, color = AccentDiario)
            }
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            itemsIndexed(state.diaryEntries) { index, entry ->
                val accent = entryAccents[index % entryAccents.size]
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
                            Text("${local.date}".uppercase(), style = MaterialTheme.typography.labelMedium, color = accent)
                            Text(entry.text, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}
