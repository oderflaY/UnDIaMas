package com.eter.undiamas.core.presentation.theme

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Anchor
import androidx.compose.material.icons.rounded.DirectionsWalk
import androidx.compose.material.icons.rounded.MonitorHeart
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ConfirmationNumber
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Diversity3
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Flight
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.HealthAndSafety
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Hearing
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.LocalCafe
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Logout
import androidx.compose.material.icons.rounded.MedicalServices
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Nightlight
import androidx.compose.material.icons.rounded.PanTool
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.PhoneInTalk
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material.icons.rounded.SentimentDissatisfied
import androidx.compose.material.icons.rounded.SentimentNeutral
import androidx.compose.material.icons.rounded.SentimentSatisfied
import androidx.compose.material.icons.rounded.SentimentVeryDissatisfied
import androidx.compose.material.icons.rounded.SentimentVerySatisfied
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.Traffic
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.Work
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Catálogo único de iconografía. Centralizarlo evita que cada pantalla elija
 * un icono distinto para el mismo concepto y facilita cambiar el set completo.
 */
object AppIcons {
    // Navegación
    val Inicio: ImageVector = Icons.Rounded.Home
    val CheckIn: ImageVector = Icons.Rounded.CheckCircle
    val Diario: ImageVector = Icons.Rounded.AutoStories
    val Estadisticas: ImageVector = Icons.Rounded.Insights
    val Perfil: ImageVector = Icons.Rounded.AccountCircle
    val Configuracion: ImageVector = Icons.Rounded.Settings
    val Atras: ImageVector = Icons.Rounded.ArrowBack

    // Sobriedad
    val Racha: ImageVector = Icons.Rounded.LocalFireDepartment
    val Reloj: ImageVector = Icons.Rounded.Timer
    val Record: ImageVector = Icons.Rounded.EmojiEvents
    val Insignia: ImageVector = Icons.Rounded.WorkspacePremium
    val Semaforo: ImageVector = Icons.Rounded.Traffic
    val Escudo: ImageVector = Icons.Rounded.Shield
    val Calendario: ImageVector = Icons.Rounded.CalendarMonth

    // Emergencia
    val Emergencia: ImageVector = Icons.Rounded.HealthAndSafety
    val Respiracion: ImageVector = Icons.Rounded.SelfImprovement
    val Aire: ImageVector = Icons.Rounded.Air
    val Llamar: ImageVector = Icons.Rounded.PhoneInTalk
    val Distraccion: ImageVector = Icons.Rounded.TouchApp
    val Calma: ImageVector = Icons.Rounded.Spa

    // Ahorro
    val Ahorro: ImageVector = Icons.Rounded.Savings
    val Cartera: ImageVector = Icons.Rounded.AccountBalanceWallet
    val Cafe: ImageVector = Icons.Rounded.LocalCafe
    val Libro: ImageVector = Icons.Rounded.MenuBook
    val Concierto: ImageVector = Icons.Rounded.ConfirmationNumber
    val Viaje: ImageVector = Icons.Rounded.Flight
    val Tendencia: ImageVector = Icons.Rounded.TrendingUp
    val Meta: ImageVector = Icons.Rounded.Flag

    // Asistente
    val Asistente: ImageVector = Icons.Rounded.AutoAwesome
    val Mente: ImageVector = Icons.Rounded.Psychology
    val Enviar: ImageVector = Icons.Rounded.Send

    // Perfil y red de soporte
    val Red: ImageVector = Icons.Rounded.Diversity3
    val PorQue: ImageVector = Icons.Rounded.Favorite
    val Grupo: ImageVector = Icons.Rounded.Groups

    // Configuración y privacidad
    val Notificaciones: ImageVector = Icons.Rounded.Notifications
    val Resumen: ImageVector = Icons.Rounded.Insights
    val TemaOscuro: ImageVector = Icons.Rounded.DarkMode
    val Bloqueado: ImageVector = Icons.Rounded.Lock
    val Desbloqueado: ImageVector = Icons.Rounded.LockOpen
    val Camuflaje: ImageVector = Icons.Rounded.VisibilityOff
    val CerrarSesion: ImageVector = Icons.Rounded.Logout
    val Borrar: ImageVector = Icons.Rounded.DeleteForever
    val Hora: ImageVector = Icons.Rounded.Schedule
    val Buscar: ImageVector = Icons.Rounded.Search
    val Refrescar: ImageVector = Icons.Rounded.Refresh
    val Alerta: ImageVector = Icons.Rounded.Warning

    // Saludo según la hora
    val Dia: ImageVector = Icons.Rounded.WbSunny
    val Tarde: ImageVector = Icons.Rounded.Nightlight
    val Noche: ImageVector = Icons.Rounded.Bedtime

    // Sentidos del ejercicio 5-4-3-2-1
    val Ver: ImageVector = Icons.Rounded.Visibility
    val Tocar: ImageVector = Icons.Rounded.PanTool
    val Oir: ImageVector = Icons.Rounded.Hearing
    val Oler: ImageVector = Icons.Rounded.Air
    val Saborear: ImageVector = Icons.Rounded.Restaurant

    // Secciones nuevas
    val Capsula: ImageVector = Icons.Rounded.Inventory2
    val Habitos: ImageVector = Icons.Rounded.Spa
    val Ancla: ImageVector = Icons.Rounded.Anchor
    val CheckInVacio: ImageVector = Icons.Rounded.RadioButtonUnchecked

    // Tipos de ancla
    val AnclaPersona: ImageVector = Icons.Rounded.Favorite
    val AnclaMascota: ImageVector = Icons.Rounded.Pets
    val AnclaMeta: ImageVector = Icons.Rounded.Flag
    val AnclaLugar: ImageVector = Icons.Rounded.Place
    val AnclaRecuerdo: ImageVector = Icons.Rounded.PhotoCamera

    // Biometría
    val Corazon: ImageVector = Icons.Rounded.MonitorHeart
    val Pasos: ImageVector = Icons.Rounded.DirectionsWalk

    // Detonantes
    val Estres: ImageVector = Icons.Rounded.Warning
    val Soledad: ImageVector = Icons.Rounded.Nightlight
    val Cansancio: ImageVector = Icons.Rounded.Bedtime
    val Aburrimiento: ImageVector = Icons.Rounded.Schedule
    val Social: ImageVector = Icons.Rounded.People
    val Trabajo: ImageVector = Icons.Rounded.Work

    // Ánimo
    val AnimoMuyMal: ImageVector = Icons.Rounded.SentimentVeryDissatisfied
    val AnimoMal: ImageVector = Icons.Rounded.SentimentDissatisfied
    val AnimoNeutral: ImageVector = Icons.Rounded.SentimentNeutral
    val AnimoBien: ImageVector = Icons.Rounded.SentimentSatisfied
    val AnimoMuyBien: ImageVector = Icons.Rounded.SentimentVerySatisfied

    // Sentimiento del diario
    val SentimientoPositivo: ImageVector = Icons.Rounded.WbSunny
    val SentimientoNeutral: ImageVector = Icons.Rounded.Air
    val SentimientoVulnerable: ImageVector = Icons.Rounded.WaterDrop

    // Roles de la red de soporte
    val RolPadrino: ImageVector = Icons.Rounded.Diversity3
    val RolTerapeuta: ImageVector = Icons.Rounded.MedicalServices
    val RolFamiliar: ImageVector = Icons.Rounded.Home
    val RolAmistad: ImageVector = Icons.Rounded.Favorite
}
