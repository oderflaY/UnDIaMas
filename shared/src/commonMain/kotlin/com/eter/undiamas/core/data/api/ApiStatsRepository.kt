package com.eter.undiamas.core.data.api

import com.eter.undiamas.core.domain.repository.Alert
import com.eter.undiamas.core.domain.repository.RiskTrends
import com.eter.undiamas.core.domain.repository.StatsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Tendencias calculadas por el servidor.
 *
 * Es lo único que no se guarda en el teléfono: son un cálculo derivado sobre todo el
 * historial, no un dato que la persona haya escrito. Sin conexión no se muestran, y la
 * pantalla de estadísticas sigue teniendo lo que calcula en local a partir de los
 * check-ins guardados.
 */
class ApiStatsRepository(
    private val api: UnDiaMasApi,
) : StatsRepository {

    private val _trends = MutableStateFlow<RiskTrends?>(null)
    override val trends: StateFlow<RiskTrends?> = _trends.asStateFlow()

    override suspend fun refresh(days: Int) {
        _trends.value = api.riskTrends(days).toDomain()
    }

    private fun RiskTrendsDto.toDomain() = RiskTrends(
        days = dias,
        totalCheckIns = totalCheckIns,
        green = verdes,
        yellow = amarillos,
        red = rojos,
        averageCraving = promedioCraving,
        relapses = recaidas,
        alerts = alertas,
        topTriggers = detonantesFrecuentes.map { it.valor to it.total },
        trend = tendencia,
    )
}

/** Alerta del servidor traducida al dominio. Compartida por el repositorio y por el SSE. */
fun AlertDto.toDomain(): Alert = Alert(
    id = id,
    riskLevel = riskLevel.toRiskLevel(),
    message = message,
    handled = handled,
)
