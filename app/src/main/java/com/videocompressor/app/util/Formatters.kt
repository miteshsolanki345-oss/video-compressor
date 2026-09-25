package com.videocompressor.app.util

import java.util.Locale

object Formatters {

    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt().coerceIn(0, units.size - 1)
        val value = bytes / Math.pow(1024.0, digitGroups.toDouble())
        return String.format(Locale.US, "%.1f %s", value, units[digitGroups])
    }

    fun formatDuration(durationMs: Long): String {
        val totalSeconds = (durationMs / 1000).coerceAtLeast(0)
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60

        return if (hours > 0) {
            String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.US, "%02d:%02d", minutes, seconds)
        }
    }

    fun formatBitrate(bitrateBps: Long): String {
        if (bitrateBps <= 0) return "Unknown"
        val kbps = bitrateBps / 1000.0
        return if (kbps >= 1000) {
            String.format(Locale.US, "%.1f Mbps", kbps / 1000.0)
        } else {
            String.format(Locale.US, "%.0f kbps", kbps)
        }
    }

    fun formatFps(fps: Float): String {
        return if (fps <= 0) "30 fps" else String.format(Locale.US, "%.1f fps", fps)
    }

    fun formatPercentageSaved(percentage: Float): String {
        return String.format(Locale.US, "%.1f%% Saved", percentage)
    }
}
