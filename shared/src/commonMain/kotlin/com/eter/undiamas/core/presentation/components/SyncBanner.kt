package com.eter.undiamas.core.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.eter.undiamas.core.data.local.EstadoSync
import com.eter.undiamas.core.presentation.AppState
import com.eter.undiamas.core.presentation.theme.AppIcons
import com.eter.undiamas.core.presentation.theme.RiskYellow

/**
 * Aviso de que hay cosas guardadas en el teléfono esperando a que vuelva la red.
 *
 * Existe por una razón concreta: alguien que escribe un check-in sin señal necesita saber
 * que **se guardó**, no quedarse con la duda de si su registro se perdió. Por eso el texto
 * afirma primero lo que sí pasó ("guardado en tu teléfono") y solo después explica lo que
 * falta. Y por eso no aparece cuando no hay nada pendiente: un aviso permanente de "sin
 * conexión" no ayuda a nadie y solo hace la app más ansiosa de usar.
 */
@Composable
fun SyncBanner(state: AppState, modifier: Modifier = Modifier) {
    val visible = state.pendingChanges > 0 || state.syncState == EstadoSync.ENVIANDO

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
        modifier = modifier,
    ) {
        Surface(
            color = RiskYellow.copy(alpha = 0.14f),
            contentColor = MaterialTheme.colorScheme.onSurface,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (state.syncState == EstadoSync.ENVIANDO) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = RiskYellow,
                    )
                } else {
                    Icon(
                        AppIcons.Escudo,
                        contentDescription = null,
                        tint = RiskYellow,
                        modifier = Modifier.size(20.dp),
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = textoPrincipal(state),
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Text(
                        text = textoSecundario(state),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                if (state.syncState != EstadoSync.ENVIANDO) {
                    TextButton(onClick = { state.sincronizarAhora() }) { Text("Reintentar") }
                }
            }
        }
    }
}

private fun textoPrincipal(state: AppState): String = when (state.syncState) {
    EstadoSync.ENVIANDO -> "Enviando tus cambios…"
    else -> {
        val cuantos = state.pendingChanges
        if (cuantos == 1) "1 cambio guardado en tu teléfono" else "$cuantos cambios guardados en tu teléfono"
    }
}

private fun textoSecundario(state: AppState): String = when {
    state.syncState == EstadoSync.ENVIANDO -> "Un momento."
    !state.isOnline -> "Se enviarán solos en cuanto vuelva la conexión."
    else -> "El servidor no responde. Se reintentará solo."
}
