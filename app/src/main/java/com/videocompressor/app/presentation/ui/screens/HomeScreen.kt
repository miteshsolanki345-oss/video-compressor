package com.videocompressor.app.presentation.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.videocompressor.app.domain.model.CompressionPreset
import com.videocompressor.app.presentation.theme.AccentEmerald
import com.videocompressor.app.presentation.theme.BackgroundDark
import com.videocompressor.app.presentation.theme.PrimaryIndigo
import com.videocompressor.app.presentation.theme.PrimaryIndigoDark
import com.videocompressor.app.presentation.theme.SurfaceDarkElevated
import com.videocompressor.app.presentation.theme.SurfaceDarkStroke
import com.videocompressor.app.presentation.theme.TextPrimary
import com.videocompressor.app.presentation.theme.TextSecondary
import com.videocompressor.app.presentation.ui.components.CustomSettingsCard
import com.videocompressor.app.presentation.ui.components.PresetSelectorCard
import com.videocompressor.app.presentation.ui.components.VideoInfoCard
import com.videocompressor.app.presentation.viewmodel.CompressorUiEvent
import com.videocompressor.app.presentation.viewmodel.CompressorUiState
import com.videocompressor.app.util.Formatters

@Composable
fun HomeScreen(
    uiState: CompressorUiState,
    onEvent: (CompressorUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    // Android Photo Picker Launcher (zero broad storage permissions required)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            onEvent(CompressorUiEvent.OnVideoSelected(uri))
        }
    }

    // Android Document / File Picker Launcher (allows picking AVI, MOV, MKV from File Manager/Downloads)
    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            onEvent(CompressorUiEvent.OnVideoSelected(uri))
        }
    }

    val scrollState = rememberScrollState()

    Scaffold(
        containerColor = BackgroundDark,
        topBar = {
            TopHeader()
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (uiState) {
                is CompressorUiState.Idle -> {
                    EmptyPickerState(
                        onPickGalleryClicked = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                            )
                        },
                        onBrowseFilesClicked = {
                            documentPickerLauncher.launch(arrayOf("video/*"))
                        }
                    )
                }

                is CompressorUiState.Analyzing -> {
                    AnalyzingState(message = uiState.message)
                }

                is CompressorUiState.Ready -> {
                    ReadyState(
                        state = uiState,
                        onEvent = onEvent,
                        onPickAnotherClicked = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                            )
                        }
                    )
                }

                is CompressorUiState.Error -> {
                    ErrorState(
                        errorMessage = uiState.errorMessage,
                        onRetry = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                            )
                        }
                    )
                }

                else -> {
                    // Handled in separate screens
                }
            }
        }
    }
}

