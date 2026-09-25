package com.videocompressor.app.domain.model

/**
 * Result data class comparing the original video vs compressed video for side-by-side verification.
 */
data class CompressionResult(
    val originalVideo: VideoMetadata,
    val compressedVideo: VideoMetadata,
    val outputFilePath: String,
    val compressionDurationMs: Long,
    val configUsed: CompressionConfig
) {
    val bytesSaved: Long
        get() = (originalVideo.fileSizeBytes - compressedVideo.fileSizeBytes).coerceAtLeast(0L)

    val percentageSaved: Float
        get() {
            if (originalVideo.fileSizeBytes <= 0) return 0f
            val ratio = (originalVideo.fileSizeBytes - compressedVideo.fileSizeBytes).toFloat() / originalVideo.fileSizeBytes
            return (ratio * 100f).coerceIn(0f, 100f)
        }

    val compressionRatio: Float
        get() {
            if (compressedVideo.fileSizeBytes <= 0) return 1f
            return originalVideo.fileSizeBytes.toFloat() / compressedVideo.fileSizeBytes
        }
}
