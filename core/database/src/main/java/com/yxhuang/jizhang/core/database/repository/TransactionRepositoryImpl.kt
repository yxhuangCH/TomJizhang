package com.yxhuang.jizhang.core.database.repository

import com.yxhuang.jizhang.core.database.dao.TransactionDao
import com.yxhuang.jizhang.core.database.entity.TransactionEntity
import com.yxhuang.jizhang.core.model.Transaction
import com.yxhuang.jizhang.core.model.TransactionType
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

    override suspend fun getAll(): List<Transaction> {
        return dao.getAll().map { it.toDomain() }
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

    override suspend fun getUnclassifiedByMerchantKeyword(keyword: String): List<Transaction> {
        return dao.getUnclassifiedByMerchantKeyword(keyword).map { it.toDomain() }
    }

    override suspend fun deleteAll() {
        dao.deleteAll()
    }

    override fun searchByKeyword(keyword: String): Flow<List<Transaction>> {
        return dao.searchByKeyword(keyword).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getByCategoryAndDateRange(category: String, startTime: Long, endTime: Long): List<Transaction> {
        return dao.getByCategoryAndDateRange(category, startTime, endTime).map { it.toDomain() }
    }

    override suspend fun getByMerchantName(merchant: String): List<Transaction> {
        return dao.getByMerchantName(merchant).map { it.toDomain() }
    }

    override suspend fun getTotalByTypeAndDateRange(type: TransactionType, startTime: Long, endTime: Long): Double {
        return dao.getTotalByTypeAndDateRange(type.name, startTime, endTime)
    }

    override suspend fun getCategorySummary(startTime: Long, endTime: Long): List<com.yxhuang.jizhang.core.database.dao.CategorySummary> {
        return dao.getCategorySummary(startTime, endTime)
    }

    private fun TransactionEntity.toDomain(): Transaction {
        return Transaction(
            id = id,
            amount = amount,
            merchant = merchant,
            category = category,
            type = TransactionType.valueOf(type),
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
            type = type.name,
            timestamp = timestamp,
            sourceApp = sourceApp,
            rawText = rawText,
            createdAt = createdAt
        )
    }
}
