package com.example.sms_android

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.Telephony
import android.telephony.SubscriptionManager
import android.util.Log

/**
 * Utility for resolving MMS APN settings from the device's telephony database.
 */
object ApnSettingsResolver {
    private const val TAG = "ApnSettingsResolver"
    private const val DEFAULT_USER_AGENT = "AndroidMms/1.0"

    data class ApnSettings(
        val mmsc: String,
        val proxy: String?,
        val port: String?,
        val userAgent: String?,
        val user: String?,
        val password: String?
    )

    fun resolve(context: Context): ApnSettings? {
        val resolver = context.contentResolver
        val subscriptionManager = context.getSystemService(SubscriptionManager::class.java)
        val dataSubId = subscriptionManager?.defaultDataSubscriptionId
            ?: SubscriptionManager.getDefaultDataSubscriptionId()

        val potentialUris = mutableListOf<Uri>()
        if (dataSubId != null && dataSubId != SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                potentialUris.add(Telephony.Carriers.getUriForSubscriptionId(dataSubId))
            }
            potentialUris.add(Uri.parse("content://telephony/carriers/preferapn/subId/$dataSubId"))
            potentialUris.add(Uri.withAppendedPath(Telephony.Carriers.CONTENT_URI, "subId/$dataSubId"))
        }
        potentialUris.add(Uri.parse("content://telephony/carriers/preferapn"))
        potentialUris.add(Telephony.Carriers.CONTENT_URI)

        val projection = arrayOf(
            Telephony.Carriers.MMSC,
            Telephony.Carriers.MMSPROXY,
            Telephony.Carriers.MMSPORT,
            Telephony.Carriers.USER_AGENT,
            Telephony.Carriers.USER,
            Telephony.Carriers.PASSWORD
        )
        val selection = "${Telephony.Carriers.TYPE} LIKE ?"
        val selectionArgs = arrayOf("%mms%")

        for (uri in potentialUris) {
            try {
                val cursor = resolver.query(uri, projection, selection, selectionArgs, null)
                cursor.useSafely { c ->
                    if (c != null && c.moveToFirst()) {
                        val mmsc = c.getString(c.getColumnIndexOrThrow(Telephony.Carriers.MMSC))
                        if (!mmsc.isNullOrEmpty()) {
                            val proxy = c.getStringOrNull(Telephony.Carriers.MMSPROXY)
                            val port = c.getStringOrNull(Telephony.Carriers.MMSPORT)
                            val userAgent = c.getStringOrNull(Telephony.Carriers.USER_AGENT)
                            val user = c.getStringOrNull(Telephony.Carriers.USER)
                            val password = c.getStringOrNull(Telephony.Carriers.PASSWORD)

                            Log.d(TAG, "Resolved APN from $uri: mmsc=$mmsc, proxy=$proxy, port=$port")
                            return ApnSettings(
                                mmsc = mmsc,
                                proxy = proxy,
                                port = port,
                                userAgent = userAgent ?: DEFAULT_USER_AGENT,
                                user = user,
                                password = password
                            )
                        }
                    }
                }
            } catch (security: SecurityException) {
                Log.w(TAG, "Unable to query APN uri=$uri due to security restriction: ${security.message}")
            } catch (throwable: Throwable) {
                Log.e(TAG, "Failed to query APN uri=$uri: ${throwable.message}", throwable)
            }
        }

        Log.w(TAG, "Falling back to default APN configuration")
        return null
    }

    private inline fun Cursor?.useSafely(block: (Cursor?) -> Unit) {
        try {
            block(this)
        } finally {
            this?.close()
        }
    }

    private fun Cursor.getStringOrNull(column: String): String? {
        return try {
            getString(getColumnIndexOrThrow(column))?.takeIf { it.isNotEmpty() }
        } catch (e: Exception) {
            null
        }
    }
}
