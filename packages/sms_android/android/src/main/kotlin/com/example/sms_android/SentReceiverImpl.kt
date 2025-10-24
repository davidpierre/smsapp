package com.example.sms_android

import android.content.Context
import android.content.Intent
import android.util.Log
import com.klinker.android.send_message.SentReceiver

/**
 * Receiver to handle SMS/MMS send status updates from the android-smsmms library.
 * This receiver updates the SMS status in the database to type=2 (sent) or type=5 (failed).
 */
class SentReceiverImpl : SentReceiver() {
    
    private val TAG = "SentReceiverImpl"
    
    override fun updateInInternalDatabase(context: Context, intent: Intent, resultCode: Int) {
        Log.d(TAG, "📤 SMS send status update - resultCode: $resultCode")
        
        // Call the parent class to handle the actual database update
        // This updates the SMS type from OUTBOX to SENT (2) or FAILED (5)
        super.updateInInternalDatabase(context, intent, resultCode)
        
        Log.d(TAG, "✅ SMS status updated in database")
    }
    
    override fun onMessageStatusUpdated(context: Context?, intent: Intent?, receiverResultCode: Int) {
        Log.d(TAG, "📤 SMS message status callback - resultCode: $receiverResultCode")
        // Optional: Add custom logic here after status update
    }
}