@Composable
private fun TopHeader() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(BackgroundDark)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Video Shrink",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )
                Text(
                    text = "Same-to-Same Lossless Quality",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }

            // Offline badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(PrimaryIndigo.copy(alpha = 0.15f))
                    .border(1.dp, PrimaryIndigo.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.CloudOff,
                        contentDescription = null,
                        tint = PrimaryIndigo,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "100% Offline",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = PrimaryIndigo
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyPickerState(
    onPickGalleryClicked: () -> Unit,
    onBrowseFilesClicked: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        // Supported formats badge
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, SurfaceDarkStroke, RoundedCornerShape(20.dp))
                .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            Text(
                text = "Supports MP4 • MOV • AVI • MKV • WebM • 3GP",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = AccentEmerald
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Large Pick Video Dropzone Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(SurfaceDarkElevated)
                .border(1.5.dp, PrimaryIndigo.copy(alpha = 0.35f), RoundedCornerShape(24.dp))
                .padding(vertical = 32.dp, horizontal = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(PrimaryIndigo, PrimaryIndigoDark)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.FolderOpen,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(34.dp)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = "Select a Video to Compress",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "High efficiency local HEVC re-encoding.\nZero visual loss, 100% offline.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Gallery Button
                Button(
                    onClick = onPickGalleryClicked,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .height(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.FolderOpen,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Choose from Gallery (Photo Picker)")
                }

                Spacer(modifier = Modifier.height(10.dp))

                // File Manager / Documents Button
                OutlinedButton(
                    onClick = onBrowseFilesClicked,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .height(46.dp)
                ) {
                    Text(text = "Browse Device Files (AVI, MOV, MKV...)", color = TextPrimary)
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Feature Highlights
        Text(
            text = "WHY SAME-TO-SAME COMPRESSION?",
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, bottom = 12.dp)
        )

        FeatureRow(
            icon = Icons.Rounded.CheckCircle,
            title = "Zero Downscaling",
            description = "Preserves 100% of original frame resolution (4K, 1080p, 60fps passthrough)."
        )

        Spacer(modifier = Modifier.height(10.dp))

        FeatureRow(
            icon = Icons.Rounded.Bolt,
            title = "Advanced HEVC / H.265 Codec",
            description = "Utilizes Constant Rate Factor (CRF 21-23) to eliminate redundancy without visual degradation."
        )

        Spacer(modifier = Modifier.height(10.dp))

        FeatureRow(
            icon = Icons.Rounded.Security,
            title = "Strict Privacy Guaranteed",
            description = "Encodes locally on your device CPU/GPU. No video ever leaves your phone."
        )
    }
}

@Composable
private fun ReadyState(
    state: CompressorUiState.Ready,
    onEvent: (CompressorUiEvent) -> Unit,
    onPickAnotherClicked: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Video Specs Card
        VideoInfoCard(metadata = state.videoMetadata)

        Spacer(modifier = Modifier.height(16.dp))

        // Preset Selector
        PresetSelectorCard(
            selectedPreset = state.config.preset,
            onPresetSelected = { onEvent(CompressorUiEvent.OnPresetSelected(it)) }
        )

        // Custom parameters if Custom mode selected
        AnimatedVisibility(visible = state.config.preset == CompressionPreset.CUSTOM) {
            Column {
                Spacer(modifier = Modifier.height(12.dp))
                CustomSettingsCard(
                    config = state.config,
                    onCrfChanged = { onEvent(CompressorUiEvent.OnCrfChanged(it)) },
                    onFfmpegPresetChanged = { onEvent(CompressorUiEvent.OnFfmpegPresetChanged(it)) },
                    onAudioModeChanged = { onEvent(CompressorUiEvent.OnAudioModeChanged(it)) },
                    onHardwareToggled = { onEvent(CompressorUiEvent.OnHardwareAccelerationToggled(it)) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Estimated output preview banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(AccentEmerald.copy(alpha = 0.12f))
                .border(1.dp, AccentEmerald.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                .padding(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.Bolt,
                    contentDescription = null,
                    tint = AccentEmerald,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Estimated Output Size",
                        style = MaterialTheme.typography.labelSmall,
                        color = AccentEmerald
                    )
                    Text(
                        text = "${Formatters.formatFileSize(state.estimatedSizeRange.first)} - ${Formatters.formatFileSize(state.estimatedSizeRange.second)} (Original: ${Formatters.formatFileSize(state.videoMetadata.fileSizeBytes)})",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Start Compression Button
        Button(
            onClick = { onEvent(CompressorUiEvent.OnStartCompressionClicked) },
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.PlayCircle,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Start Same-to-Same Compression",
                style = MaterialTheme.typography.labelLarge.copy(fontSize = 16.sp)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Pick Another Video Button
        OutlinedButton(
            onClick = onPickAnotherClicked,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Refresh,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "Choose Another Video", color = TextSecondary)
        }
    }
}

@Composable
private fun AnalyzingState(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(
                color = PrimaryIndigo,
                strokeWidth = 3.dp,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = TextPrimary
            )
        }
    }
}

@Composable
private fun ErrorState(errorMessage: String, onRetry: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 30.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(SurfaceDarkElevated)
                .padding(20.dp)
        ) {
            Text(
                text = "Compression Error",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Select Another Video")
            }
        }
    }
}

@Composable
private fun FeatureRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceDarkElevated)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(PrimaryIndigo.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = PrimaryIndigo,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = TextPrimary
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }
    }
}
