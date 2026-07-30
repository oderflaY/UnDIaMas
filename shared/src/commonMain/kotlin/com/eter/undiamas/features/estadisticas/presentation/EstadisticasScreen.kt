package com.eter.undiamas.features.estadisticas.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.eter.undiamas.core.domain.model.RiskLevel
import com.eter.undiamas.core.presentation.AppState
import com.eter.undiamas.core.presentation.color
import com.eter.undiamas.core.presentation.label

@Composable
fun EstadisticasScreen(state: AppState) {
    val total = state.checkIns.size
    val byLevel = RiskLevel.entries.associateWith { level -> state.checkIns.count { it.riskLevel == level } }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Estadísticas", style = MaterialTheme.typography.headlineSmall)
        Text("Check-ins registrados: $total", style = MaterialTheme.typography.bodyLarge)

        byLevel.forEach { (level, count) ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(level.label, color = level.color, style = MaterialTheme.typography.titleMedium)
                    Text("$count veces", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Entradas de diario", style = MaterialTheme.typography.titleMedium)
                Text("${state.diaryEntries.size}", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
