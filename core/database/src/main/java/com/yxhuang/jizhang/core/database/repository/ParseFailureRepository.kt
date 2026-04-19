package com.yxhuang.jizhang.core.database.repository

import com.yxhuang.jizhang.core.model.ParseFailureLog
import kotlinx.coroutines.flow.Flow

interface ParseFailureRepository {
    fun observeAll(): Flow<List<ParseFailureLog>>
    suspend fun insert(log: ParseFailureLog): Long
    suspend fun getById(id: Long): ParseFailureLog?
}
