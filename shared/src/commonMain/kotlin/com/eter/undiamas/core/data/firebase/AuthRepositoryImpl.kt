package com.eter.undiamas.core.data.firebase

import com.eter.undiamas.core.domain.repository.AuthRepository
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.EmailAuthProvider
import dev.gitlive.firebase.auth.auth
import kotlinx.coroutines.flow.map

/** Adaptador real sobre Firebase Auth (Fase 3 · 07): sesion anonima por defecto. */
class AuthRepositoryImpl : AuthRepository {
    private val auth = Firebase.auth

    override val currentUserId = auth.authStateChanged.map { it?.uid }

    override suspend fun signInAnonymously(): String {
        auth.currentUser?.let { return it.uid }
        val result = auth.signInAnonymously()
        return requireNotNull(result.user?.uid) { "Firebase Auth no devolvio un uid tras iniciar sesion anonima" }
    }

    override suspend fun linkAnonymousWithEmail(email: String, password: String): String {
        val anonymous = auth.currentUser
        val credential = EmailAuthProvider.credential(email, password)
        val result = if (anonymous != null) {
            anonymous.linkWithCredential(credential)
        } else {
            auth.createUserWithEmailAndPassword(email, password)
        }
        return requireNotNull(result.user?.uid) { "Firebase Auth no devolvio un uid tras vincular la cuenta" }
    }

    override suspend fun signInWithEmail(email: String, password: String): String {
        val result = auth.signInWithEmailAndPassword(email, password)
        return requireNotNull(result.user?.uid) { "Firebase Auth no devolvio un uid tras iniciar sesion" }
    }

    override suspend fun signOut() {
        auth.signOut()
    }
}
