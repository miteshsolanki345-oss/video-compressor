package com.videocompressor.app.data.repository

import android.content.Context
import android.net.Uri
import com.videocompressor.app.data.compression.FFmpegVideoCompressor
import com.videocompressor.app.data.compression.MediaMetadataExtractor
import com.videocompressor.app.data.storage.MediaStoreSaver
import com.videocompressor.app.data.storage.StorageHelper
import com.videocompressor.app.domain.model.CompressionConfig
import com.videocompressor.app.domain.model.CompressionProgress
import com.videocompressor.app.domain.model.CompressionResult
import com.videocompressor.app.domain.model.VideoMetadata
import com.videocompressor.app.domain.repository.VideoCompressionRepository
import kotlinx.coroutines.flow.Flow
import java.io.File

class VideoCompressionRepositoryImpl(
    private val context: Context,
    private val metadataExtractor: MediaMetadataExtractor = MediaMetadataExtractor(context),
    private val compressor: FFmpegVideoCompressor = FFmpegVideoCompressor(),
    private val mediaStoreSaver: MediaStoreSaver = MediaStoreSaver(context)
) : VideoCompressionRepository {

    override suspend fun analyzeVideo(sourceUri: Uri): Result<VideoMetadata> {
        return metadataExtractor.extractFromUri(sourceUri)
    }

    override fun compressVideo(
        sourceMetadata: VideoMetadata,
        config: CompressionConfig
    ): Flow<CompressionProgress> {
        // Verify disk space
        if (!StorageHelper.hasSufficientFreeSpace(context, sourceMetadata.fileSizeBytes)) {
            throw IllegalStateException("Insufficient free disk space for compression. Please free up space.")
        }

        val outputFile = StorageHelper.createOutputFile(context, sourceMetadata.fileName)
        return compressor.compress(
            inputMetadata = sourceMetadata,
            outputFilePath = outputFile.absolutePath,
            config = config
        )
    }

    override suspend fun finalizeCompression(
        sourceMetadata: VideoMetadata,
        outputPath: String,
        startTimeMs: Long,
        config: CompressionConfig
    ): Result<CompressionResult> {
        val outputFile = File(outputPath)
        if (!outputFile.exists() || outputFile.length() == 0L) {
            return Result.failure(IllegalStateException("Compressed output file was not found or is empty."))
        }

        // Analyze compressed file metadata
        val compressedMetaResult = metadataExtractor.extractFromUri(Uri.fromFile(outputFile))
        return if (compressedMetaResult.isSuccess) {
            val compressedMeta = compressedMetaResult.getOrThrow()
            val totalTime = System.currentTimeMillis() - startTimeMs
            Result.success(
                CompressionResult(
                    originalVideo = sourceMetadata,
                    compressedVideo = compressedMeta,
                    outputFilePath = outputPath,
                    compressionDurationMs = totalTime,
                    configUsed = config
                )
            )
        } else {
            Result.failure(compressedMetaResult.exceptionOrNull() ?: IllegalStateException("Failed to inspect output video."))
        }
    }

    override fun cancelActiveCompression() {
        compressor.cancelActiveSession()
    }

    override suspend fun saveToGallery(filePath: String, suggestedName: String): Result<Uri> {
        return mediaStoreSaver.saveVideoToGallery(filePath, suggestedName)
    }
}
