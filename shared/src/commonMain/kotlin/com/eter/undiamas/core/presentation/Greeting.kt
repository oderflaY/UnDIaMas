package com.eter.undiamas.core.presentation

/** Saludo según la hora local, para que la app se sienta acompañando el día. */
fun greetingForHour(hour: Int): String = when (hour) {
    in 5..11 -> "Buenos días ☀️"
    in 12..18 -> "Buenas tardes 🌤️"
    else -> "Buenas noches 🌙"
}

/** Frases de aliento; se rotan sin repetir la misma dos veces seguidas. */
val motivationalQuotes: List<String> = listOf(
    "Un día a la vez es suficiente.",
    "No tienes que hacerlo perfecto, solo seguir aquí.",
    "El impulso pasa; tú te quedas.",
    "Hoy elegiste cuidarte, y eso cuenta.",
    "Tu récord empezó con un solo día como este.",
    "Pedir ayuda también es avanzar.",
    "Lo difícil de hoy es la fuerza de mañana.",
    "Estás construyendo algo que nadie te puede quitar.",
)
