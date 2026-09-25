package com.videocompressor.app.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.videocompressor.app.domain.model.CompressionResult
import com.videocompressor.app.presentation.theme.AccentEmerald
import com.videocompressor.app.presentation.theme.EmeraldContainer
import com.videocompressor.app.presentation.theme.OnEmeraldContainer
import com.videocompressor.app.presentation.theme.SurfaceDarkElevated
import com.videocompressor.app.presentation.theme.SurfaceDarkStroke
import com.videocompressor.app.presentation.theme.TextPrimary
import com.videocompressor.app.presentation.theme.TextSecondary
import com.videocompressor.app.util.Formatters

@Composable
fun StatComparisonRow(
    result: CompressionResult,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceDarkElevated)
            .border(1.dp, SurfaceDarkStroke, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Column {
            // Header with Saved Badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "COMPRESSION EFFICIENCY",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                    Text(
                        text = Formatters.formatPercentageSaved(result.percentageSaved),
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = AccentEmerald
                        )
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(EmeraldContainer)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.ArrowDownward,
                            contentDescription = null,
                            tint = OnEmeraldContainer,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "-${Formatters.formatFileSize(result.bytesSaved)}",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = OnEmeraldContainer
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Metrics table
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricRow(
                    label = "File Size",
                    original = Formatters.formatFileSize(result.originalVideo.fileSizeBytes),
                    compressed = Formatters.formatFileSize(result.compressedVideo.fileSizeBytes),
                    isHighlighted = true
                )

                MetricRow(
                    label = "Bitrate",
                    original = Formatters.formatBitrate(result.originalVideo.bitrateBps),
                    compressed = Formatters.formatBitrate(result.compressedVideo.bitrateBps),
                    isHighlighted = false
                )

                MetricRow(
                    label = "Resolution",
                    original = result.originalVideo.resolutionLabel,
                    compressed = "${result.compressedVideo.resolutionLabel} (100% Same)",
                    isHighlighted = false,
                    isExactMatch = true
                )

                MetricRow(
                    label = "Codec",
                    original = result.originalVideo.videoCodec.substringAfterLast("/"),
                    compressed = "HEVC / H.265 (hvc1)",
                    isHighlighted = false
                )
            }
        }
    }
}

@Composable
private fun MetricRow(
    label: String,
    original: String,
    compressed: String,
    isHighlighted: Boolean = false,
    isExactMatch: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            modifier = Modifier.width(90.dp)
        )

        Text(
            text = original,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            modifier = Modifier.weight(1f)
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1.3f)
        ) {
            if (isExactMatch) {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    tint = AccentEmerald,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                text = compressed,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.Medium
                ),
                color = if (isHighlighted) AccentEmerald else TextPrimary
            )
        }
    }
}
