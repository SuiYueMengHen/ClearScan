package com.clearscan

import android.app.Application

class ClearScanApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppLogger.init(this)
        AppLogger.i("Application", "ClearScanApp started")
    }
}
