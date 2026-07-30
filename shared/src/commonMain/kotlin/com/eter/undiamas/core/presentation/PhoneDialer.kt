package com.eter.undiamas.core.presentation

import androidx.compose.runtime.Composable

/**
 * Abre la app de teléfono con el número ya cargado, sin marcar por su cuenta:
 * la persona confirma la llamada. Así el protocolo de emergencia funciona sin
 * pedir el permiso CALL_PHONE ni arriesgar una llamada accidental.
 */
@Composable
expect fun rememberPhoneDialer(): (String) -> Unit

/** Deja solo lo que un marcador entiende, por si el teléfono se capturó con espacios o guiones. */
internal fun sanitizePhoneNumber(raw: String): String =
    raw.filter { it.isDigit() || it == '+' || it == '#' || it == '*' }
