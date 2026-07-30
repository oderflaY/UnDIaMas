package com.eter.undiamas.features.estadisticas.presentation

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.eter.undiamas.core.domain.model.RiskLevel
import com.eter.undiamas.core.presentation.AppState
import com.eter.undiamas.core.presentation.color
import com.eter.undiamas.core.presentation.components.GradientCard
import com.eter.undiamas.core.presentation.emoji
import com.eter.undiamas.core.presentation.formatStreak
import com.eter.undiamas.core.presentation.label
import com.eter.undiamas.core.presentation.theme.AccentDiario
import com.eter.undiamas.core.presentation.theme.AccentStats
import com.eter.undiamas.core.presentation.theme.StatsBrush
import kotlinx.datetime.Clock

@Composable
fun EstadisticasScreen(state: AppState) {
    val total = state.checkIns.size
    val byLevel = RiskLevel.entries.associateWith { level -> state.checkIns.count { it.riskLevel == level } }
    val max = (byLevel.values.maxOrNull() ?: 0).coerceAtLeast(1)
    val streak = state.sobrietyCounter.currentStreakSeconds(state.profile, Clock.System.now())

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("📊 Estadísticas", style = MaterialTheme.typography.headlineMedium)

        GradientCard(brush = StatsBrush) {
            Text("CHECK-INS REGISTRADOS", style = MaterialTheme.typography.labelMedium)
            Text("$total", style = MaterialTheme.typography.displayMedium)
            Text("Racha actual: ${formatStreak(streak)}", style = MaterialTheme.typography.bodyMedium)
        }

        Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceVariant) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Distribución del semáforo", style = MaterialTheme.typography.titleMedium)
                byLevel.forEach { (level, count) ->
                    RiskBar(
                        emoji = level.emoji,
                        label = level.label,
                        count = count,
                        fraction = count.toFloat() / max,
                        accent = level.color,
                    )
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            MiniStat("📓", "Entradas", "${state.diaryEntries.size}", AccentDiario, Modifier.weight(1f))
            MiniStat("💬", "Mensajes", "${state.aiMessages.size}", AccentStats, Modifier.weight(1f))
        }
    }
}

@Composable
private fun RiskBar(emoji: String, label: String, count: Int, fraction: Float, accent: Color) {
    val animated by animateFloatAsState(targetValue = fraction, animationSpec = tween(700))

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("$emoji  $label", style = MaterialTheme.typography.bodyMedium)
            Text("$count", style = MaterialTheme.typography.labelMedium, color = accent)
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .background(accent.copy(alpha = 0.15f), RoundedCornerShape(6.dp)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animated.coerceIn(0f, 1f))
                    .height(12.dp)
                    .background(
                        Brush.horizontalGradient(listOf(accent, accent.copy(alpha = 0.65f))),
                        RoundedCornerShape(6.dp),
                    ),
            )
        }
    }
}

@Composable
private fun MiniStat(emoji: String, label: String, value: String, accent: Color, modifier: Modifier = Modifier) {
    Surface(shape = MaterialTheme.shapes.medium, color = accent.copy(alpha = 0.13f), modifier = modifier) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(emoji, style = MaterialTheme.typography.titleLarge)
            Text(label.uppercase(), style = MaterialTheme.typography.labelMedium, color = accent)
            Text(value, style = MaterialTheme.typography.headlineSmall)
        }
    }
}
