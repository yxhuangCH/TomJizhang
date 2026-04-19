package com.yxhuang.jizhang.feature.parser.di

import com.yxhuang.jizhang.feature.parser.TransactionParser
import org.koin.dsl.module

val parserModule = module {
    single { TransactionParser }
}
