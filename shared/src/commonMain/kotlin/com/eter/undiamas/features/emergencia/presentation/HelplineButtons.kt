package com.eter.undiamas.features.emergencia.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LocalHospital
import androidx.compose.material.icons.rounded.SupportAgent
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.eter.undiamas.core.presentation.components.pressable

/**
 * Líneas de ayuda oficiales de México, siempre disponibles aunque la persona no tenga
 * un contacto de confianza registrado.
 *
 * Línea de la Vida (800 911 2000) es el servicio gratuito 24/7 de CONASAMA especializado
 * en adicciones y salud mental; 911 es la emergencia nacional para urgencias médicas.
 * Si esta app se distribuye fuera de México, estos números deben regionalizarse.
 */
private const val LINEA_DE_LA_VIDA = "8009112000"
private const val EMERGENCIAS_MEDICAS = "911"

@Composable
fun HelplineButtons(onDial: (String) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        HelplineButton(
            icon = Icons.Rounded.SupportAgent,
            title = "Línea de la Vida (24/7)",
            subtitle = "800 911 2000 · gratuita, especializada en adicciones",
            onClick = { onDial(LINEA_DE_LA_VIDA) },
        )
        HelplineButton(
            icon = Icons.Rounded.LocalHospital,
            title = "Emergencias médicas (911)",
            subtitle = "Si hay riesgo inmediato para tu vida o la de alguien más",
            onClick = { onDial(EMERGENCIAS_MEDICAS) },
        )
    }
}

@Composable
private fun HelplineButton(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    // Fondo translúcido para integrarse con el degradado coral sin perder contraste del texto.
    Surface(
        shape = RoundedCornerShape(50),
        color = Color.White.copy(alpha = 0.14f),
        modifier = Modifier.fillMaxWidth().height(72.dp).pressable(onClick, longPress = true),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = Color.White)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.82f),
                )
            }
        }
    }
}
