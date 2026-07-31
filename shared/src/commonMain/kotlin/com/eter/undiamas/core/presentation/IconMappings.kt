package com.eter.undiamas.core.presentation

import androidx.compose.ui.graphics.vector.ImageVector
import com.eter.undiamas.core.domain.model.AddictionType
import com.eter.undiamas.core.domain.model.Mood
import com.eter.undiamas.core.domain.model.RiskLevel
import com.eter.undiamas.core.domain.model.SupportRole
import com.eter.undiamas.core.domain.model.Trigger
import com.eter.undiamas.core.presentation.theme.AppIcons
import com.eter.undiamas.features.anclas.domain.AnchorKind
import com.eter.undiamas.features.diario.domain.Sentiment

/**
 * Traducciones de tipos de dominio a iconografía. Viven en presentación para que
 * la capa de negocio no sepa nada de cómo se dibuja.
 */

val RiskLevel.icon: ImageVector
    get() = when (this) {
        RiskLevel.VERDE -> AppIcons.Escudo
        RiskLevel.AMARILLO -> AppIcons.Alerta
        RiskLevel.ROJO -> AppIcons.Emergencia
    }

val Mood.icon: ImageVector
    get() = when (this) {
        Mood.MUY_MAL -> AppIcons.AnimoMuyMal
        Mood.MAL -> AppIcons.AnimoMal
        Mood.NEUTRAL -> AppIcons.AnimoNeutral
        Mood.BIEN -> AppIcons.AnimoBien
        Mood.MUY_BIEN -> AppIcons.AnimoMuyBien
    }

val Trigger.icon: ImageVector
    get() = when (this) {
        Trigger.ESTRES -> AppIcons.Estres
        Trigger.SOLEDAD -> AppIcons.Soledad
        Trigger.CANSANCIO -> AppIcons.Cansancio
        Trigger.ABURRIMIENTO -> AppIcons.Aburrimiento
        Trigger.SOCIAL -> AppIcons.Social
        Trigger.TRABAJO -> AppIcons.Trabajo
    }

val SupportRole.icon: ImageVector
    get() = when (this) {
        SupportRole.PADRINO -> AppIcons.RolPadrino
        SupportRole.TERAPEUTA -> AppIcons.RolTerapeuta
        SupportRole.FAMILIAR -> AppIcons.RolFamiliar
        SupportRole.AMISTAD -> AppIcons.RolAmistad
    }

val AnchorKind.icon: ImageVector
    get() = when (this) {
        AnchorKind.PERSONA -> AppIcons.AnclaPersona
        AnchorKind.MASCOTA -> AppIcons.AnclaMascota
        AnchorKind.META -> AppIcons.AnclaMeta
        AnchorKind.LUGAR -> AppIcons.AnclaLugar
        AnchorKind.RECUERDO -> AppIcons.AnclaRecuerdo
    }

val AddictionType.icon: ImageVector
    get() = when (this) {
        AddictionType.ALCOHOL -> AppIcons.AdiccionAlcohol
        AddictionType.NICOTINA -> AppIcons.AdiccionNicotina
        AddictionType.OPIOIDES -> AppIcons.AdiccionOpioides
        AddictionType.ESTIMULANTES -> AppIcons.AdiccionEstimulantes
        AddictionType.CANNABIS -> AppIcons.AdiccionCannabis
        AddictionType.JUEGO -> AppIcons.AdiccionJuego
        AddictionType.PANTALLAS -> AppIcons.AdiccionPantallas
        AddictionType.COMPRAS -> AppIcons.AdiccionCompras
        AddictionType.OTRA -> AppIcons.AdiccionOtra
    }

val Sentiment.icon: ImageVector
    get() = when (this) {
        Sentiment.POSITIVO -> AppIcons.SentimientoPositivo
        Sentiment.NEUTRAL -> AppIcons.SentimientoNeutral
        Sentiment.VULNERABLE -> AppIcons.SentimientoVulnerable
    }

/** Icono del saludo según la hora local, en línea con [greetingForHour]. */
fun greetingIconForHour(hour: Int): ImageVector = when (hour) {
    in 5..11 -> AppIcons.Dia
    in 12..18 -> AppIcons.Tarde
    else -> AppIcons.Noche
}
