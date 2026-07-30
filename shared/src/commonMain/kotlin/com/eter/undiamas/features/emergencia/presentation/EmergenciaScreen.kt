package com.eter.undiamas.features.emergencia.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.eter.undiamas.core.presentation.AppState

@Composable
fun EmergenciaScreen(state: AppState) {
    val contact = state.profile.trustedContact

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFC62828).copy(alpha = 0.08f))
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            "Estás a salvo. Vamos paso a paso.",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Text(
            "Respira profundo: inhala 4 segundos, sostén 4, exhala 6. Repítelo 3 veces mientras decides tu siguiente paso.",
            style = MaterialTheme.typography.bodyLarge,
        )

        if (contact != null) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Tu contacto de confianza", style = MaterialTheme.typography.labelLarge)
                    Text(contact.name, style = MaterialTheme.typography.titleLarge)
                    Text(contact.phone, style = MaterialTheme.typography.bodyMedium)
                }
            }
            Button(
                onClick = { /* Fase 3: integrar marcado real o deep link telefónico */ },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Llamar a ${contact.name}")
            }
        } else {
            Text(
                "Aún no registras un contacto de confianza. Agrégalo en tu perfil para tenerlo aquí a la mano.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        Text(
            "Este momento pasará. Ya has llegado hasta aquí antes.",
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
