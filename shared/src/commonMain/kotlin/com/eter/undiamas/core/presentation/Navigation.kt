package com.eter.undiamas.core.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class Screen(val label: String) {
    Inicio("Inicio"),
    Sobriedad("Sobriedad"),
    CheckIn("Check-in"),
    Ia("Asistente"),
    Diario("Diario"),
    Estadisticas("Estadísticas"),
    Calculadora("Ahorro"),
    Emergencia("Emergencia"),
    UrgeSurfing("Sostener el impulso"),
    Perfil("Perfil"),
    Configuracion("Configuración"),
}

class Navigator(initial: Screen = Screen.Inicio) {
    private val backStack = mutableListOf(initial)

    var current: Screen by mutableStateOf(initial)
        private set

    fun goTo(screen: Screen) {
        backStack.add(screen)
        current = screen
    }

    fun back(): Boolean {
        if (backStack.size <= 1) return false
        backStack.removeAt(backStack.lastIndex)
        current = backStack.last()
        return true
    }
}
