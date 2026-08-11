package com.eter.undiamas.core.data.api

/**
 * Reserva, no la direccion real.
 *
 * La de verdad la pone MainActivity con `ApiConfig.configurar(BuildConfig.API_BASE_URL, …)`,
 * que en debug apunta a la maquina de desarrollo y en release al dominio de produccion.
 *
 * Aqui no puede haber una IP de red local: este archivo se compila tal cual dentro del APK
 * que se sube a la tienda, y la direccion del wifi de alguien no tiene por que viajar ahi.
 */
actual fun defaultApiBaseUrl(): String = "https://api.undiamas.mx"
