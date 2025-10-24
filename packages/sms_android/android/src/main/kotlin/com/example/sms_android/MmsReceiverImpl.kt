package com.example.sms_android

import android.content.Context
import android.content.Intent
import android.util.Log
import com.android.mms.transaction.PushReceiver

/**
 * MMS WAP_PUSH receiver that extends PushReceiver from android-smsmms library.
 * 
 * The parent PushReceiver class from android-smsmms library handles:
 * - Parsing WAP_PUSH PDU data
 * - Persisting MMS notifications to the database
 * - Triggering automatic MMS downloads
 * 
 * This class adds logging and ensures the library's auto-download is triggered.
 */
class MmsReceiverImpl : PushReceiver() {
    
    companion object {
        private const val TAG = "MmsReceiverImpl"
    }
    
    override fun onReceive(context: Context?, intent: Intent?) {
        Log.d(TAG, "📱 WAP_PUSH_DELIVER received!")
        
        if (context == null || intent == null) {
            Log.e(TAG, "❌ Context or Intent is null!")
            return
        }
        
        try {
            // Log intent details
            Log.d(TAG, "Action: ${intent.action}")
            Log.d(TAG, "Data: ${intent.data}")
            
            val pdu = intent.getByteArrayExtra("data")
            if (pdu != null) {
                Log.d(TAG, "📦 PDU data received: ${pdu.size} bytes")
            } else {
                Log.w(TAG, "⚠️ No PDU data in intent!")
            }
            
            // Call parent class to handle MMS notification and trigger download
            // The library will automatically:
            // 1. Parse the PDU
            // 2. Save the MMS notification to the database
            // 3. Trigger automatic download in the background
            super.onReceive(context, intent)
            
            Log.d(TAG, "✅ PushReceiver.onReceive() completed - download should be initiated")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error in onReceive: ${e.message}", e)
            e.printStackTrace()
        }
    }
}
