package com.example.tickerdemo

import android.app.Application
import com.example.tickerdemo.di.appModules
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class TickerDemoApp : Application() {

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@TickerDemoApp)
            modules(appModules)
        }
    }
}
