package com.videocompressor.app.data.compression

import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import com.videocompressor.app.domain.model.VideoMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class MediaMetadataExtractor(private val context: Context) {

    /**
     * Copies the content URI to a local cache file (if not already local) and extracts full multimedia metadata.
     */
    suspend fun extractFromUri(uri: Uri): Result<VideoMetadata> = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        try {
            // First determine filename and size from ContentResolver
            val (fileName, contentSize) = queryFileDetails(uri)

            // Cache file locally to provide direct seekable access for FFmpeg
            val localCacheDir = File(context.cacheDir, "picked").apply { mkdirs() }
            val sanitizedName = fileName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
            val localFile = File(localCacheDir, "input_${System.currentTimeMillis()}_$sanitizedName")

            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(localFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            } ?: return@withContext Result.failure(IllegalStateException("Unable to open input stream for URI: $uri"))

            var durationMs = 0L
            var width = 0
            var height = 0
            var rotation = 0
            var bitrateBps = 0L
            var mimeType = "video/mp4"

            try {
                retriever.setDataSource(localFile.absolutePath)
                durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
                width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
                height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
                rotation = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0
                bitrateBps = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toLongOrNull() ?: 0L
                mimeType = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE) ?: "video/mp4"
            } catch (_: Exception) {
                // Will fallback to FFprobe below
            }

            // Inspect track-level details using MediaExtractor
            var trackDetails = inspectTracks(localFile.absolutePath)

            // If Android's native retriever failed (common with older AVI or exotic MOV files), fallback to FFprobeKit
            if (durationMs <= 0L || width <= 0 || height <= 0 || trackDetails.videoCodec == null) {
                try {
                    val probeSession = com.arthenica.ffmpegkit.FFprobeKit.getMediaInformation(localFile.absolutePath)
                    val mediaInfo = probeSession.mediaInformation
                    if (mediaInfo != null) {
                        if (durationMs <= 0L) {
                            val durSec = mediaInfo.duration?.toDoubleOrNull() ?: 0.0
                            durationMs = (durSec * 1000.0).toLong()
                        }
                        if (bitrateBps <= 0L) {
                            bitrateBps = mediaInfo.bitrate?.toLongOrNull() ?: 0L
                        }
                        mediaInfo.streams?.forEach { stream ->
                            if (stream.type == "video") {
                                if (width <= 0) width = stream.width?.toInt() ?: 0
                                if (height <= 0) height = stream.height?.toInt() ?: 0
                                if (trackDetails.videoCodec == null) {
                                    val fps = stream.realFrameRate?.split("/")?.let { parts ->
                                        if (parts.size == 2 && parts[1].toFloatOrNull() != 0f) {
                                            parts[0].toFloat() / parts[1].toFloat()
                                        } else null
                                    } ?: 30f
                                    trackDetails = trackDetails.copy(
                                        videoCodec = stream.codec,
                                        frameRate = fps
                                    )
                                }
                            } else if (stream.type == "audio" && trackDetails.audioCodec == null) {
                                trackDetails = trackDetails.copy(
                                    audioCodec = stream.codec,
                                    audioChannels = stream.channels?.toInt(),
                                    audioSampleRate = stream.sampleRate?.toInt()
                                )
                            }
                        }
                    }
                } catch (_: Exception) {}
            }

            if (bitrateBps <= 0L && durationMs > 0) {
                bitrateBps = (localFile.length() * 8L * 1000L) / durationMs
            }

            val metadata = VideoMetadata(
                uriString = uri.toString(),
                localFilePath = localFile.absolutePath,
                fileName = fileName,
                fileSizeBytes = localFile.length(),
                durationMs = durationMs,
                width = width,
                height = height,
                rotation = rotation,
                bitrateBps = bitrateBps,
                frameRate = trackDetails.frameRate,
                videoCodec = trackDetails.videoCodec ?: mimeType,
                audioCodec = trackDetails.audioCodec,
                audioChannels = trackDetails.audioChannels,
                audioSampleRate = trackDetails.audioSampleRate
            )

            Result.success(metadata)
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {}
        }
    }

    private fun queryFileDetails(uri: Uri): Pair<String, Long> {
        var name = "video_${System.currentTimeMillis()}.mp4"
        var size = 0L

        if (uri.scheme == "content") {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIndex != -1) {
                        cursor.getString(nameIndex)?.let { name = it }
                    }
                    if (sizeIndex != -1) {
                        size = cursor.getLong(sizeIndex)
                    }
                }
            }
        } else if (uri.scheme == "file") {
            val file = File(uri.path ?: "")
            if (file.exists()) {
                name = file.name
                size = file.length()
            }
        }
        return Pair(name, size)
    }

    private data class TrackDetails(
        val frameRate: Float = 30f,
        val videoCodec: String? = null,
        val audioCodec: String? = null,
        val audioChannels: Int? = null,
        val audioSampleRate: Int? = null
    )

    private fun inspectTracks(filePath: String): TrackDetails {
        var fps = 30f
        var vCodec: String? = null
        var aCodec: String? = null
        var channels: Int? = null
        var sampleRate: Int? = null

        val extractor = MediaExtractor()
        try {
            extractor.setDataSource(filePath)
            val trackCount = extractor.trackCount
            for (i in 0 until trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: continue
                if (mime.startsWith("video/")) {
                    vCodec = mime
                    if (format.containsKey(MediaFormat.KEY_FRAME_RATE)) {
                        fps = format.getInteger(MediaFormat.KEY_FRAME_RATE).toFloat()
                    }
                } else if (mime.startsWith("audio/")) {
                    aCodec = mime
                    if (format.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
                        channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                    }
                    if (format.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                        sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                    }
                }
            }
        } catch (_: Exception) {
            // Fallback gracefully
        } finally {
            try {
                extractor.release()
            } catch (_: Exception) {}
        }

        return TrackDetails(
            frameRate = fps,
            videoCodec = vCodec,
            audioCodec = aCodec,
            audioChannels = channels,
            audioSampleRate = sampleRate
        )
    }
}
