package com.example.sms_android

import android.app.Service
import android.content.Intent
import android.net.Uri
import android.os.IBinder
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Background service for downloading MMS using android-smsmms library.
 * Uses the same library that successfully sends MMS.
 */
class MmsDownloadService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO)

    companion object {
        private const val TAG = "MmsDownloadService"
        const val ACTION_DOWNLOAD_MMS = "com.example.sms_android.DOWNLOAD_MMS"
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "🚀 MmsDownloadService started")
        
        if (intent?.action == ACTION_DOWNLOAD_MMS) {
            val pdu = intent.getByteArrayExtra("data")
            
            if (pdu != null) {
                Log.d(TAG, "📦 Received PDU data (${pdu.size} bytes)")
                
                serviceScope.launch {
                    try {
                        downloadMmsWithLibrary(pdu)
                    } catch (e: Exception) {
                        Log.e(TAG, "❌ Error in MMS download: ${e.message}", e)
                    } finally {
                        stopSelf(startId)
                    }
                }
            } else {
                Log.e(TAG, "❌ No PDU data in intent")
                stopSelf(startId)
            }
        } else {
            stopSelf(startId)
        }
        
        return START_NOT_STICKY
    }

    /**
     * Download MMS - wait for library to handle it automatically, then query database.
     * The android-smsmms library downloads MMS in the background automatically.
     */
    private suspend fun downloadMmsWithLibrary(pdu: ByteArray) {
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "⏳ Waiting for android-smsmms library to download MMS automatically...")
                
                // The library's MmsReceivedReceiver (in AndroidManifest.xml) handles downloads automatically
                // Just wait for it to complete and query the database
                delay(5000)  // Give the library time to download
                
                // Query database to find the downloaded MMS and notify Flutter
                Log.d(TAG, "🔍 Querying database for downloaded MMS...")
                notifyFlutterOfNewMms()
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ MMS download failed: ${e.message}", e)
            }
        }
    }
    
    /**
     * Query for the most recent INBOX MMS and notify Flutter.
     */
    private fun notifyFlutterOfNewMms() {
        try {
            val uri = Uri.parse("content://mms")
            val projection = arrayOf("_id", "date", "msg_box", "m_type")
            
            // First, log ALL recent MMS records to see what's in the database
            Log.d(TAG, "📊 Checking ALL recent MMS records in database...")
            val allCursor = applicationContext.contentResolver.query(
                uri,
                projection,
                null, // No filter - get ALL MMS
                null,
                "date DESC LIMIT 5"
            )
            allCursor?.use {
                var count = 0
                while (it.moveToNext()) {
                    val id = it.getString(it.getColumnIndexOrThrow("_id"))
                    val msgBox = it.getInt(it.getColumnIndexOrThrow("msg_box"))
                    val mType = it.getInt(it.getColumnIndexOrThrow("m_type"))
                    val date = it.getLong(it.getColumnIndexOrThrow("date"))
                    Log.d(TAG, "  MMS #${++count}: ID=$id, msg_box=$msgBox, m_type=$mType, date=$date")
                }
                if (count == 0) {
                    Log.d(TAG, "  No MMS records found at all!")
                }
            }
            
            // Now look for successfully downloaded MMS (m_type=132)
            val cursor = applicationContext.contentResolver.query(
                uri,
                projection,
                "msg_box = ? AND m_type = ?",
                arrayOf("1", "132"), // INBOX and m-retrieve-conf (successfully downloaded)
                "date DESC LIMIT 1"
            )
            
            cursor?.use {
                if (it.moveToFirst()) {
                    val mmsId = it.getString(it.getColumnIndexOrThrow("_id"))
                    val date = it.getLong(it.getColumnIndexOrThrow("date"))
                    val msgBox = it.getInt(it.getColumnIndexOrThrow("msg_box"))
                    val mType = it.getInt(it.getColumnIndexOrThrow("m_type"))
                    
                    Log.d(TAG, "📨 Found downloaded MMS: ID=$mmsId, msg_box=$msgBox, m_type=$mType")
                    
                    // Extract sender address and message body
                    val address = getAddressNumber(mmsId)
                    val body = getMessageText(mmsId)
                    
                    if (address != null && body != null) {
                        SmsAndroidPlugin.incomingEventSink?.success(mapOf(
                            "address" to address,
                            "body" to body,
                            "timestamp" to date,
                            "isMms" to true
                        ))
                        Log.d(TAG, "✅ Notified Flutter of incoming MMS from $address")
                    } else {
                        Log.w(TAG, "⚠️ Could not extract address or body from MMS")
                    }
                } else {
                    Log.w(TAG, "⚠️ No downloaded MMS found in database")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error notifying Flutter of new MMS: ${e.message}", e)
        }
    }

    /**
     * Get the sender's phone number from an MMS.
     */
    private fun getAddressNumber(mmsId: String): String? {
        val uriStr = "content://mms/$mmsId/addr"
        val uri = Uri.parse(uriStr)
        
        val cursor = applicationContext.contentResolver.query(uri, null, null, null, null)
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
    private fun getMessageText(mmsId: String): String? {
        val uri = Uri.parse("content://mms/part")
        val selection = "mid=?"
        val selectionArgs = arrayOf(mmsId)
        
        val cursor = applicationContext.contentResolver.query(
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
}
