package com.yxhuang.jizhang.core.database.repository

import com.yxhuang.jizhang.core.database.dao.ParseFailureLogDao
import com.yxhuang.jizhang.core.database.entity.ParseFailureLogEntity
import com.yxhuang.jizhang.core.model.ParseFailureLog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ParseFailureRepositoryImpl(
    private val dao: ParseFailureLogDao
) : ParseFailureRepository {

    override fun observeAll(): Flow<List<ParseFailureLog>> {
        return dao.observeAll().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun insert(log: ParseFailureLog): Long {
        return dao.insert(log.toEntity())
    }

    override suspend fun getById(id: Long): ParseFailureLog? {
        return dao.getById(id)?.toDomain()
    }

    override suspend fun deleteAll() {
        dao.deleteAll()
    }

    private fun ParseFailureLogEntity.toDomain(): ParseFailureLog {
        return ParseFailureLog(
            id = id,
            rawText = rawText,
            sourceApp = sourceApp,
            timestamp = timestamp,
            reason = reason
        )
    }

    private fun ParseFailureLog.toEntity(): ParseFailureLogEntity {
        return ParseFailureLogEntity(
            id = id,
            rawText = rawText,
            sourceApp = sourceApp,
            timestamp = timestamp,
            reason = reason
        )
    }
}
