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
import kotlin.time.Instant
import kotlin.time.Clock
import kotlinx.coroutines.delay
import com.eter.undiamas.core.presentation.rememberNow
import com.eter.undiamas.core.presentation.theme.RiskYellow
import com.eter.undiamas.core.domain.biometrics.SpikeDetector
import com.eter.undiamas.core.domain.biometrics.WITHDRAWAL_BPM_THRESHOLD
import com.eter.undiamas.core.domain.biometrics.WithdrawalAlertDetector
import com.eter.undiamas.core.presentation.Navigator
import com.eter.undiamas.core.presentation.Screen

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
private const val REFRESH_INTERVAL_MILLIS = 60_000L

@Composable
fun BiometriaScreen(state: AppState, navigator: Navigator) {
    val provider = state.biometricsProvider
    var ui by remember { mutableStateOf<UiState>(UiState.Loading) }
    var lastUpdated by remember { mutableStateOf<Instant?>(null) }
    var showJson by remember { mutableStateOf(false) }
    // Inicio del episodio del que ya se avisó, para no repetir el aviso en cada refresco.
    var avisadoEn by remember { mutableStateOf<Long?>(null) }
    val scope = rememberCoroutineScope()

    /**
     * [silent] distingue la carga inicial del refresco periódico: en silencio los datos
     * se actualizan en su lugar sin spinner, y un fallo pasajero de fondo no borra de la
     * pantalla la última lectura buena.
     */
    suspend fun reload(silent: Boolean = false) {
        if (provider == null) {
            ui = UiState.Unavailable
            return
        }
        if (!silent) ui = UiState.Loading
        when (val result = provider.readLast24h()) {
            is BiometricsResult.Ready -> {
                ui = UiState.Ready(result.snapshot)
                lastUpdated = Clock.System.now()

                // Solo se avisa si el pulso sigue alto AHORA y no se avisó ya de este
                // mismo episodio: repetir el aviso cada minuto acabaría por anularlo.
                val detector = WithdrawalAlertDetector()
                val episodioActivo = detector.isActive(result.snapshot.heartRate)
                val ultimoEpisodio = detector.detect(result.snapshot.heartRate).lastOrNull()
                if (episodioActivo && ultimoEpisodio != null &&
                    ultimoEpisodio.startEpochSeconds != avisadoEn
                ) {
                    avisadoEn = ultimoEpisodio.startEpochSeconds
                    state.notify(
                        "Tu pulso está en ${ultimoEpisodio.peakBpm} bpm. " +
                            "Puede ser un momento de abstinencia: respira, no estás solo.",
                    )
                }
                if (!episodioActivo) avisadoEn = null
            }
            // Si revocaron el permiso a media sesión sí hay que decirlo, aun en silencio.
            BiometricsResult.PermissionsRequired -> ui = UiState.NeedsPermissions
            BiometricsResult.Unavailable -> ui = UiState.Unavailable
            is BiometricsResult.Error ->
                if (!silent || ui !is UiState.Ready) ui = UiState.Error(result.message)
        }
    }

    // Carga inicial y sondeo cada minuto mientras la pantalla esté visible; al salir,
    // LaunchedEffect se cancela y el sondeo muere con ella.
    LaunchedEffect(Unit) {
        reload()
        while (true) {
            delay(REFRESH_INTERVAL_MILLIS)
            reload(silent = true)
        }
    }

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
        lastUpdated?.let { updated -> LastUpdatedLine(updated) }

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
                WithdrawalAlertCard(current.snapshot, navigator)
                SnapshotContent(current.snapshot, showJson, onToggleJson = { showJson = !showJson })
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { scope.launch { reload(silent = true) } },
                ) {
                    Icon(AppIcons.Refrescar, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text("  Actualizar ahora")
                }
            }
        }
    }
}

/** "Actualizado hace Ns · se refresca cada minuto", con el contador avanzando en vivo. */
@Composable
private fun LastUpdatedLine(updated: Instant) {
    val now by rememberNow()
    val seconds = (now.epochSeconds - updated.epochSeconds).coerceAtLeast(0)
    val age = if (seconds < 60) "hace ${seconds}s" else "hace ${seconds / 60} min"

    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            AppIcons.Reloj,
            contentDescription = null,
            tint = RiskGreen,
            modifier = Modifier.size(14.dp),
        )
        Text(
            "Actualizado $age · se refresca cada minuto",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
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
            val spikes = remember(snapshot) { SpikeDetector().detect(snapshot.heartRate) }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Stat("MÍN", snapshot.minBpm, RiskGreen)
                Stat("MEDIA", snapshot.avgBpm, PrimaryVioletStart)
                Stat("MÁX", snapshot.maxBpm, EmergencyCoralStart)
                Stat("PICOS", spikes.size.toLong(), RiskYellow)
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

/**
 * Aviso de posible episodio de abstinencia por frecuencia cardíaca.
 *
 * Se distingue entre "está pasando ahora" y "pasó hoy": el primero ofrece salida
 * inmediata a Urge Surfing, el segundo solo informa. Un aviso que exige acción cuando ya
 * pasó el momento genera culpa por algo que la persona ya superó.
 *
 * El tono es deliberadamente sin alarma ni juicio: un pulso alto tiene mil causas
 * inocentes, y tratar cada una como una emergencia enseñaría a ignorar el aviso.
 */
@Composable
private fun WithdrawalAlertCard(snapshot: BiometricsSnapshot, navigator: Navigator) {
    val detector = remember { WithdrawalAlertDetector() }
    val episodios = remember(snapshot) { detector.detect(snapshot.heartRate) }
    if (episodios.isEmpty()) return

    val activo = remember(snapshot) { detector.isActive(snapshot.heartRate) }
    val ultimo = episodios.last()
    val accent = if (activo) EmergencyCoralStart else RiskYellow

    SectionCard {
        SectionHeader(
            AppIcons.Alerta,
            if (activo) "Tu pulso está alto ahora" else "Hubo pulso alto hoy",
            accent,
        )
        Text(
            if (activo) {
                "Llevas ${ultimo.peakBpm} bpm, por encima de $WITHDRAWAL_BPM_THRESHOLD. " +
                    "Puede ser abstinencia, o puede ser que acabas de moverte. " +
                    "Si notas ansiedad, esto ayuda a que pase."
            } else {
                "Tu pulso pasó de $WITHDRAWAL_BPM_THRESHOLD bpm en " +
                    "${episodios.size} ${if (episodios.size == 1) "momento" else "momentos"} " +
                    "de las últimas 24 h, con un máximo de ${ultimo.peakBpm} bpm. " +
                    "Lo apuntamos por si te sirve reconocer tus horas difíciles."
            },
            style = MaterialTheme.typography.bodyMedium,
        )
        if (activo) {
            Button(
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = accent),
                onClick = { navigator.goTo(Screen.UrgeSurfing) },
            ) {
                Icon(AppIcons.Escudo, contentDescription = null, modifier = Modifier.size(20.dp))
                Text("  Acompáñame ahora")
            }
        }
    }
}
