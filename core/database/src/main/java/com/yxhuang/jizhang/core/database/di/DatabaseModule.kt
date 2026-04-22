package com.yxhuang.jizhang.core.database.di

import androidx.room.Room
import com.yxhuang.jizhang.core.database.JizhangDatabase
import com.yxhuang.jizhang.core.database.migration.Migration_1_2
import com.yxhuang.jizhang.core.database.repository.CategoryRuleRepository
import com.yxhuang.jizhang.core.database.repository.CategoryRuleRepositoryImpl
import com.yxhuang.jizhang.core.database.repository.ParseFailureRepository
import com.yxhuang.jizhang.core.database.repository.ParseFailureRepositoryImpl
import com.yxhuang.jizhang.core.database.repository.TransactionRepository
import com.yxhuang.jizhang.core.database.repository.TransactionRepositoryImpl
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val databaseModule = module {
    single {
        Room.databaseBuilder(
            androidContext(),
            JizhangDatabase::class.java,
            "jizhang.db"
        )
            .addMigrations(Migration_1_2)
            .build()
    }
    single { get<JizhangDatabase>().transactionDao() }
    single { get<JizhangDatabase>().categoryRuleDao() }
    single { get<JizhangDatabase>().parseFailureLogDao() }
    single<TransactionRepository> { TransactionRepositoryImpl(get()) }
    single<CategoryRuleRepository> { CategoryRuleRepositoryImpl(get()) }
    single<ParseFailureRepository> { ParseFailureRepositoryImpl(get()) }
}
