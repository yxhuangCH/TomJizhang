package com.yxhuang.jizhang.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.yxhuang.jizhang.core.database.entity.CategoryRuleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryRuleDao {
    @Query("SELECT * FROM category_rules")
    fun observeAll(): Flow<List<CategoryRuleEntity>>

    @Query("SELECT * FROM category_rules")
    suspend fun getAll(): List<CategoryRuleEntity>

    @Insert
    suspend fun insert(entity: CategoryRuleEntity): Long

    @Query("SELECT * FROM category_rules WHERE id = :id")
    suspend fun getById(id: Long): CategoryRuleEntity?

    @Query("SELECT COUNT(*) FROM category_rules")
    suspend fun count(): Int

    @Query("DELETE FROM category_rules")
    suspend fun deleteAll()
}
