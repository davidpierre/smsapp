package com.example.sms_android

import android.app.Activity
import android.app.PendingIntent
import android.app.role.RoleManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.database.ContentObserver
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Telephony
import android.telephony.SmsManager
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import androidx.annotation.NonNull
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry

import com.example.sms_android.ApnSettingsResolver.ApnSettings
import com.example.sms_android.MmsUtils.getAddress
import com.example.sms_android.MmsUtils.getAttachments
import com.example.sms_android.MmsUtils.getTimestamp
import com.example.sms_android.MmsUtils.buildDisplayBody

class SmsAndroidPlugin: FlutterPlugin, MethodCallHandler, ActivityAware, PluginRegistry.ActivityResultListener {
    private lateinit var channel: MethodChannel
    private lateinit var eventChannel: EventChannel
    private var context: Context? = null
    private var activity: Activity? = null
    private var pendingResult: Result? = null
    private var mmsObserver: ContentObserver? = null
    private var lastKnownMmsId: Long = 0

    companion object {
        private const val ROLE_REQUEST_CODE = 1001
        private const val TAG = "SmsAndroidPlugin"
        var incomingEventSink: EventChannel.EventSink? = null
        var lastContext: Context? = null // For MmsReceiverImpl to access
    }

    override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        context = flutterPluginBinding.applicationContext
        lastContext = context // Store for MmsReceiverImpl
        channel = MethodChannel(flutterPluginBinding.binaryMessenger, "sms_android")
        channel.setMethodCallHandler(this)
        
        // Initialize MMS settings for the android-smsmms library (critical for receiving MMS!)
        initializeMmsSettings(flutterPluginBinding.applicationContext)
        
