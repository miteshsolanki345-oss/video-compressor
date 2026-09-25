package com.videocompressor.app.data.storage

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream

class MediaStoreSaver(private val context: Context) {

    /**
     * Saves the compressed file to the Android device Gallery (Movies/VideoCompressor) using MediaStore API.
     * Fully compliant with Scoped Storage (Android 10 - Android 15+).
     */
    suspend fun saveVideoToGallery(
        compressedFilePath: String,
        displayName: String
    ): Result<Uri> = withContext(Dispatchers.IO) {
        val file = File(compressedFilePath)
        if (!file.exists()) {
            return@withContext Result.failure(IllegalArgumentException("File does not exist: $compressedFilePath"))
        }

        val sanitizedName = if (displayName.endsWith(".mp4", ignoreCase = true)) {
            displayName
        } else {
            "$displayName.mp4"
        }

        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, sanitizedName)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            put(MediaStore.Video.Media.DATE_ADDED, System.currentTimeMillis() / 1000)
            put(MediaStore.Video.Media.DATE_MODIFIED, System.currentTimeMillis() / 1000)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Video.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MOVIES}/VideoCompressor")
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }
        }

        val resolver = context.contentResolver
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }

        val uri = resolver.insert(collection, values)
            ?: return@withContext Result.failure(IllegalStateException("Failed to insert MediaStore record"))

        try {
            resolver.openOutputStream(uri)?.use { outputStream ->
                FileInputStream(file).use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
            } ?: throw IllegalStateException("Failed to open output stream for MediaStore Uri: $uri")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Video.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            }

            Result.success(uri)
        } catch (e: Exception) {
            resolver.delete(uri, null, null)
            Result.failure(e)
        }
    }
}
