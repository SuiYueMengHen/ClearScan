package com.clearscan

import android.app.Application

class ClearScanApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppLogger.init(this)
        AppLogger.i("Application", "ClearScanApp started")
        val prefs = getSharedPreferences("clearscan-settings", MODE_PRIVATE)
        GitHubUpdateRepository.schedule(this, prefs.getBoolean("autoCheckUpdates", true), prefs.getBoolean("wifiOnlyUpdates", true))
    }
}
