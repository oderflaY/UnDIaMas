package com.eter.undiamas.features.avisos.domain

import com.eter.undiamas.core.domain.model.RiskLevel

/**
 * Una plantilla de aviso.
 *
 * [aplicaA] decide en qué niveles del semáforo tiene sentido; [disponible] descarta las que
 * no se pueden redactar con los datos que hay — una plantilla de ahorro sin gasto declarado
 * saldría como "Has ahorrado 0", que es peor que no mandar nada.
 */
data class PlantillaAviso(
    val id: String,
    val categoria: CategoriaAviso,
    val aplicaA: Set<RiskLevel>,
    val urgente: Boolean = false,
    val disponible: (ContextoAviso) -> Boolean = { true },
    val titulo: (ContextoAviso) -> String,
    val cuerpo: (ContextoAviso) -> String,
)

private val TODOS = setOf(RiskLevel.VERDE, RiskLevel.AMARILLO, RiskLevel.ROJO)
private val AMARILLO_Y_ROJO = setOf(RiskLevel.AMARILLO, RiskLevel.ROJO)
private val SOLO_ROJO = setOf(RiskLevel.ROJO)

/** Importe con dos decimales, sin depender de formateadores que no existen en KMP común. */
internal fun dinero(cantidad: Double, moneda: String): String {
    val centavos = (cantidad * 100).toLong()
    val enteros = centavos / 100
    val resto = (centavos % 100).toInt().let { if (it < 10) "0$it" else "$it" }
    return "$$enteros.$resto $moneda"
}

private fun dias(n: Long) = if (n == 1L) "1 día" else "$n días"

/**
 * Catálogo de plantillas.
 *
 * Están escritas con dos reglas que no son negociables en esta app:
 *
 * 1. **Nunca culpan.** Ni una insinúa que la persona haya fallado, ni siquiera en rojo.
 *    Un aviso que hace sentir mal a alguien que está a punto de consumir empuja justo en
 *    la dirección contraria.
 * 2. **Siempre ofrecen algo que hacer.** Sobre todo las de ayuda: "respira conmigo",
 *    "llama a Luis", "abre tus anclas". Un ánimo sin salida concreta no sirve en rojo.
 */
