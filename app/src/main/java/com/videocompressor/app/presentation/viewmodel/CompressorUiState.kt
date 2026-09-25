package com.videocompressor.app.presentation.viewmodel

import com.videocompressor.app.domain.model.CompressionConfig
import com.videocompressor.app.domain.model.CompressionPreset
import com.videocompressor.app.domain.model.CompressionProgress
import com.videocompressor.app.domain.model.CompressionResult
import com.videocompressor.app.domain.model.VideoMetadata

sealed interface CompressorUiState {
    data object Idle : CompressorUiState
    
    data class Analyzing(val message: String = "Analyzing video stream & metadata...") : CompressorUiState

    data class Ready(
        val videoMetadata: VideoMetadata,
        val config: CompressionConfig = CompressionConfig.fromPreset(CompressionPreset.BALANCED_SMART_SHRINK),
        val estimatedSizeRange: Pair<Long, Long> = calculateEstimatedSize(videoMetadata, config)
    ) : CompressorUiState

    data class Compressing(
        val videoMetadata: VideoMetadata,
        val config: CompressionConfig,
        val progress: CompressionProgress = CompressionProgress()
    ) : CompressorUiState

    data class Completed(
        val result: CompressionResult,
        val isSavedToGallery: Boolean = false,
        val savedGalleryUri: String? = null,
        val saveInProgress: Boolean = false,
        val userMessage: String? = null
    ) : CompressorUiState

    data class Error(
        val errorMessage: String,
        val previousMetadata: VideoMetadata? = null
    ) : CompressorUiState
}

private fun calculateEstimatedSize(
    metadata: VideoMetadata,
    config: CompressionConfig
): Pair<Long, Long> {
    val orig = metadata.fileSizeBytes
    if (orig <= 0) return Pair(0L, 0L)
    return when (config.preset) {
        CompressionPreset.PERCEPTUALLY_LOSSLESS -> Pair((orig * 0.70).toLong(), (orig * 0.90).toLong())
        CompressionPreset.BALANCED_SMART_SHRINK -> Pair((orig * 0.35).toLong(), (orig * 0.55).toLong())
        CompressionPreset.HIGH_COMPRESSION -> Pair((orig * 0.20).toLong(), (orig * 0.35).toLong())
        CompressionPreset.CUSTOM -> {
            val factor = ((config.crf - 16).toFloat() / 16f).coerceIn(0f, 1f)
            val minRatio = (0.75f - (factor * 0.55f)).coerceAtLeast(0.15f)
            val maxRatio = (minRatio + 0.15f).coerceAtMost(0.95f)
            Pair((orig * minRatio).toLong(), (orig * maxRatio).toLong())
        }
    }
}
