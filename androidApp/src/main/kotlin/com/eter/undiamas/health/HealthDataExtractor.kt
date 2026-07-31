package com.eter.undiamas.health

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.eter.undiamas.core.domain.biometrics.BiometricsProvider
import com.eter.undiamas.core.domain.biometrics.BiometricsResult
import com.eter.undiamas.core.domain.biometrics.BiometricsSnapshot
import com.eter.undiamas.core.domain.biometrics.HeartRateSample
import java.time.Duration
import java.time.Instant

/**
 * Implementación Android del puerto [BiometricsProvider] sobre Health Connect.
 *
 * La pulsera (Xiaomi u otra) sincroniza con Health Connect y esta clase lee de ahí:
 * nunca habla con la pulsera directamente. Solo lee pasos y frecuencia cardíaca de
 * las últimas 24 horas, que es lo único que el análisis de prevención necesita.
 *
 * [launchPermissionRequest] lo aporta la Activity, porque el diálogo de permisos de
 * Health Connect es un ActivityResultContract y necesita una Activity viva para lanzarse.
 */
class HealthDataExtractor(
    private val context: Context,
    private val launchPermissionRequest: suspend (Set<String>) -> Set<String>,
) : BiometricsProvider {

    private val requiredPermissions = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(HeartRateRecord::class),
    )

    override suspend fun readLast24h(): BiometricsResult {
        if (HealthConnectClient.getSdkStatus(context) != HealthConnectClient.SDK_AVAILABLE) {
            return BiometricsResult.Unavailable
        }
        val client = HealthConnectClient.getOrCreate(context)

        val granted = try {
            client.permissionController.getGrantedPermissions()
        } catch (e: Exception) {
            return BiometricsResult.Error(e.message ?: "No se pudo consultar los permisos")
        }
        if (!granted.containsAll(requiredPermissions)) {
            return BiometricsResult.PermissionsRequired
        }

        return try {
            val end = Instant.now()
            val window = TimeRangeFilter.between(end.minus(Duration.ofHours(24)), end)

            val totalSteps = client
                .readRecords(ReadRecordsRequest(StepsRecord::class, timeRangeFilter = window))
                .records
                .sumOf { it.count }

            val heartRate = client
                .readRecords(ReadRecordsRequest(HeartRateRecord::class, timeRangeFilter = window))
                .records
                .flatMap { record -> record.samples }
                .map { sample -> HeartRateSample(sample.time.epochSecond, sample.beatsPerMinute) }
                .sortedBy { it.epochSeconds }

            BiometricsResult.Ready(
                BiometricsSnapshot(
                    generatedAtEpochSeconds = end.epochSecond,
                    totalSteps = totalSteps,
                    heartRate = heartRate,
                ),
            )
        } catch (e: Exception) {
            // Un fallo de lectura no debe tumbar la sección: la UI muestra el error y reintenta.
            BiometricsResult.Error(e.message ?: "Error leyendo Health Connect")
        }
    }

    override suspend fun requestPermissions(): Boolean {
        val granted = launchPermissionRequest(requiredPermissions)
        return granted.containsAll(requiredPermissions)
    }
}
