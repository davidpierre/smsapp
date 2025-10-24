package com.example.sms_android

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.Telephony
import android.util.Log
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Shared helpers for reading MMS content from the telephony provider.
 */
object MmsUtils {
    private const val TAG = "MmsUtils"

    private data class MmsPart(
        val id: Long,
        val contentType: String?,
        val text: String?,
        val dataPath: String?,
        val fileName: String?,
        val size: Long?
    )

    fun getAddress(context: Context, mmsId: Long): String? {
        val addrUri = Uri.parse("content://mms/$mmsId/addr")
        val projection = arrayOf("address", "type")
        val cursor = context.contentResolver.query(addrUri, projection, null, null, null)
        cursor.useSafely { c ->
            if (c != null) {
                while (c.moveToNext()) {
                    val type = c.getInt(c.getColumnIndexOrThrow("type"))
                    if (type == Telephony.Mms.Addr.FROM || type == 137) {
                        val address = c.getString(c.getColumnIndexOrThrow("address"))
                        if (!address.isNullOrEmpty() && address != "insert-address-token") {
                            return address
                        }
                    }
                }
            }
        }
        return null
    }

    fun getTimestamp(context: Context, mmsId: Long): Long? {
        val uri = ContentUris.withAppendedId(Telephony.Mms.CONTENT_URI, mmsId)
        val cursor = context.contentResolver.query(uri, arrayOf(Telephony.Mms.DATE), null, null, null)
        cursor.useSafely { c ->
            if (c != null && c.moveToFirst()) {
                val seconds = c.getLong(c.getColumnIndexOrThrow(Telephony.Mms.DATE))
                return seconds * 1000
            }
        }
        return null
    }

    fun buildDisplayBody(context: Context, mmsId: Long): String {
        val parts = loadParts(context, mmsId)
        if (parts.isEmpty()) {
            return "Picture message"
        }

        val textParts = mutableListOf<String>()
        parts.forEach { part ->
            when {
                part.contentType == "text/plain" && !part.text.isNullOrEmpty() -> {
                    textParts.add(part.text)
                }
                part.contentType?.startsWith("image/") == true -> {
                    val imageUri = "content://mms/part/${part.id}"
                    textParts.add("[IMAGE:$imageUri]")
                }
                part.contentType?.startsWith("video/") == true -> {
                    val videoUri = "content://mms/part/${part.id}"
                    textParts.add("[VIDEO:$videoUri]")
                }
                part.contentType?.startsWith("audio/") == true -> {
                    val audioUri = "content://mms/part/${part.id}"
                    textParts.add("[AUDIO:$audioUri]")
                }
            }
        }

        return if (textParts.isEmpty()) "Picture message" else textParts.joinToString(" ")
    }

    fun getAttachments(context: Context, mmsId: Long): List<Map<String, Any>> {
        val parts = loadParts(context, mmsId)
        if (parts.isEmpty()) return emptyList()

        return parts.filter { part ->
            val type = part.contentType?.lowercase()
            type != null && (type.startsWith("image/") || type.startsWith("video/") || type.startsWith("audio/"))
        }.map { part ->
            val uri = "content://mms/part/${part.id}"
            val map = mutableMapOf<String, Any>(
                "uri" to uri,
                "contentType" to (part.contentType ?: "application/octet-stream")
            )
            part.fileName?.let { map["fileName"] = it }
            part.size?.let { map["size"] = it }
            map
        }
    }

    private fun loadParts(context: Context, mmsId: Long): List<MmsPart> {
        val projection = arrayOf(
            Telephony.Mms.Part._ID,
            Telephony.Mms.Part.CONTENT_TYPE,
            Telephony.Mms.Part.TEXT,
            Telephony.Mms.Part._DATA,
            Telephony.Mms.Part.NAME,
            Telephony.Mms.Part.FILENAME,
            Telephony.Mms.Part._SIZE
        )
        val selection = "${Telephony.Mms.Part.MSG_ID}=?"
        val selectionArgs = arrayOf(mmsId.toString())
        val parts = mutableListOf<MmsPart>()

        var cursor = context.contentResolver.query(
            Telephony.Mms.Part.CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            null
        )

        if (cursor == null) {
            // Some devices restrict the direct part table query; fall back to the legacy per-MMS URI.
            val partUri = Uri.parse("content://mms/$mmsId/part")
            cursor = context.contentResolver.query(partUri, projection, null, null, null)
        }

        cursor.useSafely { c ->
            if (c != null) {
                while (c.moveToNext()) {
                    val id = c.getLong(c.getColumnIndexOrThrow(Telephony.Mms.Part._ID))
                    val type = c.getString(c.getColumnIndexOrThrow(Telephony.Mms.Part.CONTENT_TYPE))
                    val rawText = try {
                        c.getString(c.getColumnIndexOrThrow(Telephony.Mms.Part.TEXT))
                    } catch (_: Exception) {
                        null
                    }
                    val dataPath = try {
                        c.getString(c.getColumnIndexOrThrow(Telephony.Mms.Part._DATA))
                    } catch (_: Exception) {
                        null
                    }
                    val fileName = try {
                        c.getString(c.getColumnIndexOrThrow(Telephony.Mms.Part.NAME))
                            ?: c.getString(c.getColumnIndexOrThrow(Telephony.Mms.Part.FILENAME))
                    } catch (_: Exception) {
                        null
                    }
                    val size = try {
                        c.getLong(c.getColumnIndexOrThrow(Telephony.Mms.Part._SIZE))
                    } catch (_: Exception) {
                        null
                    }

                    val text = when {
                        !rawText.isNullOrEmpty() -> rawText
                        type.equals("text/plain", ignoreCase = true) -> loadTextFromPart(context, id)
                        else -> null
                    }

                    parts.add(MmsPart(id, type, text, dataPath, fileName, size))
                }
            }
        }
        Log.d(TAG, "Resolved ${parts.size} parts for MMS $mmsId")
        return parts
    }

    private fun loadTextFromPart(context: Context, partId: Long): String? {
        return try {
            val uri = ContentUris.withAppendedId(Telephony.Mms.Part.CONTENT_URI, partId)
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use(BufferedReader::readText)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to load text for part $partId", e)
            null
        }
    }

    private inline fun Cursor?.useSafely(block: (Cursor?) -> Unit) {
        try {
            block(this)
        } finally {
            this?.close()
        }
    }
}
