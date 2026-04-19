package com.yxhuang.jizhang.di

import com.yxhuang.jizhang.core.database.di.databaseModule
import com.yxhuang.jizhang.feature.capture.di.captureModule
import com.yxhuang.jizhang.feature.ledger.di.ledgerModule
import com.yxhuang.jizhang.feature.parser.di.parserModule

val appModules = listOf(
    databaseModule,
    parserModule,
    captureModule,
    ledgerModule
)
