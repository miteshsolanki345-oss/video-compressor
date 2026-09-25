package com.videocompressor.app.presentation.viewmodel

import android.content.Context
import android.net.Uri
import com.videocompressor.app.domain.model.AudioMode
import com.videocompressor.app.domain.model.CompressionPreset

sealed interface CompressorUiEvent {
    data class OnVideoSelected(val uri: Uri) : CompressorUiEvent
    data class OnPresetSelected(val preset: CompressionPreset) : CompressorUiEvent
    data class OnCrfChanged(val crf: Int) : CompressorUiEvent
    data class OnFfmpegPresetChanged(val preset: String) : CompressorUiEvent
    data class OnAudioModeChanged(val audioMode: AudioMode) : CompressorUiEvent
    data class OnHardwareAccelerationToggled(val enabled: Boolean) : CompressorUiEvent
    data object OnStartCompressionClicked : CompressorUiEvent
    data object OnCancelCompressionClicked : CompressorUiEvent
    data object OnSaveToGalleryClicked : CompressorUiEvent
    data class OnShareClicked(val context: Context) : CompressorUiEvent
    data object OnResetClicked : CompressorUiEvent
    data object OnDismissMessage : CompressorUiEvent
}
