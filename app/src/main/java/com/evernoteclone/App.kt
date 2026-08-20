package com.evernoteclone

import android.app.Application
import com.evernoteclone.util.NotificationHelper

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        NotificationHelper.ensureChannel(this)
    }
}
