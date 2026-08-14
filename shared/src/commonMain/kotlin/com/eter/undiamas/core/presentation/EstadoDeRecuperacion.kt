package com.eter.undiamas.core.presentation

/**
 * Lo que la pantalla de recuperacion necesita saber en cada momento.
 *
 * Es una sola estructura y no cuatro banderas sueltas porque los estados se excluyen entre
 * si: o se esta pidiendo el codigo, o se esta escribiendo, o el servidor no puede mandar
 * correos. Tenerlos separados invita a pintar dos a la vez.
 */
data class EstadoDeRecuperacion(
    val email: String,
    /** true en cuanto el servidor acepta mandar el codigo: entonces se pide escribirlo. */
    val codigoEnviado: Boolean = false,
    val enviando: Boolean = false,
    /**
     * El servidor no tiene SMTP configurado.
     *
     * Se distingue del resto de errores porque no hay nada que reintentar: insistir no va a
     * hacer que llegue un correo, y dejar a alguien pulsando "reenviar" es peor que decirlo.
     */
    val sinCorreoEnElServidor: Boolean = false,
    val error: String? = null,
)
