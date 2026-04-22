package com.yxhuang.jizhang.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "category_rules")
data class CategoryRuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val keyword: String,
    val category: String,
    val confidence: Float = 1.0f,
    val matchType: String = "CONTAINS"
)
