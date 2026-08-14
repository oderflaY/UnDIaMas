package com.eter.undiamas.core.presentation

import androidx.compose.runtime.Composable

/**
 * Abre una direccion en el navegador del telefono.
 *
 * Existe para las paginas que Google Play exige tener fuera de la app —politica de
 * privacidad y borrado de cuenta— y que por eso no pueden vivir dentro de ella.
 */
@Composable
expect fun rememberLinkOpener(): (String) -> Unit

/**
 * Las paginas legales, en el dominio raiz.
 *
 * Play pide que la de privacidad sea accesible sin instalar la app y que la de borrado de
 * cuenta se pueda alcanzar desde fuera. Van en `undiamas.site` y no en `api.undiamas.site`
 * porque el subdominio del API solo sirve JSON.
 */
object PaginasLegales {
    const val PRIVACIDAD = "https://undiamas.site/privacidad"
    const val BORRAR_CUENTA = "https://undiamas.site/borrar-cuenta"
}
