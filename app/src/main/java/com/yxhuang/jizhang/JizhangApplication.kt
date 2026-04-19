package com.yxhuang.jizhang

import android.app.Application
import com.yxhuang.jizhang.di.appModules
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin

class JizhangApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        if (GlobalContext.getOrNull() == null) {
            startKoin {
                androidContext(this@JizhangApplication)
                modules(appModules)
            }
        }
    }
}
