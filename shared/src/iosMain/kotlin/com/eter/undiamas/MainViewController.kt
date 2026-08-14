package com.eter.undiamas

import androidx.compose.ui.window.ComposeUIViewController

/**
 * Punto de entrada de iOS.
 *
 * La direccion del backend no se pasa desde Swift: es una sola, fija, y vive en
 * [com.eter.undiamas.core.data.api.ApiConfig].
 */
fun MainViewController() = ComposeUIViewController { App() }
