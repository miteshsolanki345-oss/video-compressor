package com.videocompressor.app.presentation.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.videocompressor.app.data.repository.VideoCompressionRepositoryImpl
import com.videocompressor.app.data.service.VideoCompressionService
import com.videocompressor.app.data.storage.StorageHelper
import com.videocompressor.app.domain.model.AudioMode
import com.videocompressor.app.domain.model.CompressionConfig
import com.videocompressor.app.domain.model.CompressionPreset
import com.videocompressor.app.domain.model.CompressionProgress
import com.videocompressor.app.domain.repository.VideoCompressionRepository
import com.videocompressor.app.util.FileProviderUtil
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class CompressorViewModel(
    application: Application,
    private val repository: VideoCompressionRepository = VideoCompressionRepositoryImpl(application)
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<CompressorUiState>(CompressorUiState.Idle)
    val uiState: StateFlow<CompressorUiState> = _uiState.asStateFlow()

    private var activeCompressionJob: Job? = null
    private var activeOutputPath: String? = null
    private var activeStartTime: Long = 0L

    init {
        // Clean up any stale files from previous app runs on startup
        StorageHelper.cleanupAgedPickedFiles(application)
    }

    fun onEvent(event: CompressorUiEvent) {
        when (event) {
            is CompressorUiEvent.OnVideoSelected -> handleVideoSelected(event.uri)
            is CompressorUiEvent.OnPresetSelected -> handlePresetSelected(event.preset)
            is CompressorUiEvent.OnCrfChanged -> handleCrfChanged(event.crf)
            is CompressorUiEvent.OnFfmpegPresetChanged -> handleFfmpegPresetChanged(event.preset)
            is CompressorUiEvent.OnAudioModeChanged -> handleAudioModeChanged(event.audioMode)
            is CompressorUiEvent.OnHardwareAccelerationToggled -> handleHardwareAccelerationToggled(event.enabled)
            is CompressorUiEvent.OnStartCompressionClicked -> startCompression()
            is CompressorUiEvent.OnCancelCompressionClicked -> cancelCompression()
            is CompressorUiEvent.OnSaveToGalleryClicked -> saveToGallery()
            is CompressorUiEvent.OnShareClicked -> shareVideo(event.context)
            is CompressorUiEvent.OnResetClicked -> resetToIdle()
            is CompressorUiEvent.OnDismissMessage -> dismissMessage()
        }
    }

    private fun handleVideoSelected(uri: Uri) {
        viewModelScope.launch {
            _uiState.value = CompressorUiState.Analyzing("Inspecting video streams & metadata...")
            val result = repository.analyzeVideo(uri)
            result.fold(
                onSuccess = { metadata ->
                    _uiState.value = CompressorUiState.Ready(
                        videoMetadata = metadata,
                        config = CompressionConfig.fromPreset(CompressionPreset.BALANCED_SMART_SHRINK)
                    )
                },
                onFailure = { error ->
                    _uiState.value = CompressorUiState.Error(
                        errorMessage = "Could not process selected video: ${error.localizedMessage ?: "Unknown error"}"
                    )
                }
            )
        }
    }

    private fun handlePresetSelected(preset: CompressionPreset) {
        val currentState = _uiState.value
        if (currentState is CompressorUiState.Ready) {
            val newConfig = CompressionConfig.fromPreset(preset)
            _uiState.value = currentState.copy(config = newConfig)
        }
    }

    private fun handleCrfChanged(crf: Int) {
        val currentState = _uiState.value
        if (currentState is CompressorUiState.Ready) {
            val newConfig = currentState.config.copy(
                preset = CompressionPreset.CUSTOM,
                crf = crf
            )
            _uiState.value = currentState.copy(config = newConfig)
        }
    }

    private fun handleFfmpegPresetChanged(ffmpegPreset: String) {
        val currentState = _uiState.value
        if (currentState is CompressorUiState.Ready) {
            val newConfig = currentState.config.copy(
                preset = CompressionPreset.CUSTOM,
                ffmpegPreset = ffmpegPreset
            )
            _uiState.value = currentState.copy(config = newConfig)
        }
    }

    private fun handleAudioModeChanged(audioMode: AudioMode) {
        val currentState = _uiState.value
        if (currentState is CompressorUiState.Ready) {
            val newConfig = currentState.config.copy(audioMode = audioMode)
            _uiState.value = currentState.copy(config = newConfig)
        }
    }

    private fun handleHardwareAccelerationToggled(enabled: Boolean) {
        val currentState = _uiState.value
        if (currentState is CompressorUiState.Ready) {
            val newConfig = currentState.config.copy(useHardwareAcceleration = enabled)
            _uiState.value = currentState.copy(config = newConfig)
        }
    }

    private fun startCompression() {
        val currentState = _uiState.value as? CompressorUiState.Ready ?: return
        val metadata = currentState.videoMetadata
        val config = currentState.config
        val context = getApplication<Application>()

        val outputFile = StorageHelper.createOutputFile(context, metadata.fileName)
        activeOutputPath = outputFile.absolutePath
        activeStartTime = System.currentTimeMillis()

        // Start Foreground Service so compression continues without interruption in background
        VideoCompressionService.start(context, metadata.fileName)

        _uiState.value = CompressorUiState.Compressing(
            videoMetadata = metadata,
            config = config,
            progress = CompressionProgress()
        )

        activeCompressionJob = viewModelScope.launch {
            repository.compressVideo(metadata, config)
                .catch { exception ->
                    VideoCompressionService.stop(context)
                    _uiState.value = CompressorUiState.Error(
                        errorMessage = "Compression failed: ${exception.localizedMessage ?: "Unknown error"}",
                        previousMetadata = metadata
                    )
                }
                .collect { progress ->
                    _uiState.value = CompressorUiState.Compressing(
                        videoMetadata = metadata,
                        config = config,
                        progress = progress
                    )

                    // Update persistent foreground notification
                    val statusText = "${progress.formattedPercentage} • ${String.format("%.1f", progress.currentFps)} fps • ETA: ${progress.formattedEta}"
                    VideoCompressionService.updateProgress(
                        context,
                        metadata.fileName,
                        progress.percentage.toInt(),
                        statusText
                    )

                    if (progress.percentage >= 100f) {
                        finalizeCompressionJob(metadata, config)
                    }
                }
        }
    }

    private suspend fun finalizeCompressionJob(metadata: com.videocompressor.app.domain.model.VideoMetadata, config: CompressionConfig) {
        val context = getApplication<Application>()
        VideoCompressionService.stop(context)

        val outputPath = activeOutputPath ?: return
        val result = repository.finalizeCompression(
            sourceMetadata = metadata,
            outputPath = outputPath,
            startTimeMs = activeStartTime,
            config = config
        )

        result.fold(
            onSuccess = { compressionResult ->
                _uiState.value = CompressorUiState.Completed(result = compressionResult)
            },
            onFailure = { error ->
                _uiState.value = CompressorUiState.Error(
                    errorMessage = "Failed to inspect final video: ${error.localizedMessage}",
                    previousMetadata = metadata
                )
            }
        )
    }

    private fun cancelCompression() {
        activeCompressionJob?.cancel()
        activeCompressionJob = null
        repository.cancelActiveCompression()

        val context = getApplication<Application>()
        VideoCompressionService.stop(context)

        activeOutputPath?.let { StorageHelper.cleanupFile(it) }

        val currentState = _uiState.value
        if (currentState is CompressorUiState.Compressing) {
            _uiState.value = CompressorUiState.Ready(
                videoMetadata = currentState.videoMetadata,
                config = currentState.config
            )
        } else {
            _uiState.value = CompressorUiState.Idle
        }
    }

    private fun saveToGallery() {
        val currentState = _uiState.value as? CompressorUiState.Completed ?: return
        if (currentState.isSavedToGallery || currentState.saveInProgress) return

        _uiState.value = currentState.copy(saveInProgress = true)

        viewModelScope.launch {
            val suggestedName = "Shrunk_${currentState.result.originalVideo.fileName}"
            val saveResult = repository.saveToGallery(
                filePath = currentState.result.outputFilePath,
                suggestedName = suggestedName
            )

            saveResult.fold(
                onSuccess = { uri ->
                    _uiState.value = currentState.copy(
                        isSavedToGallery = true,
                        savedGalleryUri = uri.toString(),
                        saveInProgress = false,
                        userMessage = "Saved to Gallery (Movies/VideoCompressor)"
                    )
                },
                onFailure = { error ->
                    _uiState.value = currentState.copy(
                        saveInProgress = false,
                        userMessage = "Failed to save: ${error.localizedMessage}"
                    )
                }
            )
        }
    }

    private fun shareVideo(context: Context) {
        val currentState = _uiState.value as? CompressorUiState.Completed ?: return
        val intent = FileProviderUtil.createShareIntent(context, currentState.result.outputFilePath)
        val chooser = Intent.createChooser(intent, "Share Compressed Video").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    private fun resetToIdle() {
        activeOutputPath = null
        _uiState.value = CompressorUiState.Idle
    }

    private fun dismissMessage() {
        val currentState = _uiState.value
        if (currentState is CompressorUiState.Completed) {
            _uiState.value = currentState.copy(userMessage = null)
        }
    }

    override fun onCleared() {
        super.onCleared()
        activeCompressionJob?.cancel()
        repository.cancelActiveCompression()
    }
}
