package com.eter.undiamas.features.diario.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.eter.undiamas.core.presentation.AppState
import com.eter.undiamas.features.diario.domain.DiaryEntry
import kotlinx.datetime.Clock

@Composable
fun DiarioScreen(state: AppState) {
    var draft by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Diario", style = MaterialTheme.typography.headlineSmall)

        OutlinedTextField(
            value = draft,
            onValueChange = { draft = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("¿Cómo estuvo tu día?") },
        )
        Button(
            enabled = draft.isNotBlank(),
            onClick = {
                state.addDiaryEntry(
                    DiaryEntry(id = "", userId = state.profile.userId, createdAt = Clock.System.now(), text = draft),
                )
                draft = ""
            },
        ) { Text("Guardar") }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(state.diaryEntries) { entry ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Text(entry.text, modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
