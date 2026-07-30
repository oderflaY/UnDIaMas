package com.eter.undiamas.core.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.eter.undiamas.core.domain.model.RiskLevel
import com.eter.undiamas.core.presentation.color
import kotlinx.datetime.LocalDate

private const val COLUMNS = 7

/**
 * Cuadrícula de los últimos [days] días: cada casilla toma el color del semáforo de ese día,
 * y queda hueca si no hubo check-in. Es el registro visual de los días limpios.
 */
@Composable
fun CleanDaysCalendar(
    days: List<LocalDate>,
    levelByDay: Map<LocalDate, RiskLevel>,
    today: LocalDate,
    modifier: Modifier = Modifier,
) {
    val emptyColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f)
    val todayBorder = MaterialTheme.colorScheme.primary

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        days.chunked(COLUMNS).forEach { week ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                week.forEach { day ->
                    val level = levelByDay[day]
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .background(level?.color ?: emptyColor, RoundedCornerShape(8.dp))
                            .then(
                                if (day == today) {
                                    Modifier.border(2.dp, todayBorder, RoundedCornerShape(8.dp))
                                } else {
                                    Modifier
                                },
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "${day.dayOfMonth}",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (level != null) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                // Rellena la última fila incompleta para que las casillas no se estiren.
                repeat(COLUMNS - week.size) {
                    Box(modifier = Modifier.weight(1f).aspectRatio(1f))
                }
            }
        }
    }
}
