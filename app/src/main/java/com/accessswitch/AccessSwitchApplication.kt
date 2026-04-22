package com.accessswitch

import android.app.Application
import com.accessswitch.util.StartupLogger
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class AccessSwitchApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        StartupLogger.log("Application: onCreate — Hilt component initializing")
    }
}
