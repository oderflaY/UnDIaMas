package com.eter.undiamas.core.data.firebase

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.firestore
import dev.gitlive.firebase.functions.functions

/**
 * Mientras no se corra `firebase deploy` contra el proyecto real, la app apunta a los
 * emuladores locales de Firebase (Auth/Firestore/Functions), siguiendo el checkpoint de
 * "Demo local funcional" de la Fase 3 (`firebase emulators:start` en la raiz del repo).
 * Cambialo a false una vez el backend este desplegado de verdad.
 */
const val USE_FIREBASE_EMULATORS = true

/** Loopback del host visto desde el emulador de Android (`10.0.2.2`). En iOS simulator usar "localhost". */
private const val EMULATOR_HOST = "10.0.2.2"

fun configureFirebaseEmulatorsIfNeeded() {
    if (!USE_FIREBASE_EMULATORS) return
    Firebase.auth.useEmulator(EMULATOR_HOST, 9099)
    Firebase.firestore.useEmulator(EMULATOR_HOST, 8080)
    Firebase.functions.useEmulator(EMULATOR_HOST, 5001)
}
