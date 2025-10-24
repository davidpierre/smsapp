package com.example.sms_android

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Telephony

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        android.util.Log.d("SmsReceiver", "onReceive called, action: ${intent.action}")
        if (intent.action == Telephony.Sms.Intents.SMS_DELIVER_ACTION) {
            android.util.Log.d("SmsReceiver", "Processing SMS_DELIVER_ACTION")
            val messages = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                Telephony.Sms.Intents.getMessagesFromIntent(intent)
            } else {
                return
            }

            messages?.forEach { smsMessage ->
                val address = smsMessage.displayOriginatingAddress
                val body = smsMessage.messageBody
                val timestamp = smsMessage.timestampMillis

                android.util.Log.d("SmsReceiver", "SMS from $address: $body")
                
                // As the default SMS app, WE must write received messages to the database
                saveReceivedSmsToDatabase(context, address, body, timestamp)

                val data = mapOf(
                    "address" to address,
                    "body" to body,
                    "timestamp" to timestamp,
                    "isMms" to false
                )

                // Send to Flutter via EventChannel
                if (SmsAndroidPlugin.incomingEventSink != null) {
                    android.util.Log.d("SmsReceiver", "Sending to Flutter EventSink")
                    SmsAndroidPlugin.incomingEventSink?.success(data)
                } else {
                    android.util.Log.e("SmsReceiver", "EventSink is null! Flutter not listening")
                }
            }
        }
    }
    
    private fun saveReceivedSmsToDatabase(context: Context, address: String, body: String, timestamp: Long) {
        try {
            val values = android.content.ContentValues().apply {
                put("address", address)
                put("body", body)
                put("date", timestamp)
                put("date_sent", timestamp)
                put("read", 0) // Mark as unread
                put("type", 1) // 1 = received
                put("seen", 0) // Not seen yet
            }
            
            val uri = context.contentResolver.insert(
                Uri.parse("content://sms/inbox"),
                values
            )
            
            if (uri != null) {
                android.util.Log.d("SmsReceiver", "✅ Saved received SMS to database: $uri")
            } else {
                android.util.Log.e("SmsReceiver", "❌ Failed to save received SMS to database (uri is null)")
            }
        } catch (e: Exception) {
            android.util.Log.e("SmsReceiver", "❌ Exception saving received SMS: ${e.message}", e)
        }
    }
}

