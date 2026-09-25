package com.videocompressor.app.domain.model

/**
 * Detailed metadata of a video file for compression analysis and UI rendering.
 */
data class VideoMetadata(
    val uriString: String,
    val localFilePath: String,
    val fileName: String,
    val fileSizeBytes: Long,
    val durationMs: Long,
    val width: Int,
    val height: Int,
    val rotation: Int = 0,
    val bitrateBps: Long = 0L,
    val frameRate: Float = 30f,
    val videoCodec: String = "unknown",
    val audioCodec: String? = null,
    val audioChannels: Int? = null,
    val audioSampleRate: Int? = null
) {
    /**
     * Width after accounting for display orientation rotation (e.g. portrait video filmed at 90/270 degrees).
     */
    val displayWidth: Int
        get() = if (rotation == 90 || rotation == 270) height else width

    /**
     * Height after accounting for display orientation rotation.
     */
    val displayHeight: Int
        get() = if (rotation == 90 || rotation == 270) width else height

    /**
     * Resolution string formatted e.g. "1920 x 1080"
     */
    val resolutionLabel: String
        get() = "${displayWidth}x${displayHeight}"
}
