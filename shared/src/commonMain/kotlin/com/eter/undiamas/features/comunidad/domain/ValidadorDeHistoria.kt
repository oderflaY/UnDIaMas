package com.eter.undiamas.features.comunidad.domain

/** Qué está mal en un borrador, con un texto que se le puede enseñar a la persona. */
sealed interface ProblemaDeHistoria {
    val mensaje: String

    data object TituloVacio : ProblemaDeHistoria {
        override val mensaje = "Ponle un título a tu historia."
    }

    data object TituloLargo : ProblemaDeHistoria {
        override val mensaje = "El título no puede pasar de $TITULO_MAXIMO caracteres."
    }

    data object CuerpoCorto : ProblemaDeHistoria {
        override val mensaje = "Cuéntanos un poco más: al menos $CUERPO_MINIMO caracteres."
    }

    data object CuerpoLargo : ProblemaDeHistoria {
        override val mensaje = "Te pasaste de $CUERPO_MAXIMO caracteres."
    }

    data object RachaInsuficiente : ProblemaDeHistoria {
        override val mensaje =
            "Para publicar hacen falta $DIAS_MINIMOS_PARA_PUBLICAR días de racha."
    }

    data object SinAlias : ProblemaDeHistoria {
        override val mensaje = "Elige primero un alias para el muro."
    }

    data class PosibleDatoPersonal(val detectado: String) : ProblemaDeHistoria {
        override val mensaje =
            "Parece que escribiste $detectado. En el muro te lee gente que no conoces."
    }
}

/**
 * Revisa un borrador antes de mandarlo.
 *
 * Dos cosas distintas: los límites de longitud, que el backend valida igual (esto solo
 * evita el viaje), y el aviso de datos personales, que es lo que de verdad importa aquí.
 *
 * El aviso **no bloquea**. Alguien puede querer poner su teléfono a propósito para que le
 * escriban, y decidirlo es suyo; lo que no puede pasar es que lo publique sin darse cuenta
 * de que el muro es público.
 */
class ValidadorDeHistoria {

    /** Problemas que impiden publicar. */
    fun errores(borrador: BorradorDeHistoria, perfil: PerfilDeComunidad): List<ProblemaDeHistoria> =
        buildList {
            val titulo = borrador.titulo.trim()
            val cuerpo = borrador.cuerpo.trim()

            if (titulo.isEmpty()) add(ProblemaDeHistoria.TituloVacio)
            if (titulo.length > TITULO_MAXIMO) add(ProblemaDeHistoria.TituloLargo)
            if (cuerpo.length < CUERPO_MINIMO) add(ProblemaDeHistoria.CuerpoCorto)
            if (cuerpo.length > CUERPO_MAXIMO) add(ProblemaDeHistoria.CuerpoLargo)
            if (perfil.alias.isBlank()) add(ProblemaDeHistoria.SinAlias)
            if (!perfil.puedePublicar) add(ProblemaDeHistoria.RachaInsuficiente)
        }

    /** Avisos que no impiden publicar, pero conviene enseñar antes. */
    fun avisos(borrador: BorradorDeHistoria): List<ProblemaDeHistoria> = buildList {
        val texto = borrador.cuerpo
        if (CORREO.containsMatchIn(texto)) {
            add(ProblemaDeHistoria.PosibleDatoPersonal("un correo"))
        }
        if (TELEFONO.containsMatchIn(texto)) {
            add(ProblemaDeHistoria.PosibleDatoPersonal("un número de teléfono"))
        }
        if (ENLACE.containsMatchIn(texto)) {
            add(ProblemaDeHistoria.PosibleDatoPersonal("un enlace"))
        }
    }

    fun puedeEnviar(borrador: BorradorDeHistoria, perfil: PerfilDeComunidad): Boolean =
        errores(borrador, perfil).isEmpty()

    private companion object {
        val CORREO = Regex("""[\w.+-]+@[\w-]+\.[\w.]{2,}""")

        /** Siete dígitos o más seguidos, con espacios o guiones en medio. */
        val TELEFONO = Regex("""(\+?\d[\d\s-]{6,}\d)""")
        val ENLACE = Regex("""(https?://|www\.)\S+""", RegexOption.IGNORE_CASE)
    }
}
