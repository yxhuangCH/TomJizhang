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

    @Insert
    suspend fun insert(entity: CategoryRuleEntity): Long

    @Query("SELECT * FROM category_rules WHERE id = :id")
    suspend fun getById(id: Long): CategoryRuleEntity?
}
