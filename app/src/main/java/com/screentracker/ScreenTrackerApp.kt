package com.screentracker

import android.app.Application
import com.screentracker.util.NotificationHelper

class ScreenTrackerApp : Application() {

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createNotificationChannels(this)
    }
}
