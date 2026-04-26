package com.yxhuang.jizhang.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transactions",
    indices = [
        Index(value = ["type"]),
        Index(value = ["merchant"]),
        Index(value = ["category", "timestamp"])
    ]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amount: Double,
    val merchant: String,
    val category: String?,
    @ColumnInfo(name = "type") val type: String = "EXPENSE",
    val timestamp: Long,
    val sourceApp: String,
    val rawText: String,
    val createdAt: Long = System.currentTimeMillis()
)
