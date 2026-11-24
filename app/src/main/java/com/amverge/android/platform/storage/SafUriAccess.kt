package com.amverge.android.platform.storage

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.OpenableColumns
import com.amverge.android.core.logging.Logger
import java.io.File
import java.io.FileInputStream

class SafUriAccess(
    private val context: Context
) {
    companion object {
        private const val TAG = "SafUriAccess"
    }

    fun takePersistableReadPermission(uri: Uri): Int {
        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        try {
            context.contentResolver.takePersistableUriPermission(uri, flags)
            Logger.d(TAG, "Persistable read permission taken for $uri")
            return flags
        } catch (e: SecurityException) {
            Logger.w(TAG, "Failed to take persistable URI permission for $uri", e)
            return 0
        }
    }

    fun isUriAccessible(uri: Uri): Boolean {
        return try {
            context.contentResolver.openFileDescriptor(uri, "r")?.use { true } ?: false
        } catch (e: Exception) {
            Logger.w(TAG, "URI accessibility check failed for $uri", e)
            false
        }
    }

    fun getDisplayName(uri: Uri): String {
        var name: String? = null
        if (uri.scheme == "content") {
            try {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1 && cursor.moveToFirst()) {
                        name = cursor.getString(nameIndex)
                    }
                }
            } catch (e: Exception) {
                Logger.w(TAG, "Failed to query display name for $uri", e)
            }
        }
        return name ?: uri.lastPathSegment ?: "unknown_video.mp4"
    }

    fun copyFileToDocumentUri(sourceFile: File, destinationUri: Uri): Boolean {
        return try {
            context.contentResolver.openOutputStream(destinationUri, "w")?.use { outputStream ->
                FileInputStream(sourceFile).use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            true
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to copy ${sourceFile.name} to $destinationUri", e)
            false
        }
    }

    fun createDocumentInTree(treeUri: Uri, mimeType: String, displayName: String): Uri? {
        return try {
            val docUri = DocumentsContract.buildDocumentUriUsingTree(
                treeUri,
                DocumentsContract.getTreeDocumentId(treeUri)
            )
            DocumentsContract.createDocument(context.contentResolver, docUri, mimeType, displayName)
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to create document in tree $treeUri for $displayName", e)
            null
        }
    }

    fun saveVideoToSharedStorage(sourceFile: File, displayName: String): Uri? {
        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/AMVerge")
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }
        }

        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }

        val destinationUri = context.contentResolver.insert(collection, values) ?: return null

        try {
            context.contentResolver.openOutputStream(destinationUri, "w")?.use { out ->
                FileInputStream(sourceFile).use { input ->
                    input.copyTo(out)
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Video.Media.IS_PENDING, 0)
                context.contentResolver.update(destinationUri, values, null, null)
            }

            Logger.i(TAG, "Saved video to shared storage: $destinationUri")
            return destinationUri
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to save video to shared storage", e)
            try {
                context.contentResolver.delete(destinationUri, null, null)
            } catch (_: Exception) {}
            return null
        }
    }

    fun saveFileToSharedStorage(
        sourceFile: File,
        displayName: String,
        mimeType: String = "application/zip",
        subDir: String = "Movies/AMVerge"
    ): Uri? {
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, subDir)
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }

        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }

        val destinationUri = runCatching { context.contentResolver.insert(collection, values) }.getOrNull() ?: return null

        return try {
            context.contentResolver.openOutputStream(destinationUri, "w")?.use { out ->
                FileInputStream(sourceFile).use { input ->
                    input.copyTo(out)
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                context.contentResolver.update(destinationUri, values, null, null)
            }

            Logger.i(TAG, "Saved file to shared storage: $destinationUri")
            destinationUri
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to save file to shared storage", e)
            runCatching { context.contentResolver.delete(destinationUri, null, null) }
            null
        }
    }
}
