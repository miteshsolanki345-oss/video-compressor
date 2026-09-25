package com.videocompressor.app.domain.model

/**
 * Real-time progress stats emitted during the compression pipeline.
 */
data class CompressionProgress(
    val percentage: Float = 0f,
    val currentFps: Float = 0f,
    val processedDurationMs: Long = 0L,
    val totalDurationMs: Long = 0L,
    val currentBitrateKbps: Double = 0.0,
    val speedMultiplier: Double = 1.0,
    val estimatedTimeRemainingSeconds: Long = 0L,
    val elapsedTimeMs: Long = 0L
) {
    val formattedPercentage: String
        get() = "${percentage.coerceIn(0f, 100f).toInt()}%"

    val formattedEta: String
        get() {
            if (estimatedTimeRemainingSeconds <= 0) return "--:--"
            val minutes = estimatedTimeRemainingSeconds / 60
            val seconds = estimatedTimeRemainingSeconds % 60
            return String.format("%02d:%02d", minutes, seconds)
        }
}
