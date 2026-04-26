package com.yxhuang.jizhang.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.yxhuang.jizhang.core.database.dao.BudgetDao
import com.yxhuang.jizhang.core.database.dao.CategoryRuleDao
import com.yxhuang.jizhang.core.database.dao.ParseFailureLogDao
import com.yxhuang.jizhang.core.database.dao.TransactionDao
import com.yxhuang.jizhang.core.database.entity.BudgetEntity
import com.yxhuang.jizhang.core.database.entity.CategoryRuleEntity
import com.yxhuang.jizhang.core.database.entity.ParseFailureLogEntity
import com.yxhuang.jizhang.core.database.entity.TransactionEntity

@Database(
    entities = [
        TransactionEntity::class,
        CategoryRuleEntity::class,
        ParseFailureLogEntity::class,
        BudgetEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class JizhangDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun categoryRuleDao(): CategoryRuleDao
    abstract fun parseFailureLogDao(): ParseFailureLogDao
    abstract fun budgetDao(): BudgetDao
}
