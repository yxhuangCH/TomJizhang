package com.yxhuang.jizhang.core.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val Migration_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE category_rules ADD COLUMN matchType TEXT NOT NULL DEFAULT 'CONTAINS'")
    }
}
