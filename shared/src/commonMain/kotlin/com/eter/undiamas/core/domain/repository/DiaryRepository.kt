package com.eter.undiamas.core.domain.repository

import com.eter.undiamas.features.diario.domain.DiaryEntry
import kotlinx.coroutines.flow.Flow

/** Puerto de persistencia del diario, aislado por uid en `/usuarios/{uid}/diario`. */
interface DiaryRepository {
    fun observeRecent(uid: String, limit: Int = 200): Flow<List<DiaryEntry>>

    suspend fun add(uid: String, entry: DiaryEntry): String

    suspend fun deleteAll(uid: String)
}
