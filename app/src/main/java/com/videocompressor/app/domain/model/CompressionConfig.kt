package com.videocompressor.app.domain.model

/**
 * Configuration parameters for the video compression engine.
 */
data class CompressionConfig(
    val preset: CompressionPreset = CompressionPreset.BALANCED_SMART_SHRINK,
    val crf: Int = preset.crf,
    val ffmpegPreset: String = preset.ffmpegPreset,
    val audioMode: AudioMode = preset.audioMode,
    val useHardwareAcceleration: Boolean = false, // false = libx265 (most reliable offline CRF efficiency)
    val preserveMetadata: Boolean = true,
    val fastStart: Boolean = true // -movflags +faststart for instant streaming/playback
) {
    companion object {
        fun fromPreset(preset: CompressionPreset): CompressionConfig {
            return CompressionConfig(
                preset = preset,
                crf = preset.crf,
                ffmpegPreset = preset.ffmpegPreset,
                audioMode = preset.audioMode
            )
        }
    }
}
