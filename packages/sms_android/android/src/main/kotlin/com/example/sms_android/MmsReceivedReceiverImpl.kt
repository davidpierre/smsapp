package com.example.sms_android

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.util.Log
import com.klinker.android.send_message.MmsReceivedReceiver

import com.example.sms_android.MmsUtils.buildDisplayBody
import com.example.sms_android.MmsUtils.getAddress
import com.example.sms_android.MmsUtils.getAttachments
import com.example.sms_android.MmsUtils.getTimestamp

/**
 * Receives callbacks from the android-smsmms library when an MMS download completes.
 * Converts the download into an event that is forwarded to Flutter.
 */
class MmsReceivedReceiverImpl : MmsReceivedReceiver() {

    override fun onMessageReceived(context: Context, messageUri: Uri) {
        Log.d(TAG, "MMS retrieved: $messageUri")
        try {
            val mmsId = ContentUris.parseId(messageUri)
            val address = getAddress(context, mmsId) ?: "Unknown"
            val body = buildDisplayBody(context, mmsId)
            val attachments = getAttachments(context, mmsId)
            val timestamp = getTimestamp(context, mmsId) ?: System.currentTimeMillis()

            val payload = mutableMapOf<String, Any>(
                "address" to address,
                "body" to body,
                "timestamp" to timestamp,
                "isMms" to true
            )
            if (attachments.isNotEmpty()) {
                payload["attachments"] = attachments
            }

            SmsAndroidPlugin.incomingEventSink?.success(payload)
            Log.d(TAG, "Forwarded MMS to Flutter: address=$address, attachments=${attachments.size}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed processing downloaded MMS: ${e.message}", e)
        }
    }

    override fun onError(context: Context, error: String) {
        Log.e(TAG, "MMS download error: $error")
    }

    companion object {
        private const val TAG = "MmsReceivedReceiverImpl"
    }
}
