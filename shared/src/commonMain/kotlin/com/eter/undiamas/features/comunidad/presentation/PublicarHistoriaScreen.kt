package com.eter.undiamas.features.comunidad.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.eter.undiamas.core.domain.model.AddictionType
import com.eter.undiamas.core.presentation.AppState
import com.eter.undiamas.core.presentation.components.pressable
import com.eter.undiamas.core.presentation.theme.AppIcons
import com.eter.undiamas.core.presentation.theme.InkMuted
import com.eter.undiamas.core.presentation.theme.RiskYellow
import com.eter.undiamas.features.comunidad.domain.BorradorDeHistoria
import com.eter.undiamas.features.comunidad.domain.CUERPO_MAXIMO
import com.eter.undiamas.features.comunidad.domain.CUERPO_MINIMO
import com.eter.undiamas.features.comunidad.domain.TITULO_MAXIMO
import com.eter.undiamas.features.comunidad.domain.ValidadorDeHistoria

/**
 * Escribir una historia para el muro.
 *
 * Antes del formulario va el recordatorio de que esto es público y de que el alias es lo
 * único que se ve. Quien está contando que estuvo en recuperación tiene derecho a saber
 * exactamente qué se publica de su parte, y a saberlo antes de escribir, no después.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PublicarHistoriaScreen(state: AppState, onVolver: () -> Unit) {
    val perfil by state.comunidadPerfil
    val validador = remember { ValidadorDeHistoria() }

    var alias by remember(perfil.alias) { mutableStateOf(perfil.alias) }
    var titulo by remember { mutableStateOf("") }
    var cuerpo by remember { mutableStateOf("") }
    var objetivo by remember(state.profile.addiction) { mutableStateOf(state.profile.addiction) }
    var compartirRacha by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) { state.cargarPerfilDeComunidad() }

    val borrador = BorradorDeHistoria(titulo, cuerpo, objetivo, compartirRacha)
    val errores = validador.errores(borrador, perfil.copy(alias = alias))
    val avisos = validador.avisos(borrador)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    "Esto lo va a leer gente que no conoces",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    "Solo se publica tu alias, tu texto y —si tú quieres— tus días de racha. " +
                        "Nunca tu nombre ni tu correo. Puedes borrarla cuando quieras.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Campo("Tu alias en el muro") {
            OutlinedTextField(
                value = alias,
                onValueChange = { alias = it.take(24) },
                placeholder = { Text("Como quieras que te llamen aquí") },
                leadingIcon = { Icon(AppIcons.Usuario, null, tint = InkMuted) },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Campo("Título") {
            OutlinedTextField(
                value = titulo,
                onValueChange = { titulo = it.take(TITULO_MAXIMO) },
                placeholder = { Text("Lo que más te ayudó, en una línea") },
                supportingText = { Text("${titulo.length} / $TITULO_MAXIMO") },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Campo("Tu experiencia") {
            OutlinedTextField(
                value = cuerpo,
                onValueChange = { cuerpo = it.take(CUERPO_MAXIMO) },
                placeholder = { Text("¿Qué te funcionó? ¿Qué le dirías a quien empieza hoy?") },
                supportingText = {
                    Text(
                        if (cuerpo.length < CUERPO_MINIMO) {
                            "${cuerpo.length} / $CUERPO_MINIMO mínimo"
                        } else {
                            "${cuerpo.length} / $CUERPO_MAXIMO"
                        },
                    )
                },
                minLines = 8,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Campo("¿Sobre qué? (opcional)") {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AddictionType.entries.forEach { tipo ->
                    val activo = objetivo == tipo
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = if (activo) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                        } else {
                            MaterialTheme.colorScheme.surface
                        },
                        modifier = Modifier.pressable({ objetivo = if (activo) null else tipo }),
                    ) {
                        Text(
                            tipo.title,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (activo) MaterialTheme.colorScheme.primary else InkMuted,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Mostrar mis días de racha", style = MaterialTheme.typography.bodyLarge)
                Text(
                    "Es lo que ordena el muro. Si lo apagas, tu historia sale igual.",
                    style = MaterialTheme.typography.labelMedium,
                    color = InkMuted,
                )
            }
            Switch(checked = compartirRacha, onCheckedChange = { compartirRacha = it })
        }

        // Los avisos van justo encima del botón: es el último momento en que sirven.
        avisos.forEach { aviso ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    AppIcons.Alerta,
                    contentDescription = null,
                    tint = RiskYellow,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    aviso.mensaje,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        errores.firstOrNull()?.let { error ->
            Text(
                error.mensaje,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.error,
            )
        }

        Button(
            onClick = {
                state.publicarHistoria(borrador, alias.trim(), onListo = onVolver)
            },
            enabled = errores.isEmpty(),
            modifier = Modifier.fillMaxWidth().height(52.dp),
        ) { Text("Publicar mi historia") }

        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun Campo(etiqueta: String, contenido: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            etiqueta,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
        )
        contenido()
    }
}
