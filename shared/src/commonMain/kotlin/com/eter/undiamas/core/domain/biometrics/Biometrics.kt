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

/** Serializa la foto con kotlinx.serialization, que ya es la librería JSON del proyecto. */
fun BiometricsSnapshot.toAnalysisJson(): String = analysisJson.encodeToString(this)

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
