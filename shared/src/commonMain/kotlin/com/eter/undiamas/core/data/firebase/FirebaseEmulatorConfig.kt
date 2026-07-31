package com.eter.undiamas.core.data.firebase

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.firestore
import dev.gitlive.firebase.functions.functions

/**
 * Apunta la app a los emuladores locales de Firebase (Auth/Firestore/Functions) en vez del
 * proyecto real, para el checkpoint de "Demo local funcional" de la Fase 3.
 *
 * IMPORTANTE: [EMULATOR_HOST] es `10.0.2.2`, que es el loopback del host **visto desde el
 * emulador de Android**. Desde un telefono fisico esa direccion no lleva a ningun lado, asi
 * que con esto en true la app no puede autenticarse ni leer nada en un dispositivo real.
 *
 * Dejar en false para usar el proyecto de Firebase configurado en google-services.json.
 * Ponerlo en true solo cuando se corra `firebase emulators:start` Y se pruebe en el
 * emulador de Android (no en telefono).
 */
const val USE_FIREBASE_EMULATORS = false

/** Loopback del host visto desde el emulador de Android (`10.0.2.2`). En iOS simulator usar "localhost". */
private const val EMULATOR_HOST = "10.0.2.2"

/** Llamar una sola vez y antes de cualquier uso de Firestore: repetirlo lanza excepcion. */
private var emulatorsConfigured = false

fun configureFirebaseEmulatorsIfNeeded() {
    if (!USE_FIREBASE_EMULATORS || emulatorsConfigured) return
    emulatorsConfigured = true
    Firebase.auth.useEmulator(EMULATOR_HOST, 9099)
    Firebase.firestore.useEmulator(EMULATOR_HOST, 8080)
    Firebase.functions.useEmulator(EMULATOR_HOST, 5001)
}
