package com.videocompressor.app.domain.model

/**
 * Predefined compression profiles optimized for "Same-to-Same" visual fidelity.
 */
enum class CompressionPreset(
    val title: String,
    val subtitle: String,
    val crf: Int,
    val ffmpegPreset: String,
    val audioMode: AudioMode,
    val description: String
) {
    PERCEPTUALLY_LOSSLESS(
        title = "Perceptually Lossless",
        subtitle = "Zero perceptible loss (CRF 19)",
        crf = 19,
        ffmpegPreset = "medium",
        audioMode = AudioMode.COPY_PASSTHROUGH,
        description = "Identical visual details down to pixel level. Perfect for 4K/60fps master videos, gaming clips, and camera archives."
    ),
    BALANCED_SMART_SHRINK(
        title = "Balanced / Smart Shrink",
        subtitle = "Optimal (CRF 23) • ~40-60% size drop",
        crf = 23,
        ffmpegPreset = "medium",
        audioMode = AudioMode.AAC_160K,
        description = "Maintains exact original resolution with indistinguishable visual quality. Sweet spot for phone storage and social media sharing."
    ),
    HIGH_COMPRESSION(
        title = "High Compression",
        subtitle = "Maximum savings (CRF 27) • ~65-80% size drop",
        crf = 27,
        ffmpegPreset = "faster",
        audioMode = AudioMode.AAC_128K,
        description = "Aggressive HEVC reduction while still retaining original pixel dimensions. Ideal for email or messaging apps."
    ),
    CUSTOM(
        title = "Custom Parameters",
        subtitle = "Fine-tune CRF, preset & audio codec",
        crf = 23,
        ffmpegPreset = "medium",
        audioMode = AudioMode.AAC_160K,
        description = "Direct control over CRF (16-32), FFmpeg preset speed, and audio passthrough vs re-encoding."
    )
}

enum class AudioMode(val displayName: String, val bitrateKbps: Int?) {
    COPY_PASSTHROUGH("Passthrough (Copy Bit-for-Bit)", null),
    AAC_192K("AAC High Fidelity (192 kbps)", 192),
    AAC_160K("AAC Standard (160 kbps)", 160),
    AAC_128K("AAC Compact (128 kbps)", 128)
}
