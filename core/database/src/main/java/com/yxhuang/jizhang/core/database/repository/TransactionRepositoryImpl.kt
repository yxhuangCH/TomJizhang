package com.yxhuang.jizhang.core.database.repository

import com.yxhuang.jizhang.core.database.dao.TransactionDao
import com.yxhuang.jizhang.core.database.entity.TransactionEntity
import com.yxhuang.jizhang.core.model.Transaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TransactionRepositoryImpl(
    private val dao: TransactionDao
) : TransactionRepository {

    override fun observeAll(): Flow<List<Transaction>> {
        return dao.observeAll().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun insert(transaction: Transaction): Long {
        return dao.insert(transaction.toEntity())
    }

    override suspend fun update(transaction: Transaction) {
        dao.update(transaction.toEntity())
    }

    override suspend fun getById(id: Long): Transaction? {
        return dao.getById(id)?.toDomain()
    }

    private fun TransactionEntity.toDomain(): Transaction {
        return Transaction(
            id = id,
            amount = amount,
            merchant = merchant,
            category = category,
            timestamp = timestamp,
            sourceApp = sourceApp,
            rawText = rawText,
            createdAt = createdAt
        )
    }

    private fun Transaction.toEntity(): TransactionEntity {
        return TransactionEntity(
            id = id,
            amount = amount,
            merchant = merchant,
            category = category,
            timestamp = timestamp,
            sourceApp = sourceApp,
            rawText = rawText,
            createdAt = createdAt
        )
    }
}
