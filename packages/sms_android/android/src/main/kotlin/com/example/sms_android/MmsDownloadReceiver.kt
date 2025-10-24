package com.example.sms_android

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Telephony
import android.util.Log

/**
 * Receiver that is triggered when an MMS download completes.
 */
class MmsDownloadReceiver : BroadcastReceiver() {
    
    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "📬 MMS download completed!")
        
        try {
            val resultCode = resultCode
            Log.d(TAG, "Result code: $resultCode")
            
            // Log all intent extras for debugging
            intent.extras?.keySet()?.forEach { key ->
                Log.d(TAG, "Intent extra: $key = ${intent.extras?.get(key)}")
            }
            
            if (resultCode == android.app.Activity.RESULT_OK) {
                Log.d(TAG, "✅ MMS downloaded successfully")
                
                // Query the most recent INBOX MMS
                val uri = Uri.parse("content://mms")
                val projection = arrayOf("_id", "date", "msg_box", "m_type")
                
                val cursor = context.contentResolver.query(
                    uri,
                    projection,
                    "msg_box = ?",
                    arrayOf("1"), // INBOX
                    "date DESC LIMIT 1"
                )
                
                cursor?.use {
                    if (it.moveToFirst()) {
                        val mmsId = it.getString(it.getColumnIndexOrThrow("_id"))
                        val date = it.getLong(it.getColumnIndexOrThrow("date"))
                        val msgBox = it.getInt(it.getColumnIndexOrThrow("msg_box"))
                        val mType = it.getInt(it.getColumnIndexOrThrow("m_type"))
                        
                        Log.d(TAG, "📨 Downloaded MMS: ID=$mmsId, msg_box=$msgBox, m_type=$mType")
                        
                        // Check if it's very recent (within last 10 seconds)
                        val now = System.currentTimeMillis() / 1000
                        val messageAge = now - date
                        
                        if (messageAge <= 10) {
                            // Get sender and body
                            val address = getAddressNumber(mmsId, context)
                            val body = getMessageText(mmsId, context)
                            
                            if (address != null && body != null) {
                                val timestamp = date * 1000
                                
                                val data = mapOf(
                                    "address" to address,
                                    "body" to body,
                                    "timestamp" to timestamp,
                                    "isMms" to true
                                )
                                
                                SmsAndroidPlugin.incomingEventSink?.success(data)
                                Log.d(TAG, "✅ Notified Flutter of incoming MMS from $address")
                            }
                        } else {
                            Log.d(TAG, "⏰ Message is too old ($messageAge seconds), ignoring")
                        }
                    } else {
                        Log.d(TAG, "⚠️ No INBOX MMS found after download")
                    }
                }
            } else {
                Log.e(TAG, "❌ MMS download failed with result code: $resultCode")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error processing MMS download completion: ${e.message}", e)
        }
    }
    
    /**
     * Get the sender's phone number from an MMS.
     */
    private fun getAddressNumber(mmsId: String, context: Context): String? {
        val uriStr = "content://mms/$mmsId/addr"
        val uri = Uri.parse(uriStr)
        
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        var address: String? = null
        
        cursor?.use {
            while (it.moveToNext()) {
                val typeColumn = it.getColumnIndex("type")
                val addrColumn = it.getColumnIndex("address")
                
                if (typeColumn >= 0 && addrColumn >= 0) {
                    val type = it.getInt(typeColumn)
                    val number = it.getString(addrColumn)
                    
                    // type = 137 means FROM address (sender)
                    if (type == 137 && number != null && !number.contains("insert-address-token")) {
                        address = number
                        break
                    }
                }
            }
        }
        
        return address
    }
    
    /**
     * Get the message text from an MMS, including [IMAGE:...] tags.
     */
    private fun getMessageText(mmsId: String, context: Context): String? {
        val uri = Uri.parse("content://mms/part")
        val selection = "mid=?"
        val selectionArgs = arrayOf(mmsId)
        
        val cursor = context.contentResolver.query(
            uri,
            arrayOf("_id", "ct", "text", "_data"),
            selection,
            selectionArgs,
            null
        )
        
        val textParts = mutableListOf<String>()
        
        cursor?.use {
            while (it.moveToNext()) {
                val contentType = it.getString(it.getColumnIndexOrThrow("ct"))
                
                when {
                    contentType == "text/plain" -> {
                        val textColumn = it.getColumnIndex("text")
                        if (textColumn >= 0) {
                            val text = it.getString(textColumn)
                            if (text != null) {
                                textParts.add(text)
                            }
                        }
                    }
                    contentType?.startsWith("image/") == true -> {
                        val partId = it.getString(it.getColumnIndexOrThrow("_id"))
                        val imageUri = "content://mms/part/$partId"
                        textParts.add("[IMAGE:$imageUri]")
                    }
                }
            }
        }
        
        return if (textParts.isEmpty()) "Picture message" else textParts.joinToString(" ")
    }
    
    companion object {
        private const val TAG = "MmsDownloadReceiver"
    }
}