val PLANTILLAS: List<PlantillaAviso> = listOf(

    // ---- Racha ------------------------------------------------------------------
    PlantillaAviso(
        id = "racha_hoy",
        categoria = CategoriaAviso.RACHA,
        aplicaA = TODOS,
        titulo = { "Hoy suma" },
        cuerpo = { ctx ->
            if (ctx.dias > 0) {
                "Llevas ${dias(ctx.dias)}. Hoy puede ser uno más."
            } else {
                "Hoy empieza tu cuenta. Un día a la vez."
            }
        },
    ),
    PlantillaAviso(
        id = "racha_cerca_record",
        categoria = CategoriaAviso.RACHA,
        aplicaA = TODOS,
        disponible = { it.faltaParaRecord in 1..14 },
        titulo = { "Estás cerca" },
        cuerpo = { ctx ->
            "Te faltan ${dias(ctx.faltaParaRecord)} para igualar tu récord de " +
                "${dias(ctx.recordDias)}. Ya lo hiciste una vez."
        },
    ),
    PlantillaAviso(
        id = "racha_es_record",
        categoria = CategoriaAviso.RACHA,
        aplicaA = TODOS,
        disponible = { it.recordDias > 0 && it.dias >= it.recordDias },
        titulo = { "Tu mejor racha" },
        cuerpo = { ctx -> "${dias(ctx.dias)}. Nunca habías llegado tan lejos." },
    ),
    PlantillaAviso(
        id = "racha_manana",
        categoria = CategoriaAviso.RACHA,
        aplicaA = TODOS,
        titulo = { ctx -> if (ctx.nombreCorto.isBlank()) "Buenos días" else "Buenos días, ${ctx.nombreCorto}" },
        cuerpo = { "Un día nuevo. No tienes que resolverlo todo, solo hoy." },
    ),
    PlantillaAviso(
        id = "racha_noche",
        categoria = CategoriaAviso.RACHA,
        aplicaA = TODOS,
        disponible = { it.dias > 0 },
        titulo = { "Un día más" },
        cuerpo = { ctx -> "Cierras el día con ${dias(ctx.dias)}. Eso ya no te lo quita nadie." },
    ),

    // ---- Check-in ---------------------------------------------------------------
    PlantillaAviso(
        id = "checkin_pendiente",
        categoria = CategoriaAviso.CHECKIN,
        aplicaA = TODOS,
        disponible = { !it.checkInHecho },
        titulo = { "¿Cómo va tu día?" },
        cuerpo = { "Un minuto de check-in y sabrás mejor dónde estás parado hoy." },
    ),
    PlantillaAviso(
        id = "checkin_marca_dia",
        categoria = CategoriaAviso.CHECKIN,
        aplicaA = TODOS,
        disponible = { !it.checkInHecho },
        titulo = { "Marca tu día" },
        cuerpo = { "Todavía no registraste hoy. Se hace en menos de lo que crees." },
    ),

    // ---- Anclas y "mi por qué" --------------------------------------------------
    PlantillaAviso(
        id = "ancla_recordatorio",
        categoria = CategoriaAviso.ANCLA,
        aplicaA = TODOS,
        disponible = { it.anclas.isNotEmpty() },
        titulo = { "Por esto lo haces" },
        cuerpo = { ctx ->
            val ancla = ctx.anclas.first()
            if (ancla.note.isNotBlank()) "${ancla.title}. ${ancla.note}" else ancla.title
        },
    ),
    PlantillaAviso(
        id = "ancla_segunda",
        categoria = CategoriaAviso.ANCLA,
        aplicaA = TODOS,
        disponible = { it.anclas.size > 1 },
        titulo = { "Acuérdate" },
        cuerpo = { ctx -> ctx.anclas[1].title },
    ),
    PlantillaAviso(
        id = "ancla_por_que",
        categoria = CategoriaAviso.ANCLA,
        aplicaA = TODOS,
        disponible = { it.porQue.isNotBlank() },
        titulo = { "Tu por qué" },
        cuerpo = { ctx -> ctx.porQue },
    ),
    PlantillaAviso(
        id = "ancla_mira_muro",
        categoria = CategoriaAviso.ANCLA,
        aplicaA = AMARILLO_Y_ROJO,
        disponible = { it.anclas.isNotEmpty() },
        titulo = { "Mira tus anclas" },
        cuerpo = { ctx ->
            "Tienes ${ctx.anclas.size} ${if (ctx.anclas.size == 1) "motivo guardado" else "motivos guardados"}. " +
                "Ábrelos un momento."
        },
    ),

    // ---- Ahorro -----------------------------------------------------------------
    PlantillaAviso(
        id = "ahorro_total",
        categoria = CategoriaAviso.AHORRO,
        aplicaA = TODOS,
        disponible = { it.ahorro >= 1.0 },
        titulo = { "Lo que llevas ahorrado" },
        cuerpo = { ctx -> "${dinero(ctx.ahorro, ctx.moneda)} que se quedaron contigo." },
    ),
    PlantillaAviso(
        id = "ahorro_equivalencia",
        categoria = CategoriaAviso.AHORRO,
        aplicaA = TODOS,
        disponible = { it.ahorro >= 50.0 },
        titulo = { "Tu dinero, de vuelta" },
        cuerpo = { ctx ->
            "Llevas ${dinero(ctx.ahorro, ctx.moneda)}. Échale un ojo a lo que ya podrías hacer con eso."
        },
    ),
    PlantillaAviso(
        id = "ahorro_hoy_suma",
        categoria = CategoriaAviso.AHORRO,
        aplicaA = TODOS,
        disponible = { it.ahorro >= 1.0 },
        titulo = { "Hoy también suma" },
        cuerpo = { ctx -> "Van ${dinero(ctx.ahorro, ctx.moneda)} desde que empezaste." },
    ),

    // ---- Ayuda (amarillo) -------------------------------------------------------
    PlantillaAviso(
        id = "ayuda_respira",
        categoria = CategoriaAviso.AYUDA,
        aplicaA = AMARILLO_Y_ROJO,
        titulo = { "Un momento contigo" },
        cuerpo = { "Tres minutos de respiración guiada. Nada más." },
    ),
    PlantillaAviso(
        id = "ayuda_detonante",
        categoria = CategoriaAviso.AYUDA,
        aplicaA = AMARILLO_Y_ROJO,
        titulo = { "¿Qué está pasando?" },
        cuerpo = { "Ponerle nombre a lo que te activó le quita la mitad de la fuerza." },
    ),
    PlantillaAviso(
        id = "ayuda_bunker",
        categoria = CategoriaAviso.AYUDA,
        aplicaA = AMARILLO_Y_ROJO,
        titulo = { "Sostén el impulso" },
        cuerpo = { "El impulso sube y baja. Quince minutos acompañado y habrá pasado." },
    ),

    // ---- Ayuda (rojo) — las urgentes --------------------------------------------
    PlantillaAviso(
        id = "rojo_estoy_aqui",
        categoria = CategoriaAviso.AYUDA,
        aplicaA = SOLO_ROJO,
        urgente = true,
        titulo = { ctx -> if (ctx.nombreCorto.isBlank()) "Estamos aquí" else "${ctx.nombreCorto}, estamos aquí" },
        cuerpo = { "Esto que sientes ahora va a bajar. No tienes que aguantarlo solo." },
    ),
    PlantillaAviso(
        id = "rojo_llama_contacto",
        categoria = CategoriaAviso.AYUDA,
        aplicaA = SOLO_ROJO,
        urgente = true,
        disponible = { it.contacto != null },
        titulo = { "Llama a alguien" },
        cuerpo = { ctx ->
            val contacto = ctx.contacto
            "${contacto?.name} está en tu lista para justo este momento. Una llamada basta."
        },
    ),
    PlantillaAviso(
        id = "rojo_respira_ya",
        categoria = CategoriaAviso.AYUDA,
        aplicaA = SOLO_ROJO,
        urgente = true,
        titulo = { "Respira conmigo" },
        cuerpo = { "Abre la app y sigue el círculo. Solo eso, por ahora." },
    ),
    PlantillaAviso(
        id = "rojo_ancla_fuerte",
        categoria = CategoriaAviso.AYUDA,
        aplicaA = SOLO_ROJO,
        urgente = true,
        disponible = { it.anclas.isNotEmpty() },
        titulo = { "Acuérdate de esto" },
        cuerpo = { ctx -> "${ctx.anclas.first().title}. Sigue ahí, y mañana también." },
    ),
    PlantillaAviso(
        id = "rojo_no_decidas_ahora",
        categoria = CategoriaAviso.AYUDA,
        aplicaA = SOLO_ROJO,
        urgente = true,
        titulo = { "No decidas ahora" },
        cuerpo = { "Deja pasar diez minutos antes de nada. Solo diez." },
    ),
    PlantillaAviso(
        id = "rojo_lo_que_llevas",
        categoria = CategoriaAviso.AYUDA,
        aplicaA = SOLO_ROJO,
        urgente = true,
        disponible = { it.dias > 0 },
        titulo = { "Lo que llevas" },
        cuerpo = { ctx -> "${dias(ctx.dias)} de trabajo tuyo. Hoy no tiene que cambiar eso." },
    ),
    PlantillaAviso(
        id = "rojo_emergencia",
        categoria = CategoriaAviso.AYUDA,
        aplicaA = SOLO_ROJO,
        urgente = true,
        titulo = { "Tu protocolo está listo" },
        cuerpo = { "Respiración, anclas y tu contacto, en una sola pantalla. Ábrela." },
    ),
)
