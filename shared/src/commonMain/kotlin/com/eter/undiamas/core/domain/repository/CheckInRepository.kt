package com.eter.undiamas.core.domain.repository

import com.eter.undiamas.core.domain.model.CheckInEntry
import kotlinx.coroutines.flow.Flow

/** Puerto de persistencia del historial de check-ins, aislado por uid en `/usuarios/{uid}/checkins`. */
interface CheckInRepository {
    fun observeRecent(uid: String, limit: Int = 100): Flow<List<CheckInEntry>>

    suspend fun add(uid: String, entry: CheckInEntry): String

    /** Derecho al olvido: borra todos los check-ins del usuario. */
    suspend fun deleteAll(uid: String)
}
