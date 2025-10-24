package com.example.smsapp

import android.content.Intent
import android.telephony.TelephonyManager
import androidx.core.app.JobIntentService

/**
 * Headless service required for default SMS app on API 29+.
 * Handles quick reply functionality from notifications.
 */
class HeadlessSmsSendService : JobIntentService() {
    
    companion object {
        private const val TAG = "HeadlessSmsSendService"
    }

    override fun onHandleWork(intent: Intent) {
        if (intent.action == TelephonyManager.ACTION_RESPOND_VIA_MESSAGE) {
            android.util.Log.d(TAG, "RESPOND_VIA_MESSAGE received")
            // The telephony plugin will handle the actual message sending
        }
    }
}

