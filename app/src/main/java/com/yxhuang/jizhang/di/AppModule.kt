package com.yxhuang.jizhang.di

import com.yxhuang.jizhang.ai.di.aiModule
import com.yxhuang.jizhang.core.database.di.databaseModule
import com.yxhuang.jizhang.feature.capture.di.captureModule
import com.yxhuang.jizhang.feature.analytics.di.analyticsModule
import com.yxhuang.jizhang.feature.classification.di.classificationModule
import com.yxhuang.jizhang.feature.ledger.di.ledgerModule
import com.yxhuang.jizhang.feature.parser.di.parserModule

val appModules = listOf(
    databaseModule,
    parserModule,
    captureModule,
    ledgerModule,
    classificationModule,
    analyticsModule,
    aiModule
)
