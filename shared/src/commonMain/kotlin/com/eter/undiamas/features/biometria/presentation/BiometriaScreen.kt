package com.eter.undiamas.features.biometria.presentation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eter.undiamas.core.domain.biometrics.BiometricsResult
import com.eter.undiamas.core.domain.biometrics.BiometricsSnapshot
import com.eter.undiamas.core.domain.biometrics.toAnalysisJson
import com.eter.undiamas.core.presentation.AppState
import com.eter.undiamas.core.presentation.components.OdometerText
import com.eter.undiamas.core.presentation.components.SectionCard
import com.eter.undiamas.core.presentation.components.SectionHeader
import com.eter.undiamas.core.presentation.components.SectionHeaderLarge
import com.eter.undiamas.core.presentation.theme.AppIcons
import com.eter.undiamas.core.presentation.theme.EmergencyCoralStart
import com.eter.undiamas.core.presentation.theme.PrimaryVioletStart
import com.eter.undiamas.core.presentation.theme.RiskGreen
import kotlinx.coroutines.launch

private sealed interface UiState {
    data object Loading : UiState
    data object NeedsPermissions : UiState
    data object Unavailable : UiState
    data class Error(val message: String) : UiState
    data class Ready(val snapshot: BiometricsSnapshot) : UiState
}

/**
 * Sección de biometría: pasos y frecuencia cardíaca de las últimas 24 h leídos de la
 * pulsera vía Health Connect, más el JSON exacto que consume el análisis de prevención.
 */
@Composable
fun BiometriaScreen(state: AppState) {
    val provider = state.biometricsProvider
    var ui by remember { mutableStateOf<UiState>(UiState.Loading) }
    var showJson by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    suspend fun reload() {
        if (provider == null) {
            ui = UiState.Unavailable
            return
        }
        ui = UiState.Loading
        ui = when (val result = provider.readLast24h()) {
            is BiometricsResult.Ready -> UiState.Ready(result.snapshot)
            BiometricsResult.PermissionsRequired -> UiState.NeedsPermissions
            BiometricsResult.Unavailable -> UiState.Unavailable
            is BiometricsResult.Error -> UiState.Error(result.message)
        }
    }

    LaunchedEffect(Unit) { reload() }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        SectionHeaderLarge(AppIcons.Corazon, "Biometría")
        Text(
            "Lo que tu pulsera registró en las últimas 24 horas, tal como lo ve el análisis de prevención.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        when (val current = ui) {
            UiState.Loading -> Box(
                modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }

            UiState.Unavailable -> SectionCard {
                SectionHeader(AppIcons.Alerta, "Sin Health Connect", MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    "Este dispositivo no tiene Health Connect disponible, así que no se puede " +
                        "leer la pulsera. La app funciona igual sin esta sección.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            UiState.NeedsPermissions -> SectionCard {
                SectionHeader(AppIcons.Escudo, "Falta tu autorización", PrimaryVioletStart)
                Text(
                    "Para analizar tus pasos y tu ritmo cardíaco necesitamos que autorices la " +
                        "lectura en Health Connect. Solo se leen esos dos datos y solo las últimas 24 horas.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        scope.launch {
                            val granted = provider?.requestPermissions() ?: false
                            if (granted) reload() else state.notify("Sin permisos no podemos leer la pulsera")
                        }
                    },
                ) { Text("Autorizar lectura") }
            }

            is UiState.Error -> SectionCard {
                SectionHeader(AppIcons.Alerta, "No se pudo leer", EmergencyCoralStart)
                Text(current.message, style = MaterialTheme.typography.bodyMedium)
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { scope.launch { reload() } },
                ) { Text("Reintentar") }
            }

            is UiState.Ready -> {
                SnapshotContent(current.snapshot, showJson, onToggleJson = { showJson = !showJson })
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { scope.launch { reload() } },
                ) {
                    Icon(AppIcons.Refrescar, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text("  Actualizar lectura")
                }
            }
        }
    }
}

@Composable
private fun SnapshotContent(snapshot: BiometricsSnapshot, showJson: Boolean, onToggleJson: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        SectionCard(modifier = Modifier.weight(1f)) {
            SectionHeader(AppIcons.Pasos, "Pasos", RiskGreen)
            OdometerText("${snapshot.totalSteps}", style = MaterialTheme.typography.headlineMedium)
        }
        SectionCard(modifier = Modifier.weight(1f)) {
            SectionHeader(AppIcons.Corazon, "FC media", EmergencyCoralStart)
            OdometerText(
                snapshot.avgBpm?.let { "$it bpm" } ?: "—",
                style = MaterialTheme.typography.headlineMedium,
            )
        }
    }

    SectionCard {
        SectionHeader(AppIcons.Corazon, "Ritmo cardíaco · 24 h", EmergencyCoralStart)
        if (snapshot.heartRate.isEmpty()) {
            Text(
                "La pulsera no reportó lecturas de frecuencia cardíaca en este periodo.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            HeartRateSparkline(
                samples = snapshot.heartRate.map { it.bpm },
                modifier = Modifier.fillMaxWidth().height(120.dp),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Stat("MÍN", snapshot.minBpm, RiskGreen)
                Stat("MEDIA", snapshot.avgBpm, PrimaryVioletStart)
                Stat("MÁX", snapshot.maxBpm, EmergencyCoralStart)
            }
            Text(
                "Los picos sostenidos de FC en reposo son una de las señales que el análisis " +
                    "usa para anticipar un momento difícil.",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    SectionCard {
        SectionHeader(AppIcons.Estadisticas, "JSON para el análisis", PrimaryVioletStart)
        OutlinedButton(onClick = onToggleJson, modifier = Modifier.fillMaxWidth()) {
            Text(if (showJson) "Ocultar JSON" else "Ver JSON")
        }
        if (showJson) {
            Text(
                snapshot.toAnalysisJson(),
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                lineHeight = 15.sp,
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            )
        }
    }
}

@Composable
private fun Stat(label: String, value: Long?, accent: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = accent)
        Text(
            value?.let { "$it" } ?: "—",
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
        )
    }
}

/** Polilínea simple de la FC: suficiente para ver los picos sin librerías de gráficos. */
@Composable
private fun HeartRateSparkline(samples: List<Long>, modifier: Modifier = Modifier) {
    val accent = EmergencyCoralStart
    Canvas(modifier = modifier) {
        if (samples.size < 2) return@Canvas
        val min = samples.min().toFloat()
        val max = samples.max().toFloat()
        val range = (max - min).coerceAtLeast(1f)
        val stepX = size.width / (samples.size - 1)

        val path = Path()
        samples.forEachIndexed { index, bpm ->
            val x = index * stepX
            // Margen vertical del 10% para que los extremos no toquen el borde.
            val y = size.height * 0.9f - ((bpm - min) / range) * size.height * 0.8f
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }

        // Relleno degradado bajo la curva.
        val fill = Path().apply {
            addPath(path)
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }
        drawPath(fill, brush = Brush.verticalGradient(listOf(accent.copy(alpha = 0.25f), Color.Transparent)))
        drawPath(path, color = accent, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
    }
}
