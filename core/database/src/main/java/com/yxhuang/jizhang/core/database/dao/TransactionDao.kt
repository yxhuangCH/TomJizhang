package com.yxhuang.jizhang.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.yxhuang.jizhang.core.database.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    suspend fun getAll(): List<TransactionEntity>

    @Insert
    suspend fun insert(entity: TransactionEntity): Long

    @Update
    suspend fun update(entity: TransactionEntity)

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getById(id: Long): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE category IS NULL AND merchant LIKE '%' || :keyword || '%'")
    suspend fun getUnclassifiedByMerchantKeyword(keyword: String): List<TransactionEntity>

    @Query("DELETE FROM transactions")
    suspend fun deleteAll()

    // ===== Phase 3: indexed queries =====

    @Query("""
        SELECT * FROM transactions
        WHERE merchant LIKE '%' || :keyword || '%'
        ORDER BY timestamp DESC
    """)
    fun searchByKeyword(keyword: String): Flow<List<TransactionEntity>>

    @Query("""
        SELECT * FROM transactions
        WHERE category = :category
        AND timestamp BETWEEN :startTime AND :endTime
        ORDER BY timestamp DESC
    """)
    suspend fun getByCategoryAndDateRange(
        category: String,
        startTime: Long,
        endTime: Long
    ): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE merchant = :merchant ORDER BY timestamp ASC")
    suspend fun getByMerchantName(merchant: String): List<TransactionEntity>

    @Query("""
        SELECT COALESCE(SUM(amount), 0.0) FROM transactions
        WHERE type = :type
        AND timestamp BETWEEN :startTime AND :endTime
    """)
    suspend fun getTotalByTypeAndDateRange(type: String, startTime: Long, endTime: Long): Double

    @Query("""
        SELECT category, SUM(amount) as total, COUNT(*) as count FROM transactions
        WHERE type = 'EXPENSE'
        AND timestamp BETWEEN :startTime AND :endTime
        GROUP BY category
        ORDER BY total DESC
    """)
    suspend fun getCategorySummary(startTime: Long, endTime: Long): List<CategorySummary>
}