        eventChannel = EventChannel(flutterPluginBinding.binaryMessenger, "sms_android/incoming")
        eventChannel.setStreamHandler(object : EventChannel.StreamHandler {
            override fun onListen(arguments: Any?, events: EventChannel.EventSink?) {
                incomingEventSink = events
                android.util.Log.d(TAG, "EventSink registered")
                
                // Get the last known MMS ID to avoid notifying about old messages
                lastKnownMmsId = getLatestMmsId()
                android.util.Log.d(TAG, "Starting MMS observer from ID: $lastKnownMmsId")
                
                // Register ContentObserver to watch for new MMS
                registerMmsObserver()
            }

            override fun onCancel(arguments: Any?) {
                incomingEventSink = null
                android.util.Log.d(TAG, "EventSink cancelled")
                
                // Unregister ContentObserver
                unregisterMmsObserver()
            }
        })
    }

    override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
        channel.setMethodCallHandler(null)
        eventChannel.setStreamHandler(null)
        context = null
    }

    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        activity = binding.activity
        binding.addActivityResultListener(this)
    }

    override fun onDetachedFromActivity() {
        activity = null
    }

    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
        activity = binding.activity
        binding.addActivityResultListener(this)
    }

    override fun onDetachedFromActivityForConfigChanges() {
        activity = null
    }

    /**
     * Initialize MMS settings in SharedPreferences for the android-smsmms library.
     * This is CRITICAL for MMS receiving - PushReceiver reads these settings when downloading MMS!
     */
    private fun initializeMmsSettings(ctx: Context) {
        try {
            android.util.Log.d(TAG, "🔧 Initializing MMS settings for android-smsmms library...")

            val settings = resolveApnSettings(ctx)

            // The library reads settings from SharedPreferences with this specific name
            val prefs = ctx.getSharedPreferences("com.klinker.android.send_message_preferences", Context.MODE_PRIVATE)
            val editor = prefs.edit()

            editor.putString("mmsc_url", settings.mmsc)
            editor.putString("mms_proxy", settings.proxy ?: "")
            editor.putString("mms_port", settings.port ?: "80")
            editor.putString("user_agent", settings.userAgent ?: "AndroidMms/1.0")

            settings.user?.let { editor.putString("mms_username", it) }
            settings.password?.let { editor.putString("mms_password", it) }

            // Other important settings
            editor.putBoolean("use_system_sending", false) // Use HTTP-based MMS
            editor.putBoolean("delivery_reports", false)
            editor.putBoolean("group_message", false)

            editor.apply()

            android.util.Log.d(TAG, "✅ MMS settings initialized successfully using ${settings.mmsc}")
        } catch (e: Exception) {
            android.util.Log.e(TAG, "❌ Failed to initialize MMS settings: ${e.message}", e)
        }
    }

    private fun resolveApnSettings(ctx: Context): ApnSettings {
        ApnSettingsResolver.resolve(ctx)?.let { resolved ->
            return resolved
        }

        val telephonyManager = ctx.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        val carrierName = telephonyManager.networkOperatorName ?: ""

        return when {
            carrierName.contains("T-Mobile", ignoreCase = true) ||
                    carrierName.contains("Metro", ignoreCase = true) -> {
                android.util.Log.d(TAG, "Using fallback APN for T-Mobile/Metro")
                ApnSettings(
                    mmsc = "http://mms.msg.eng.t-mobile.com/mms/wapenc",
                    proxy = "",
                    port = "80",
                    userAgent = "AndroidMms/1.0",
                    user = null,
                    password = null
                )
            }
            carrierName.contains("AT&T", ignoreCase = true) ||
                    carrierName.contains("Cricket", ignoreCase = true) -> {
                android.util.Log.d(TAG, "Using fallback APN for AT&T/Cricket")
                ApnSettings(
                    mmsc = "http://mmsc.mobile.att.net",
                    proxy = "proxy.mobile.att.net",
                    port = "80",
                    userAgent = "AndroidMms/1.0",
                    user = null,
                    password = null
                )
            }
            carrierName.contains("Verizon", ignoreCase = true) ||
                    carrierName.contains("Visible", ignoreCase = true) -> {
                android.util.Log.d(TAG, "Using fallback APN for Verizon/Visible")
                ApnSettings(
                    mmsc = "http://mms.vtext.com/servlets/mms",
                    proxy = "",
                    port = "80",
                    userAgent = "AndroidMms/1.0",
                    user = null,
                    password = null
                )
            }
            else -> {
                android.util.Log.w(TAG, "Unknown carrier '$carrierName', using generic MMS APN fallback")
                ApnSettings(
                    mmsc = "http://mms.msg.eng.t-mobile.com/mms/wapenc",
                    proxy = "",
                    port = "80",
                    userAgent = "AndroidMms/1.0",
                    user = null,
                    password = null
                )
            }
        }
    }

    override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {
        when (call.method) {
            "isDefaultSmsApp" -> {
                val ctx = context ?: run {
                    result.error("NO_CONTEXT", "Context not available", null)
                    return
                }
                val isDefault = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val roleManager = ctx.getSystemService(RoleManager::class.java)
                    roleManager?.isRoleHeld(RoleManager.ROLE_SMS) ?: false
                } else {
                    Telephony.Sms.getDefaultSmsPackage(ctx) == ctx.packageName
                }
                result.success(isDefault)
            }
            "requestDefaultSmsRole" -> requestDefaultSmsRole(result)
            "sendText" -> {
                val address = call.argument<String>("address")
                val body = call.argument<String>("body")
                if (address != null && body != null) {
                    sendText(address, body, result)
                } else {
                    result.error("INVALID_ARGS", "Address and body required", null)
                }
            }
            "sendMms" -> {
                val addresses = call.argument<List<String>>("addresses")
                val text = call.argument<String>("text")
                val attachments = call.argument<List<Map<String, Any>>>("attachments")
                if (addresses != null) {
                    sendMms(addresses, text, attachments, result)
                } else {
                    result.error("INVALID_ARGS", "Addresses required", null)
                }
            }
            "listThreads" -> {
                val limit = call.argument<Int>("limit") ?: 100
                listThreads(limit, result)
            }
            "listMessages" -> {
                val threadId = call.argument<Int>("threadId")
                val limit = call.argument<Int>("limit") ?: 200
                if (threadId != null) {
                    listMessages(threadId.toLong(), limit, result)
                } else {
                    result.error("INVALID_ARGS", "threadId required", null)
                }
            }
            "readContentUri" -> {
                val uri = call.argument<String>("uri")
                if (uri != null) {
                    readContentUri(uri, result)
                } else {
                    result.error("INVALID_ARGS", "URI required", null)
                }
            }
            else -> result.notImplemented()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
        if (requestCode == ROLE_REQUEST_CODE) {
            android.util.Log.d("SmsAndroidPlugin", "Role request result: ${resultCode == Activity.RESULT_OK} (resultCode=$resultCode)")
            pendingResult?.success(resultCode == Activity.RESULT_OK)
            pendingResult = null
            return true
        }
        return false
    }

    private fun requestDefaultSmsRole(result: Result) {
        val ctx = context ?: run {
            result.error("NO_CONTEXT", "Context not available", null)
            return
        }
        val act = activity ?: run {
            result.error("NO_ACTIVITY", "Activity not available", null)
            return
        }

        android.util.Log.d("SmsAndroidPlugin", "Requesting SMS role, SDK: ${Build.VERSION.SDK_INT}")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = ctx.getSystemService(RoleManager::class.java)
            if (roleManager == null) {
                result.error("NO_ROLE_MANAGER", "RoleManager not available", null)
                return
            }

            if (roleManager.isRoleHeld(RoleManager.ROLE_SMS)) {
                android.util.Log.d("SmsAndroidPlugin", "Already holding SMS role")
                result.success(true)
                return
            }

            android.util.Log.d("SmsAndroidPlugin", "Launching role request intent")
            pendingResult = result
            val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_SMS)
            act.startActivityForResult(intent, ROLE_REQUEST_CODE)
        } else {
            val intent = Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT)
            intent.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, ctx.packageName)
            act.startActivity(intent)
            result.success(true)
        }
    }

    private fun sendText(address: String, body: String, result: Result) {
        val ctx = context ?: run {
            result.error("NO_CONTEXT", "Context not available", null)
            return
        }

        try {
            val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ctx.getSystemService(SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }

            smsManager?.sendTextMessage(address, null, body, null, null)
            android.util.Log.d("SmsAndroidPlugin", "SMS sent to $address, now saving to database...")
            
            saveSentSmsToDatabase(ctx, address, body)
            result.success(true)
        } catch (e: Exception) {
            android.util.Log.e("SmsAndroidPlugin", "sendText failed: ${e.message}", e)
            result.error("SEND_FAILED", e.message, null)
        }
    }
    
    private fun saveSentSmsToDatabase(context: Context, address: String, body: String) {
        try {
            val values = android.content.ContentValues().apply {
                put("address", address)
                put("body", body)
                put("date", System.currentTimeMillis())
                put("read", 1)
                put("type", 2) // 2 = sent
                put("seen", 1)
            }
            val uri = context.contentResolver.insert(android.net.Uri.parse("content://sms/sent"), values)
            if (uri != null) {
                android.util.Log.d("SmsAndroidPlugin", "✅ Saved sent SMS to database: $uri")
            } else {
                android.util.Log.e("SmsAndroidPlugin", "❌ Failed to save sent SMS to database (uri is null)")
            }
        } catch (e: Exception) {
            android.util.Log.e("SmsAndroidPlugin", "❌ Exception saving sent SMS: ${e.message}", e)
        }
    }

    private fun sendMms(
        addresses: List<String>,
        text: String?,
        attachments: List<Map<String, Any>>?,
        result: Result
    ) {
        val ctx = context ?: run {
            result.error("NO_CONTEXT", "Context not available", null)
            return
        }

        try {
            android.util.Log.d("SmsAndroidPlugin", "📤 Sending MMS via android-smsmms library to ${addresses.joinToString(", ")}")
            android.util.Log.d("SmsAndroidPlugin", "Text: $text, Attachments: ${attachments?.size ?: 0}")
            
            // Create Settings instance - configure using resolved APN information
            val settings = com.klinker.android.send_message.Settings().apply {
                useSystemSending = false  // Force HTTP-based MMS
                deliveryReports = false
                group = false
            }

            val apnSettings = resolveApnSettings(ctx)
            settings.mmsc = apnSettings.mmsc
            settings.proxy = apnSettings.proxy ?: ""
            settings.port = apnSettings.port ?: "80"
            settings.setAgent(apnSettings.userAgent ?: "AndroidMms/1.0")

            val smsSubId = SubscriptionManager.getDefaultSmsSubscriptionId()
            if (smsSubId != SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
                settings.setSubscriptionId(smsSubId)
            }

            android.util.Log.d(
                TAG,
                "Using MMS settings mmsc=${settings.mmsc}, proxy=${settings.proxy}, port=${settings.port}, subId=$smsSubId"
            )
            
            // Create MMS message using the library
            val message = com.klinker.android.send_message.Message(text ?: "", addresses.toTypedArray())
            
            // Add image attachments
            attachments?.forEach { attachment ->
                val filePath = attachment["filePath"] as? String
                val uri = attachment["uri"] as? String
                
                val fileUri = when {
                    filePath != null -> android.net.Uri.parse("file://$filePath")
                    uri != null -> android.net.Uri.parse(uri)
                    else -> null
                }
                
                if (fileUri != null) {
                    try {
                        if ("content".equals(fileUri.scheme, ignoreCase = true)) {
                            try {
                                ctx.contentResolver.takePersistableUriPermission(
                                    fileUri,
                                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                                )
                            } catch (e: SecurityException) {
                                android.util.Log.w(TAG, "Unable to persist uri permission for $fileUri: ${e.message}")
                            } catch (e: IllegalArgumentException) {
                                android.util.Log.w(TAG, "Persistable permission not available for $fileUri: ${e.message}")
                            }
                        }
                        // Read file and add as attachment
                        ctx.contentResolver.openInputStream(fileUri)?.use { inputStream ->
                            val bytes = inputStream.readBytes()
                            val contentType = attachment["contentType"] as? String ?: "image/jpeg"
                            message.addMedia(bytes, contentType)
                            android.util.Log.d("SmsAndroidPlugin", "✅ Added attachment: $fileUri ($contentType, ${bytes.size} bytes)")
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("SmsAndroidPlugin", "❌ Failed to read attachment: ${e.message}", e)
                    }
                }
            }
            
            // Register broadcast receiver for MMS send status (critical!)
            val sentReceiver = MmsSentReceiverImpl()
            val sentIntentFilter = android.content.IntentFilter()
            sentIntentFilter.addAction("com.klinker.android.messaging.MMS_SENT")
            ctx.registerReceiver(sentReceiver, sentIntentFilter, android.content.Context.RECEIVER_NOT_EXPORTED)
            android.util.Log.d("SmsAndroidPlugin", "📡 Registered MmsSentReceiverImpl for status updates")
            
            // Send MMS using the library with error handling
            val transaction = com.klinker.android.send_message.Transaction(ctx, settings)
            
            android.util.Log.d("SmsAndroidPlugin", "🚀 Calling sendNewMessage...")
            try {
                val subscriptionId = android.telephony.SubscriptionManager.getDefaultSmsSubscriptionId().toLong()
                android.util.Log.d("SmsAndroidPlugin", "📱 Using subscription ID: $subscriptionId")
                
                transaction.sendNewMessage(message, subscriptionId)
                android.util.Log.d("SmsAndroidPlugin", "✅ sendNewMessage() call completed")
                
                // Unregister receiver after a delay (give it time to receive the broadcast)
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    try {
                        ctx.unregisterReceiver(sentReceiver)
                        android.util.Log.d("SmsAndroidPlugin", "📡 Unregistered MmsSentReceiverImpl")
                    } catch (e: Exception) {
                        android.util.Log.e("SmsAndroidPlugin", "Error unregistering receiver: ${e.message}")
                    }
                }, 10000) // 10 seconds
                
            } catch (e: Exception) {
                android.util.Log.e("SmsAndroidPlugin", "❌ sendNewMessage() threw exception: ${e.message}", e)
                android.util.Log.e("SmsAndroidPlugin", "❌ Exception type: ${e.javaClass.name}")
                e.printStackTrace()
                // Unregister on error
                try {
                    ctx.unregisterReceiver(sentReceiver)
                } catch (ex: Exception) {
                    // Ignore
                }
                throw e  // Re-throw to be caught by outer try-catch
            }
            
            // WORKAROUND: The library creates MMS with OUTBOX (4) or FAILED (5) status
            // Update them to SENT (2) after a delay
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                try {
                    val values = android.content.ContentValues()
                    values.put(android.provider.Telephony.Mms.MESSAGE_BOX, android.provider.Telephony.Mms.MESSAGE_BOX_SENT)
                    
                    // Update both OUTBOX (4) and FAILED (5) MMS to SENT (2)
                    // The library leaves them in these states after sending
                    val updated = ctx.contentResolver.update(
                        android.provider.Telephony.Mms.CONTENT_URI,
                        values,
                        "${android.provider.Telephony.Mms.MESSAGE_BOX} IN (?, ?)",
                        arrayOf("4", "5")  // OUTBOX=4, FAILED=5
                    )
                    android.util.Log.d("SmsAndroidPlugin", "✅ Updated $updated MMS from OUTBOX/FAILED to SENT")
                    
                    if (updated > 0) {
                        // Notify Flutter to refresh after status update
                        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                            incomingEventSink?.success(mapOf(
                                "address" to "System",
                                "body" to "MMS status updated",
                                "timestamp" to System.currentTimeMillis(),
                                "isMms" to true
                            ))
                        }, 100)
                    }
                } catch (e: Exception) {
                    android.util.Log.e("SmsAndroidPlugin", "❌ Failed to update MMS status: ${e.message}", e)
                }
            }, 5000) // 5 second delay to ensure MMS is in database
            
            result.success(true)
            
        } catch (e: Exception) {
            android.util.Log.e("SmsAndroidPlugin", "❌ MMS send failed: ${e.message}", e)
            result.error("MMS_FAILED", e.message, null)
        }
    }

    private fun listThreads(limit: Int, result: Result) {
        val ctx = context ?: run {
            result.error("NO_CONTEXT", "Context not available", null)
            return
        }

        try {
            android.util.Log.d("SmsAndroidPlugin", "listThreads: Starting query for $limit threads")
            
            val allMessages = mutableListOf<Map<String, Any>>()
            
            // Query SMS messages
            val smsUri = Uri.parse("content://sms")
            val smsProjection = arrayOf("_id", "thread_id", "address", "body", "date", "read", "type")
            val smsCursor: Cursor? = ctx.contentResolver.query(
                smsUri,
                smsProjection,
                null,
                null,
                "date DESC"
            )

            smsCursor?.use {
                while (it.moveToNext()) {
                    val threadId = it.getLong(it.getColumnIndexOrThrow("thread_id"))
                    val address = it.getString(it.getColumnIndexOrThrow("address")) ?: "Unknown"
                    val body = it.getString(it.getColumnIndexOrThrow("body")) ?: ""
                    val date = it.getLong(it.getColumnIndexOrThrow("date"))
                    val read = it.getInt(it.getColumnIndexOrThrow("read")) == 1
                    
                    allMessages.add(mapOf(
                        "thread_id" to threadId,
                        "address" to address,
                        "body" to body,
                        "date" to date,
                        "read" to read,
                        "is_mms" to false
                    ))
                }
            }

            // Query MMS messages
            val mmsUri = Uri.parse("content://mms")
            val mmsProjection = arrayOf("_id", "thread_id", "date", "msg_box", "read")
            val mmsCursor: Cursor? = ctx.contentResolver.query(
                mmsUri,
                mmsProjection,
                null,
                null,
                "date DESC"
            )

            mmsCursor?.use {
                while (it.moveToNext()) {
                    val mmsId = it.getLong(it.getColumnIndexOrThrow("_id"))
                    val threadId = it.getLong(it.getColumnIndexOrThrow("thread_id"))
                    val date = it.getLong(it.getColumnIndexOrThrow("date")) * 1000 // MMS date is in seconds
                    val read = it.getInt(it.getColumnIndexOrThrow("read")) == 1
                    
                    val address = getAddress(ctx, mmsId) ?: "Unknown"
                    val body = buildDisplayBody(ctx, mmsId)
                    
                    allMessages.add(mapOf(
                        "thread_id" to threadId,
                        "address" to address,
                        "body" to body,
                        "date" to date,
                        "read" to read,
                        "is_mms" to true
                    ))
                }
            }

            // Sort all messages by date descending
            allMessages.sortByDescending { it["date"] as Long }

            // Group by thread_id and take the most recent message for each thread
            val threads = mutableListOf<Map<String, Any>>()
            val seenThreads = mutableSetOf<Long>()

            for (msg in allMessages) {
                val threadId = msg["thread_id"] as Long
                if (seenThreads.contains(threadId)) continue
                seenThreads.add(threadId)
                
                val messageCount = getMessageCountForThread(ctx, threadId)
                
                threads.add(mapOf(
                    "threadId" to threadId.toInt(),
                    "address" to (msg["address"] as String),
                    "snippet" to (msg["body"] as String).take(100),
                    "messageCount" to messageCount,
                    "date" to (msg["date"] as Long),
                    "isRead" to (msg["read"] as Boolean)
                ))
                
                if (threads.size >= limit) break
            }

            android.util.Log.d("SmsAndroidPlugin", "listThreads: Returning ${threads.size} threads (${allMessages.size} total messages)")
            result.success(threads)
        } catch (e: Exception) {
            android.util.Log.e("SmsAndroidPlugin", "listThreads: Exception: ${e.message}", e)
            result.error("QUERY_FAILED", e.message, null)
        }
    }

    private fun getMessageCountForThread(context: Context, threadId: Long): Int {
        val smsCount = countMessagesInTable(context, "content://sms", threadId)
        val mmsCount = countMessagesInTable(context, "content://mms", threadId)
        return smsCount + mmsCount
    }

    private fun countMessagesInTable(context: Context, uri: String, threadId: Long): Int {
        var count = 0
        val cursor = context.contentResolver.query(
            Uri.parse(uri),
            arrayOf("_id"),
            "thread_id = ?",
            arrayOf(threadId.toString()),
            null
        )
        
        cursor?.use {
            count = it.count
        }
        
        return count
    }

    private fun listMessages(threadId: Long, limit: Int, result: Result) {
        val ctx = context ?: run {
            result.error("NO_CONTEXT", "Context not available", null)
            return
        }

        try {
            android.util.Log.d("SmsAndroidPlugin", "📋 listMessages: thread=$threadId, limit=$limit")
            val messages = mutableListOf<Map<String, Any>>()

            // Query SMS messages
            val smsUri = Uri.parse("content://sms")
            val smsProjection = arrayOf("_id", "thread_id", "address", "body", "date", "type", "read")
            val smsCursor: Cursor? = ctx.contentResolver.query(
                smsUri,
                smsProjection,
                "thread_id = ?",
                arrayOf(threadId.toString()),
                "date DESC"
            )

            var smsCount = 0
            smsCursor?.use {
                while (it.moveToNext()) {
                    val id = it.getInt(it.getColumnIndexOrThrow("_id"))
                    val thread = it.getLong(it.getColumnIndexOrThrow("thread_id"))
                    val address = it.getString(it.getColumnIndexOrThrow("address")) ?: "Unknown"
                    val body = it.getString(it.getColumnIndexOrThrow("body")) ?: ""
                    val date = it.getLong(it.getColumnIndexOrThrow("date"))
                    val type = it.getInt(it.getColumnIndexOrThrow("type"))
                    val read = it.getInt(it.getColumnIndexOrThrow("read")) == 1

                    messages.add(
                        mapOf(
                            "id" to id,
                            "threadId" to thread.toInt(),
                            "address" to address,
                            "body" to body,
                            "date" to date,
                            "type" to type,
                            "isRead" to read,
                            "isMms" to false
                        )
                    )
                    smsCount++
                }
            }
            android.util.Log.d("SmsAndroidPlugin", "📋 Found $smsCount SMS messages")

            // Query MMS messages
            val mmsUri = Uri.parse("content://mms")
            val mmsProjection = arrayOf("_id", "thread_id", "date", "msg_box", "read")
            
            val mmsCursor: Cursor? = ctx.contentResolver.query(
                mmsUri,
                mmsProjection,
                "thread_id = ?",
                arrayOf(threadId.toString()),
                "date DESC"
            )

            mmsCursor?.use {
                while (it.moveToNext()) {
                    val id = it.getLong(it.getColumnIndexOrThrow("_id"))
                    val thread = it.getLong(it.getColumnIndexOrThrow("thread_id"))
                    val date = it.getLong(it.getColumnIndexOrThrow("date")) * 1000 // MMS date is in seconds
                    val msgBox = it.getInt(it.getColumnIndexOrThrow("msg_box"))
                    val read = it.getInt(it.getColumnIndexOrThrow("read")) == 1
                    
                    android.util.Log.d("SmsAndroidPlugin", "📨 Processing MMS ID: $id, msg_box: $msgBox")
                    
                    val address = getAddress(ctx, id) ?: "Unknown"
                    val body = buildDisplayBody(ctx, id)
                    val attachments = getAttachments(ctx, id)
                    
                    // Type: 1 = inbox, 2 = sent, based on msg_box
                    val type = if (msgBox == Telephony.Mms.MESSAGE_BOX_INBOX) 1 else 2

                    messages.add(
                        mapOf(
                            "id" to id.toInt(),
                            "threadId" to thread.toInt(),
                            "address" to address,
                            "body" to body,
                            "date" to date,
                            "type" to type,
                            "isRead" to read,
                            "isMms" to true,
                            "attachments" to attachments
                        )
                    )
                }
            }
            android.util.Log.d("SmsAndroidPlugin", "📋 Found ${messages.size - smsCount} MMS messages")

            // Sort by date descending
            messages.sortByDescending { it["date"] as Long }

            android.util.Log.d("SmsAndroidPlugin", "📋 Returning ${messages.take(limit).size} messages total")
            result.success(messages.take(limit))
        } catch (e: Exception) {
            android.util.Log.e("SmsAndroidPlugin", "listMessages: Exception: ${e.message}", e)
            result.error("QUERY_FAILED", e.message, null)
        }
    }

    private fun readContentUri(uriString: String, result: Result) {
        val ctx = context ?: run {
            android.util.Log.e("SmsAndroidPlugin", "❌ readContentUri: Context not available")
            result.error("NO_CONTEXT", "Context not available", null)
            return
        }

        try {
            android.util.Log.d("SmsAndroidPlugin", "📖 Reading content URI: $uriString")
            val uri = Uri.parse(uriString)
            val inputStream = ctx.contentResolver.openInputStream(uri)
            
            if (inputStream == null) {
                android.util.Log.e("SmsAndroidPlugin", "❌ Could not open input stream for: $uriString")
                result.error("READ_FAILED", "Could not open content URI", null)
                return
            }

            val bytes = inputStream.readBytes()
            inputStream.close()
            
            android.util.Log.d("SmsAndroidPlugin", "✅ Read ${bytes.size} bytes from $uriString")
            result.success(bytes)
        } catch (e: Exception) {
            android.util.Log.e("SmsAndroidPlugin", "❌ Error reading content URI $uriString: ${e.message}", e)
            result.error("READ_FAILED", e.message, null)
        }
    }

    /**
     * Register ContentObserver to watch for new MMS in the database.
     * The system will automatically download and save MMS - we just observe.
     */
    private fun registerMmsObserver() {
        val ctx = context ?: return
        
        mmsObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                onChange(selfChange, null)
            }
            
            override fun onChange(selfChange: Boolean, uri: Uri?) {
                android.util.Log.d(TAG, "📊 MMS database changed: $uri")
                checkForNewMms()
            }
        }
        
        // Watch the MMS content URI
        ctx.contentResolver.registerContentObserver(
            Uri.parse("content://mms"),
            true,
            mmsObserver!!
        )
        
        android.util.Log.d(TAG, "✅ MMS ContentObserver registered")
    }
    
    /**
     * Unregister ContentObserver.
     */
    private fun unregisterMmsObserver() {
        val ctx = context ?: return
        mmsObserver?.let {
            ctx.contentResolver.unregisterContentObserver(it)
            android.util.Log.d(TAG, "✅ MMS ContentObserver unregistered")
        }
        mmsObserver = null
    }
    
    /**
     * Get the ID of the most recent MMS.
     */
    private fun getLatestMmsId(): Long {
        val ctx = context ?: return 0
        
        try {
            val cursor = ctx.contentResolver.query(
                Uri.parse("content://mms"),
                arrayOf("_id"),
                null,
                null,
                "date DESC LIMIT 1"
            )
            
            cursor?.use {
                if (it.moveToFirst()) {
                    return it.getLong(it.getColumnIndexOrThrow("_id"))
                }
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error getting latest MMS ID: ${e.message}", e)
        }
        
        return 0
    }
    
    /**
     * Check for new MMS messages and notify Flutter.
     */
    private fun checkForNewMms() {
        val ctx = context ?: return
        
        try {
            // Query for ALL new MMS in INBOX (both notifications and downloaded)
            val cursor = ctx.contentResolver.query(
                Uri.parse("content://mms"),
                arrayOf("_id", "date", "msg_box", "m_type", "ct_l"), // ct_l = content_location
                "msg_box = ? AND _id > ?",
                arrayOf(
                    Telephony.Mms.MESSAGE_BOX_INBOX.toString(),
                    lastKnownMmsId.toString()
                ),
                "date DESC"
            )
            
            cursor?.use {
                while (it.moveToNext()) {
                    val mmsId = it.getLong(it.getColumnIndexOrThrow("_id"))
                    val date = it.getLong(it.getColumnIndexOrThrow("date"))
                    val msgBox = it.getInt(it.getColumnIndexOrThrow("msg_box"))
                    val mType = it.getInt(it.getColumnIndexOrThrow("m_type"))
                    val contentLocation = try {
                        it.getString(it.getColumnIndexOrThrow("ct_l"))
                    } catch (e: Exception) {
                        null
                    }
                    
                    android.util.Log.d(TAG, "📨 New MMS detected: ID=$mmsId, msg_box=$msgBox, m_type=$mType (130=notification, 132=downloaded), date=$date, content_url=$contentLocation")
                    
                    when (mType) {
                        130 -> {
                            // m-notification-ind: Notification waiting to be downloaded
                            // The PushReceiver has already been called and should handle download automatically
                            // We just wait for it to change to m_type=132 (fully downloaded)
                            android.util.Log.d(TAG, "📥 MMS notification detected (m_type=130) - PushReceiver should auto-download")
                            android.util.Log.d(TAG, "📍 Content location: $contentLocation")
                            android.util.Log.d(TAG, "⏳ Waiting for auto-download to complete (will become m_type=132)...")
                        }
                        132 -> {
                            // m-retrieve-conf: Successfully downloaded MMS
                            android.util.Log.d(TAG, "✅ MMS fully downloaded - notifying Flutter")
                            
                            // Extract sender and body
                            val address = getAddress(ctx, mmsId) ?: "Unknown"
                            val body = buildDisplayBody(ctx, mmsId)
                            val attachments = getAttachments(ctx, mmsId)
                            val timestamp = getTimestamp(ctx, mmsId) ?: (date * 1000)

                            val payload = mutableMapOf<String, Any>(
                                "address" to address,
                                "body" to body,
                                "timestamp" to timestamp,
                                "isMms" to true
                            )
                            if (attachments.isNotEmpty()) {
                                payload["attachments"] = attachments
                            }

                            incomingEventSink?.success(payload)

                            android.util.Log.d(TAG, "✅ Notified Flutter of new MMS from $address: $body")
                        }
                        else -> {
                            android.util.Log.d(TAG, "ℹ️ MMS with unknown m_type=$mType")
                        }
                    }
                    
                    // Update last known ID
                    if (mmsId > lastKnownMmsId) {
                        lastKnownMmsId = mmsId
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "❌ Error checking for new MMS: ${e.message}", e)
        }
    }
}
