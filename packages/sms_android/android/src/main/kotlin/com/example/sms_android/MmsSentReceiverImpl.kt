package com.example.sms_android

import android.content.Context
import android.content.Intent
import android.util.Log
import com.klinker.android.send_message.MmsSentReceiver

/**
 * Receiver to handle MMS send status updates from the android-smsmms library.
 * This receiver updates the MMS status in the database from OUTBOX (4) to SENT (2) or FAILED (5).
 */
class MmsSentReceiverImpl : MmsSentReceiver() {
    
    private val TAG = "MmsSentReceiverImpl"
    
    override fun updateInInternalDatabase(context: Context, intent: Intent, resultCode: Int) {
        Log.d(TAG, "📤 MMS send status update - resultCode: $resultCode")
        
        // Call the parent class to handle the actual database update
        // This updates MESSAGE_BOX from OUTBOX (4) to SENT (2) or FAILED (5)
        super.updateInInternalDatabase(context, intent, resultCode)
        
        Log.d(TAG, "✅ MMS status updated in database")
    }
    
    override fun onMessageStatusUpdated(context: Context?, intent: Intent?, receiverResultCode: Int) {
        Log.d(TAG, "📤 MMS message status callback - resultCode: $receiverResultCode")
        // Optional: Add custom logic here after status update
    }
}
