package com.yxhuang.jizhang.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.yxhuang.jizhang.core.database.entity.ParseFailureLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ParseFailureLogDao {
    @Query("SELECT * FROM parse_failure_logs ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<ParseFailureLogEntity>>

    @Insert
    suspend fun insert(entity: ParseFailureLogEntity): Long

    @Query("SELECT * FROM parse_failure_logs WHERE id = :id")
    suspend fun getById(id: Long): ParseFailureLogEntity?
}
