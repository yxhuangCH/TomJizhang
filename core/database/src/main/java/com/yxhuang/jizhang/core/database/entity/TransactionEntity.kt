package com.yxhuang.jizhang.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amount: Double,
    val merchant: String,
    val category: String?,
    val timestamp: Long,
    val sourceApp: String,
    val rawText: String,
    val createdAt: Long = System.currentTimeMillis()
)
