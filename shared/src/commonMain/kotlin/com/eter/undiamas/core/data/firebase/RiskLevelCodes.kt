package com.eter.undiamas.core.data.firebase

import com.eter.undiamas.core.domain.model.RiskLevel

/**
 * El dominio nombra el semaforo en espanol (VERDE/AMARILLO/ROJO) y Firestore lo guarda en
 * ingles (GREEN/YELLOW/RED), porque asi se creo la base. En vez de renombrar uno de los
 * dos y romper los datos existentes, la traduccion vive aqui, en un unico sitio.
 */
fun RiskLevel.toFirestoreCode(): String = when (this) {
    RiskLevel.VERDE -> "GREEN"
    RiskLevel.AMARILLO -> "YELLOW"
    RiskLevel.ROJO -> "RED"
}

/**
 * Un codigo desconocido cae a VERDE a proposito: es el estado que no dispara alarmas ni
 * protocolos. Inventar un ROJO por un dato corrupto seria alarmar a alguien sin motivo.
 */
fun String.toRiskLevelOrGreen(): RiskLevel = when (uppercase()) {
    "GREEN", "VERDE" -> RiskLevel.VERDE
    "YELLOW", "AMARILLO" -> RiskLevel.AMARILLO
    "RED", "ROJO" -> RiskLevel.ROJO
    else -> RiskLevel.VERDE
}
