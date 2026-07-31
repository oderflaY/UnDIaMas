package com.eter.undiamas.core.domain.repository

import com.eter.undiamas.core.domain.model.MoodEntry
import kotlinx.coroutines.flow.Flow

/** Puerto de persistencia del registro rapido de animo, aislado por uid en `/usuarios/{uid}/animos`. */
interface MoodRepository {
    fun observeRecent(uid: String, limit: Int = 200): Flow<List<MoodEntry>>

    suspend fun add(uid: String, entry: MoodEntry): String

    suspend fun deleteAll(uid: String)
}
