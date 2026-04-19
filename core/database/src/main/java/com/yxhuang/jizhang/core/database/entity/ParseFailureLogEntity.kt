package com.yxhuang.jizhang.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "parse_failure_logs")
data class ParseFailureLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val rawText: String,
    val sourceApp: String,
    val timestamp: Long,
    val reason: String?
)
