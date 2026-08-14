package com.eter.undiamas.core.presentation.theme

import androidx.compose.ui.graphics.Color

// ---- Tokens oficiales del sistema de diseño --------------------------------
//
// La paleta base es clara: púrpura #6C5CE7 sobre gris casi blanco #F8F9FA. Los tonos
// oscuros de más abajo se conservan porque el tema oscuro sigue existiendo como opción.

/** Púrpura de marca. Es el color de las acciones: si algo es pulsable e importante, va así. */
val BrandPurple = Color(0xFF6C5CE7)

/** Extremos del degradado del splash y de la tarjeta principal del inicio. */
val BrandPurpleLight = Color(0xFF8278FA)
val BrandPurpleDeep = Color(0xFF5A4FCF)

/** Fondo de las pantallas claras. No es blanco puro: deja que las tarjetas blancas resalten. */
val CanvasLight = Color(0xFFF8F9FA)

/** Tinta principal y secundaria del tema claro. */
val InkStrong = Color(0xFF2D3436)
val InkMuted = Color(0xFFA4B0BE)

/** Acentos cálidos: el naranja marca logros, el crema es el fondo de la frase del día. */
val AccentOrange = Color(0xFFFFA502)
val AccentCream = Color(0xFFFFF8E7)

/** Borde muy tenue de inputs y tarjetas; casi no se ve, y ese es el punto. */
val HairlineLight = Color(0xFFE8EAED)

// ---- Equivalentes del tema oscuro -----------------------------------------
// No son grises puros: llevan una pizca de azul-violeta para que el púrpura de marca no
// parezca pegado encima de un fondo neutro que no le corresponde.
val CanvasDark = Color(0xFF16151D)
val CanvasDarkElevated = Color(0xFF201F2A)
val InkMutedDark = Color(0xFF9A96AE)
val HairlineDark = Color(0xFF2E2C3B)

val BackgroundDark = Color(0xFF0F172A) // Slate 900
val SurfaceDark = Color(0xFF1E293B) // Slate 800

val PrimaryVioletStart = Color(0xFF8B5CF6)
val PrimaryVioletEnd = Color(0xFF6D28D9)

val RiskGreen = Color(0xFF10B981) // Emerald 500
val RiskYellow = Color(0xFFF59E0B) // Amber 500
val RiskRed = Color(0xFFEF4444) // Rose 500

val EmergencyCoralStart = Color(0xFFFF5252)
val EmergencyCoralEnd = Color(0xFFE53935)

val AssistantMagentaStart = Color(0xFFEC4899)
val AssistantMagentaEnd = Color(0xFFC026D3)

val SavingsGoldStart = Color(0xFF10B981)
val SavingsGoldEnd = Color(0xFFF59E0B)

val TextPrimary = Color(0xFFF8FAFC) // Slate 50
val TextSecondary = Color(0xFF94A3B8) // Slate 400

// ---- Acentos por módulo ----------------------------------------------------
val AccentCheckIn = RiskGreen
val AccentDiario = PrimaryVioletStart
val AccentAhorro = SavingsGoldStart
val AccentMagenta = AssistantMagentaStart
val AccentStats = Color(0xFF6366F1) // Indigo 500
val AccentPerfil = Color(0xFFEC4899)
val AnswerBlue = Color(0xFF3B82F6) // Ficha "NO" del check-in

// ---- Neutros para la variante clara ---------------------------------------
val SurfaceLight = Color(0xFFFDFBFF)
val SurfaceLightDim = Color(0xFFEEF1F7)
val InkLight = Color(0xFF0F172A)
val InkLightSecondary = Color(0xFF475569)
