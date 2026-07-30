package com.eter.undiamas.features.perfil.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.eter.undiamas.core.domain.model.TrustedContact
import com.eter.undiamas.core.presentation.AppState
import com.eter.undiamas.core.presentation.Navigator
import com.eter.undiamas.core.presentation.Screen

@Composable
fun PerfilScreen(state: AppState, navigator: Navigator) {
    var name by remember { mutableStateOf(state.profile.displayName) }
    var contactName by remember { mutableStateOf(state.profile.trustedContact?.name.orEmpty()) }
    var contactPhone by remember { mutableStateOf(state.profile.trustedContact?.phone.orEmpty()) }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Perfil", style = MaterialTheme.typography.headlineSmall)

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Tu nombre") },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = contactName,
            onValueChange = { contactName = it },
            label = { Text("Contacto de confianza") },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = contactPhone,
            onValueChange = { contactPhone = it },
            label = { Text("Teléfono de contacto") },
            modifier = Modifier.fillMaxWidth(),
        )

        Button(
            onClick = {
                state.updateProfile { profile ->
                    profile.copy(
                        displayName = name,
                        trustedContact = if (contactName.isBlank()) null else TrustedContact(contactName, contactPhone),
                    )
                }
            },
        ) { Text("Guardar cambios") }

        Button(onClick = { navigator.goTo(Screen.Configuracion) }) {
            Text("Configuración")
        }
    }
}
