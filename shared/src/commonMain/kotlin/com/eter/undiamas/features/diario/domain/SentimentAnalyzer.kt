package com.eter.undiamas.features.diario.domain

enum class Sentiment(val label: String) {
    POSITIVO("Positiva"),
    NEUTRAL("Neutral"),
    VULNERABLE("Vulnerable"),
}

/**
 * Clasificación local por palabras clave: sin red, sin enviar el diario a ningún lado.
 * Es deliberadamente conservadora — ante señales mezcladas marca VULNERABLE, porque en
 * este dominio subestimar un mal momento cuesta más que sobreestimarlo.
 */
class SentimentAnalyzer {

    private val positivas = setOf(
        "tranquilo", "tranquila", "orgulloso", "orgullosa", "feliz", "bien", "logre", "logro",
        "avance", "agradecido", "agradecida", "calma", "fuerte", "contento", "contenta", "esperanza",
    )

    private val vulnerables = setOf(
        "ansioso", "ansiosa", "ansiedad", "recaer", "recaida", "consumir", "ganas", "triste",
        "solo", "sola", "soledad", "estres", "estresado", "estresada", "culpa", "miedo",
        "cansado", "cansada", "harto", "harta", "vacio", "deprimido", "deprimida",
    )

    fun analyze(text: String): Sentiment {
        val words = normalize(text).split(' ', '\n', '\t', ',', '.', ';', ':', '!', '?')
            .filter { it.isNotBlank() }
            .toSet()

        val hasVulnerable = words.any { it in vulnerables }
        val hasPositive = words.any { it in positivas }

        return when {
            hasVulnerable -> Sentiment.VULNERABLE
            hasPositive -> Sentiment.POSITIVO
            else -> Sentiment.NEUTRAL
        }
    }

    /** Minúsculas y sin acentos, para que "ansioso" y "ANSIOSO" cuenten igual. */
    private fun normalize(text: String): String = text.lowercase()
        .map { char ->
            when (char) {
                'á' -> 'a'
                'é' -> 'e'
                'í' -> 'i'
                'ó' -> 'o'
                'ú', 'ü' -> 'u'
                else -> char
            }
        }
        .joinToString("")
}

/** Palabras del texto, para el contador en vivo de la caja de entrada. */
fun countWords(text: String): Int =
    text.split(' ', '\n', '\t').count { it.isNotBlank() }
