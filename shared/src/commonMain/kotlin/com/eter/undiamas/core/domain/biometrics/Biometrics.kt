package com.eter.undiamas.core.domain.biometrics

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Una lectura puntual de frecuencia cardíaca de la pulsera. */
@Serializable
data class HeartRateSample(
    val epochSeconds: Long,
    val bpm: Long,
)

/**
 * Foto de las últimas 24 horas de datos biométricos, ya en el formato que consume
 * el análisis de prevención. Es el JSON que viaja al algoritmo.
 */
@Serializable
data class BiometricsSnapshot(
    val generatedAtEpochSeconds: Long,
    val windowHours: Int = 24,
    val totalSteps: Long,
    val heartRate: List<HeartRateSample>,
) {
    val minBpm: Long? get() = heartRate.minOfOrNull { it.bpm }
    val maxBpm: Long? get() = heartRate.maxOfOrNull { it.bpm }
    val avgBpm: Long? get() = if (heartRate.isEmpty()) null else heartRate.sumOf { it.bpm } / heartRate.size
}

// encodeDefaults: sin esto, windowHours (que casi siempre vale su default de 24)
// desaparecería del JSON y el análisis no sabría qué ventana está mirando.
private val analysisJson = Json {
    prettyPrint = true
    encodeDefaults = true
}

/**
 * Lo que consume el algoritmo: la foto cruda, los picos relativos a la línea base del día
 * y los episodios que cruzaron el umbral absoluto de abstinencia.
 */
@Serializable
data class AnalysisPayload(
    val snapshot: BiometricsSnapshot,
    val heartRateSpikes: List<HeartRateSpike>,
    val withdrawalAlerts: List<WithdrawalAlert>,
    /** Umbral vigente, para que el análisis sepa contra qué se compararon las lecturas. */
    val withdrawalThresholdBpm: Long = WITHDRAWAL_BPM_THRESHOLD,
)

/** Serializa la foto con kotlinx.serialization, que ya es la librería JSON del proyecto. */
fun BiometricsSnapshot.toAnalysisJson(): String =
    analysisJson.encodeToString(
        AnalysisPayload(
            snapshot = this,
            heartRateSpikes = SpikeDetector().detect(heartRate),
            withdrawalAlerts = WithdrawalAlertDetector().detect(heartRate),
        ),
    )

/**
 * Punto de entrada de una sola llamada para el algoritmo: lectura de 24 h → JSON listo,
 * con cada caso no exitoso convertido en un failure descriptivo en vez de una excepción
 * suelta que tumbe la app.
 */
suspend fun BiometricsProvider.readLast24hAsAnalysisJson(): Result<String> =
    when (val result = readLast24h()) {
        is BiometricsResult.Ready -> Result.success(result.snapshot.toAnalysisJson())
        BiometricsResult.PermissionsRequired ->
            Result.failure(IllegalStateException("Faltan los permisos de Health Connect"))
        BiometricsResult.Unavailable ->
            Result.failure(IllegalStateException("Health Connect no está disponible en este dispositivo"))
        is BiometricsResult.Error -> Result.failure(IllegalStateException(result.message))
    }

/** Resultado de intentar leer la pulsera, con cada caso que la UI debe distinguir. */
sealed interface BiometricsResult {
    data class Ready(val snapshot: BiometricsSnapshot) : BiometricsResult

    /** Health Connect está, pero la persona aún no autoriza leer sus datos. */
    data object PermissionsRequired : BiometricsResult

    /** No hay Health Connect en este dispositivo (o es iOS): la sección se muestra apagada. */
    data object Unavailable : BiometricsResult

    data class Error(val message: String) : BiometricsResult
}

/**
 * Puerto de lectura biométrica. La implementación Android usa Health Connect;
 * en iOS no hay implementación todavía y la app funciona igual sin ella.
 */
interface BiometricsProvider {
    suspend fun readLast24h(): BiometricsResult

    /** Lanza el diálogo de permisos del sistema; true si quedaron todos concedidos. */
    suspend fun requestPermissions(): Boolean
}
