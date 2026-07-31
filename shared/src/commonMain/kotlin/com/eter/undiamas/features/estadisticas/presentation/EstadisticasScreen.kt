package com.eter.undiamas.features.estadisticas.presentation

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.eter.undiamas.core.domain.model.RiskLevel
import com.eter.undiamas.core.presentation.AppState
import com.eter.undiamas.core.presentation.color
import com.eter.undiamas.core.presentation.components.CleanDaysCalendar
import com.eter.undiamas.core.presentation.components.GradientCard
import com.eter.undiamas.core.presentation.components.SectionCard
import com.eter.undiamas.core.presentation.emoji
import com.eter.undiamas.core.presentation.label
import com.eter.undiamas.core.presentation.rememberNow
import com.eter.undiamas.core.presentation.streakDays
import com.eter.undiamas.core.presentation.theme.AccentAsistente
import com.eter.undiamas.core.presentation.theme.AccentDiario
import com.eter.undiamas.core.presentation.theme.RiskRed
import com.eter.undiamas.core.presentation.theme.StatsBrush
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import kotlin.math.roundToInt

@Composable
fun EstadisticasScreen(state: AppState) {
    val now by rememberNow()
    val streak = state.sobrietyCounter.currentStreakSeconds(state.profile, now)

    val timeZone = TimeZone.currentSystemDefault()
    val today = now.toLocalDateTime(timeZone).date
    val levelByDay = state.checkInHistory.byDay(state.checkIns, timeZone)
    val registeredDays = state.checkInHistory.registeredDays(state.checkIns, timeZone)
    val cleanDays = state.checkInHistory.cleanDays(state.checkIns, timeZone)
    // Ventana de 28 días (4 semanas exactas) terminando hoy.
    val calendarDays = (27 downTo 0).map { back -> today.minus(back, DateTimeUnit.DAY) }

    val total = state.checkIns.size
    val byLevel = RiskLevel.entries.associateWith { level -> state.checkIns.count { it.riskLevel == level } }
    val riskiestHour = state.riskInsights.riskiestHour(state.checkIns, timeZone)
    val riskByHour = state.riskInsights.riskByHour(state.checkIns, timeZone)
    val urgesOvercome = state.riskInsights.urgesOvercome(state.checkIns)

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("📊 Estadísticas", style = MaterialTheme.typography.headlineMedium)

        GradientCard(brush = StatsBrush) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                HeroMetric("DÍAS LIMPIOS", "$cleanDays", Modifier.weight(1f))
                HeroMetric("CHECK-INS", "$total", Modifier.weight(1f))
                HeroMetric("RACHA", "${streakDays(streak)}", Modifier.weight(1f))
            }
        }

        SectionCard {
            Text("Tus últimas 4 semanas", style = MaterialTheme.typography.titleMedium)
            CleanDaysCalendar(days = calendarDays, levelByDay = levelByDay, today = today)
            Text(
                "Cada casilla es un día. Se colorea con el semáforo de tu check-in; " +
                    "punteada significa que ese día no registraste.",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        SectionCard {
            Text("Distribución del semáforo", style = MaterialTheme.typography.titleMedium)
            byLevel.forEach { (level, count) ->
                val percent = if (total == 0) 0 else (count * 100f / total).roundToInt()
                RiskBar(
                    emoji = level.emoji,
                    label = level.label,
                    percent = percent,
                    fraction = if (total == 0) 0f else count.toFloat() / total,
                    accent = level.color,
                )
            }
        }

        SectionCard {
            Text("Horas críticas", style = MaterialTheme.typography.titleMedium)
            Text(
                riskiestHour?.let { "Tu horario con mayor riesgo suele ser alrededor de las ${pad(it)}:00." }
                    ?: "Aún no hay suficientes check-ins en riesgo para detectar un patrón.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            HourHeatmap(riskByHour, riskiestHour)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            MiniStat("💪", "Impulsos superados", "$urgesOvercome", RiskRed, Modifier.weight(1f))
            MiniStat("📅", "Días con registro", "$registeredDays", AccentDiario, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            MiniStat("📓", "Entradas", "${state.diaryEntries.size}", AccentDiario, Modifier.weight(1f))
            MiniStat("💬", "Apoyos IA", "${state.aiMessages.size}", AccentAsistente, Modifier.weight(1f))
        }
    }
}

private fun pad(value: Int) = if (value < 10) "0$value" else "$value"

@Composable
private fun HeroMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Text(
            value,
            style = MaterialTheme.typography.headlineMedium,
            fontFamily = FontFamily.Monospace,
        )
    }
}

@Composable
private fun RiskBar(emoji: String, label: String, percent: Int, fraction: Float, accent: Color) {
    val animated by animateFloatAsState(targetValue = fraction, animationSpec = tween(700), label = "risk-bar")

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("$emoji  $label", style = MaterialTheme.typography.bodyMedium)
            Text("$percent%", style = MaterialTheme.typography.labelMedium, color = accent)
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
                        Brush.horizontalGradient(listOf(accent, accent.copy(alpha = 0.6f))),
                        RoundedCornerShape(6.dp),
                    ),
            )
        }
    }
}

/** Barras de 24 horas: cuanto más alta, más veces el riesgo apareció a esa hora. */
@Composable
private fun HourHeatmap(byHour: List<Int>, riskiest: Int?) {
    val max = (byHour.maxOrNull() ?: 0).coerceAtLeast(1)
    Row(
        modifier = Modifier.fillMaxWidth().height(64.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = androidx.compose.ui.Alignment.Bottom,
    ) {
        byHour.forEachIndexed { hour, count ->
            val ratio = count.toFloat() / max
            val color = if (hour == riskiest) RiskRed else RiskRed.copy(alpha = 0.35f)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(if (count == 0) 0.06f else 0.2f + 0.8f * ratio)
                    .background(
                        if (count == 0) color.copy(alpha = 0.12f) else color,
                        RoundedCornerShape(2.dp),
                    ),
            )
        }
    }
}

@Composable
private fun MiniStat(emoji: String, label: String, value: String, accent: Color, modifier: Modifier = Modifier) {
    SectionCard(modifier = modifier, containerColor = accent.copy(alpha = 0.13f)) {
        Text(emoji, style = MaterialTheme.typography.titleLarge)
        Text(label.uppercase(), style = MaterialTheme.typography.labelMedium, color = accent)
        Text(value, style = MaterialTheme.typography.headlineSmall, fontFamily = FontFamily.Monospace)
    }
}
