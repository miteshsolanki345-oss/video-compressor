package com.videocompressor.app.data.compression

import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFmpegSession
import com.arthenica.ffmpegkit.ReturnCode
import com.videocompressor.app.data.storage.StorageHelper
import com.videocompressor.app.domain.model.AudioMode
import com.videocompressor.app.domain.model.CompressionConfig
import com.videocompressor.app.domain.model.CompressionProgress
import com.videocompressor.app.domain.model.VideoMetadata
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.io.File

class FFmpegVideoCompressor {

    private var activeSessionId: Long? = null

    /**
     * Executes the FFmpeg compression pipeline emitting real-time progress.
     */
    fun compress(
        inputMetadata: VideoMetadata,
        outputFilePath: String,
        config: CompressionConfig
    ): Flow<CompressionProgress> = callbackFlow {
        val totalDurationMs = inputMetadata.durationMs.coerceAtLeast(1L)
        val startTime = System.currentTimeMillis()

        // Build command arguments list
        val commandArgs = buildFFmpegCommand(
            inputMetadata = inputMetadata,
            outputPath = outputFilePath,
            config = config
        )

        val commandString = commandArgs.joinToString(" ")

        // Execute async session with statistics and completion callback
        val session = FFmpegKit.executeAsync(
            commandString,
            { completedSession ->
                activeSessionId = null
                val returnCode = completedSession.returnCode
                if (ReturnCode.isSuccess(returnCode)) {
                    val elapsedTime = System.currentTimeMillis() - startTime
                    trySend(
                        CompressionProgress(
                            percentage = 100f,
                            currentFps = 0f,
                            processedDurationMs = totalDurationMs,
                            totalDurationMs = totalDurationMs,
                            speedMultiplier = 1.0,
                            estimatedTimeRemainingSeconds = 0L,
                            elapsedTimeMs = elapsedTime
                        )
                    )
                    close()
                } else if (ReturnCode.isCancel(returnCode)) {
                    StorageHelper.cleanupFile(outputFilePath)
                    close(CancellationException("Compression was cancelled by user"))
                } else {
                    StorageHelper.cleanupFile(outputFilePath)
                    val logs = completedSession.allLogsAsString
                    close(IllegalStateException("FFmpeg compression failed (code ${returnCode.value}): $logs"))
                }
            },
            { /* log callback */ },
            { stats ->
                val timeMs = stats.time.coerceAtLeast(0)
                val percentage = ((timeMs.toFloat() / totalDurationMs) * 100f).coerceIn(0f, 99f)
                val speed = stats.speed.coerceAtLeast(0.01)
                val remainingMs = (totalDurationMs - timeMs).coerceAtLeast(0L)
                val etaSeconds = ((remainingMs / 1000.0) / speed).toLong()
                val elapsedTime = System.currentTimeMillis() - startTime

                val progress = CompressionProgress(
                    percentage = percentage,
                    currentFps = stats.videoFps,
                    processedDurationMs = timeMs,
                    totalDurationMs = totalDurationMs,
                    currentBitrateKbps = stats.bitrate,
                    speedMultiplier = speed,
                    estimatedTimeRemainingSeconds = etaSeconds,
                    elapsedTimeMs = elapsedTime
                )
                trySend(progress)
            }
        )

        activeSessionId = session.sessionId

        awaitClose {
            if (activeSessionId != null) {
                cancelActiveSession()
                StorageHelper.cleanupFile(outputFilePath)
            }
        }
    }

    /**
     * Cancels the currently running FFmpeg compression session immediately.
     */
    fun cancelActiveSession() {
        val id = activeSessionId
        if (id != null) {
            FFmpegKit.cancel(id)
            activeSessionId = null
        } else {
            FFmpegKit.cancel()
        }
    }

    /**
     * Constructs the optimized FFmpeg command line arguments ensuring:
     * 1. 100% Strict passthrough resolution (dimension evenness filter for HEVC).
     * 2. High-efficiency HEVC / H.265 compression with perceptual CRF.
     * 3. Universal compatibility via Apple/Android standard FourCC tag `hvc1` and `yuv420p`.
     * 4. Quick stream startup via `+faststart`.
     * 5. Accurate audio passthrough or AAC encode.
     */
    fun buildFFmpegCommand(
        inputMetadata: VideoMetadata,
        outputPath: String,
        config: CompressionConfig
    ): List<String> {
        val args = mutableListOf<String>()

        // Overwrite output without prompting
        args.add("-y")

        // Input file
        args.add("-i")
        args.add("\"${inputMetadata.localFilePath}\"")

        // Preserve all metadata & rotation flags
        if (config.preserveMetadata) {
            args.add("-map_metadata")
            args.add("0")
        }

        // Strict Passthrough Resolution Filter:
        // libx265 requires width & height to be even numbers.
        // trunc(iw/2)*2 preserves 100% of dimensions if even, or adjusts by 1 pixel if odd.
        args.add("-vf")
        args.add("\"scale=trunc(iw/2)*2:trunc(ih/2)*2\"")

        // Video codec & Rate Control
        if (config.useHardwareAcceleration) {
            // Android MediaCodec HEVC hardware encoder
            args.add("-c:v")
            args.add("hevc_mediacodec")
            args.add("-b:v")
            // Target bitrate based on CRF approximation
            val targetBps = calculateHardwareBitrate(config.crf)
            args.add("${targetBps}k")
        } else {
            // Software libx265 - highest compression ratio & superior visual quality at CRF
            args.add("-c:v")
            args.add("libx265")
            args.add("-crf")
            args.add(config.crf.toString())
            args.add("-preset")
            args.add(config.ffmpegPreset)
        }

        // Pixel format for universal hardware decoder compatibility
        args.add("-pix_fmt")
        args.add("yuv420p")

        // MP4 standard FourCC tag for HEVC: hvc1 ensures playback on iOS, macOS, Windows and Android gallery
        args.add("-tag:v")
        args.add("hvc1")

        // Audio options: PCM in MOV/AVI cannot be copied directly into MP4 without muxer error
        val isPcmAudio = inputMetadata.audioCodec?.contains("pcm", ignoreCase = true) == true
        when (config.audioMode) {
            AudioMode.COPY_PASSTHROUGH -> {
                if (isPcmAudio) {
                    args.add("-c:a")
                    args.add("aac")
                    args.add("-b:a")
                    args.add("192k")
                } else {
                    args.add("-c:a")
                    args.add("copy")
                }
            }
            AudioMode.AAC_192K -> {
                args.add("-c:a")
                args.add("aac")
                args.add("-b:a")
                args.add("192k")
            }
            AudioMode.AAC_160K -> {
                args.add("-c:a")
                args.add("aac")
                args.add("-b:a")
                args.add("160k")
            }
            AudioMode.AAC_128K -> {
                args.add("-c:a")
                args.add("aac")
                args.add("-b:a")
                args.add("128k")
            }
        }

        // Container optimization: move moov atom to the front for instant ExoPlayer loading
        if (config.fastStart) {
            args.add("-movflags")
            args.add("+faststart")
        }

        // Output file
        args.add("\"$outputPath\"")

        return args
    }

    private fun calculateHardwareBitrate(crf: Int): Int {
        return when {
            crf <= 20 -> 8000 // 8 Mbps high quality
            crf <= 24 -> 4500 // 4.5 Mbps balanced
            else -> 2500      // 2.5 Mbps high compression
        }
    }

    class CancellationException(message: String) : Exception(message)
}
