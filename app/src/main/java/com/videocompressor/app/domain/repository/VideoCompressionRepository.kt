package com.videocompressor.app.domain.repository

import android.net.Uri
import com.videocompressor.app.domain.model.CompressionConfig
import com.videocompressor.app.domain.model.CompressionProgress
import com.videocompressor.app.domain.model.CompressionResult
import com.videocompressor.app.domain.model.VideoMetadata
import kotlinx.coroutines.flow.Flow

interface VideoCompressionRepository {
    suspend fun analyzeVideo(sourceUri: Uri): Result<VideoMetadata>
    
    fun compressVideo(
        sourceMetadata: VideoMetadata,
        config: CompressionConfig
    ): Flow<CompressionProgress>

    suspend fun finalizeCompression(
        sourceMetadata: VideoMetadata,
        outputPath: String,
        startTimeMs: Long,
        config: CompressionConfig
    ): Result<CompressionResult>

    fun cancelActiveCompression()

    suspend fun saveToGallery(filePath: String, suggestedName: String): Result<Uri>
}
