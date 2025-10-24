package com.example.sms_android

import android.app.Service
import android.content.Intent
import android.os.IBinder

class HeadlessSmsSendService : Service() {
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // This service is required for apps targeting API 29+ to be default SMS app
        // It handles "respond via message" when the device is locked
        android.util.Log.d("HeadlessSmsSendService", "Handling respond via message intent")
        
        // Process the intent if needed
        intent?.let {
            android.util.Log.d("HeadlessSmsSendService", "Action: ${it.action}")
        }
        
        stopSelf(startId)
        return START_NOT_STICKY
    }
}

