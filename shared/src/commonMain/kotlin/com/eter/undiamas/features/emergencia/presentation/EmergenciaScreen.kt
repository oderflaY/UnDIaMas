package com.eter.undiamas.features.emergencia.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.eter.undiamas.core.presentation.AppState
import com.eter.undiamas.core.presentation.components.BreathingCircle
import com.eter.undiamas.core.presentation.theme.Coral60
import com.eter.undiamas.core.presentation.theme.Violet60

@Composable
fun EmergenciaScreen(state: AppState) {
    val contact = state.profile.trustedContact

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Coral60.copy(alpha = 0.16f), MaterialTheme.colorScheme.background),
                ),
            )
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Estás a salvo", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Sigue el círculo con tu respiración. Inhala 4, sostén 4, exhala 6.",
            style = MaterialTheme.typography.bodyLarge,
        )

        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            BreathingCircle(colors = listOf(Coral60, Violet60))
        }

        if (contact != null) {
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("TU CONTACTO DE CONFIANZA", style = MaterialTheme.typography.labelMedium, color = Coral60)
                    Text(contact.name, style = MaterialTheme.typography.headlineSmall)
                    Text(contact.phone, style = MaterialTheme.typography.bodyMedium)
                }
            }
            Button(
                onClick = { /* Fase 3: integrar marcado real o deep link telefónico */ },
                colors = ButtonDefaults.buttonColors(containerColor = Coral60, contentColor = Color.White),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("📞  Llamar a ${contact.name}", style = MaterialTheme.typography.titleMedium)
            }
        } else {
            Text(
                "Aún no registras un contacto de confianza. Agrégalo en tu perfil para tenerlo aquí a la mano.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        Text(
            "Este momento pasará. Ya has llegado hasta aquí antes.",
            style = MaterialTheme.typography.titleMedium,
        )
    }
}
