package com.videocompressor.app.data.storage

import android.content.Context
import android.os.StatFs
import java.io.File

object StorageHelper {

    /**
     * Checks if device has enough free storage to perform compression safely.
     * Requires at least inputFileSize * 1.2 plus a safety margin of 50 MB.
     */
    fun hasSufficientFreeSpace(context: Context, inputFileSizeBytes: Long): Boolean {
        val cacheDir = context.cacheDir
        val stat = StatFs(cacheDir.absolutePath)
        val availableBytes = stat.availableBytes

        val requiredBytes = (inputFileSizeBytes * 1.2).toLong() + (50 * 1024 * 1024)
        return availableBytes >= requiredBytes
    }

    /**
     * Prepares a clean output file in the cache directory for the compressed result.
     */
    fun createOutputFile(context: Context, originalName: String): File {
        val compressedDir = File(context.cacheDir, "compressed").apply { mkdirs() }
        val baseName = originalName.substringBeforeLast(".")
        val sanitizedBase = baseName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        return File(compressedDir, "${sanitizedBase}_compressed_${System.currentTimeMillis()}.mp4")
    }

    /**
     * Safely deletes an intermediate file if it exists.
     */
    fun cleanupFile(filePath: String?) {
        if (filePath.isNullOrBlank()) return
        try {
            val file = File(filePath)
            if (file.exists()) {
                file.delete()
            }
        } catch (_: Exception) {}
    }

    /**
     * Cleans up aged temporary picked files.
     */
    fun cleanupAgedPickedFiles(context: Context) {
        try {
            val pickedDir = File(context.cacheDir, "picked")
            if (pickedDir.exists()) {
                val cutoff = System.currentTimeMillis() - (24 * 60 * 60 * 1000L) // 24 hours
                pickedDir.listFiles()?.forEach { file ->
                    if (file.lastModified() < cutoff) {
                        file.delete()
                    }
                }
            }
        } catch (_: Exception) {}
    }
}
