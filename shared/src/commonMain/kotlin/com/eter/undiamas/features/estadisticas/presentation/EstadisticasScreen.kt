package com.eter.undiamas.features.estadisticas.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.eter.undiamas.core.domain.model.RiskLevel
import com.eter.undiamas.core.presentation.AppState
import com.eter.undiamas.core.presentation.color
import com.eter.undiamas.core.presentation.label

@Composable
fun EstadisticasScreen(state: AppState) {
    val total = state.checkIns.size
    val byLevel = RiskLevel.entries.associateWith { level -> state.checkIns.count { it.riskLevel == level } }
    val max = (byLevel.values.maxOrNull() ?: 0).coerceAtLeast(1)

    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Estadísticas", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Check-ins registrados", style = MaterialTheme.typography.labelLarge)
                Text("$total", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Distribución del semáforo", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                byLevel.forEach { (level, count) ->
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("${level.label} · $count", style = MaterialTheme.typography.bodyMedium)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(count.toFloat() / max)
                                .height(10.dp)
                                .background(level.color, RoundedCornerShape(6.dp)),
                        )
                    }
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Entradas de diario", style = MaterialTheme.typography.labelLarge)
                Text("${state.diaryEntries.size}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
        }
    }
}
