package com.videocompressor.app.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.videocompressor.app.domain.model.AudioMode
import com.videocompressor.app.domain.model.CompressionConfig
import com.videocompressor.app.presentation.theme.PrimaryIndigo
import com.videocompressor.app.presentation.theme.SurfaceDarkElevated
import com.videocompressor.app.presentation.theme.SurfaceDarkStroke
import com.videocompressor.app.presentation.theme.TextPrimary
import com.videocompressor.app.presentation.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomSettingsCard(
    config: CompressionConfig,
    onCrfChanged: (Int) -> Unit,
    onFfmpegPresetChanged: (String) -> Unit,
    onAudioModeChanged: (AudioMode) -> Unit,
    onHardwareToggled: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val presets = listOf("ultrafast", "fast", "medium", "slow", "veryslow")
    var presetDropdownExpanded by remember { mutableStateOf(false) }
    var audioDropdownExpanded by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceDarkElevated)
            .border(1.dp, SurfaceDarkStroke, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Column {
            Text(
                text = "CUSTOM ENCODING CONTROLS",
                style = MaterialTheme.typography.labelSmall,
                color = PrimaryIndigo,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            // CRF Slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Constant Rate Factor (CRF):",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "${config.crf}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = PrimaryIndigo
                )
            }

            Slider(
                value = config.crf.toFloat(),
                onValueChange = { onCrfChanged(it.toInt()) },
                valueRange = 16f..32f,
                steps = 15,
                colors = SliderDefaults.colors(
                    thumbColor = PrimaryIndigo,
                    activeTrackColor = PrimaryIndigo
                )
            )

            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "16 (Lossless / Huge)",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "32 (Very High Shrink)",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Speed preset dropdown
            ExposedDropdownMenuBox(
                expanded = presetDropdownExpanded,
                onExpandedChange = { presetDropdownExpanded = !presetDropdownExpanded }
            ) {
                OutlinedTextField(
                    value = config.ffmpegPreset.replaceFirstChar { it.uppercase() },
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Encoder Speed Preset") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = presetDropdownExpanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = presetDropdownExpanded,
                    onDismissRequest = { presetDropdownExpanded = false }
                ) {
                    presets.forEach { presetOption ->
                        DropdownMenuItem(
                            text = { Text(presetOption.replaceFirstChar { it.uppercase() }) },
                            onClick = {
                                onFfmpegPresetChanged(presetOption)
                                presetDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Audio Mode dropdown
            ExposedDropdownMenuBox(
                expanded = audioDropdownExpanded,
                onExpandedChange = { audioDropdownExpanded = !audioDropdownExpanded }
            ) {
                OutlinedTextField(
                    value = config.audioMode.displayName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Audio Encoding Mode") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = audioDropdownExpanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = audioDropdownExpanded,
                    onDismissRequest = { audioDropdownExpanded = false }
                ) {
                    AudioMode.entries.forEach { mode ->
                        DropdownMenuItem(
                            text = { Text(mode.displayName) },
                            onClick = {
                                onAudioModeChanged(mode)
                                audioDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Hardware acceleration switch
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Hardware Acceleration",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = TextPrimary
                    )
                    Text(
                        text = if (config.useHardwareAcceleration) "hevc_mediacodec (Faster encode)" else "libx265 software (Highest quality & compression)",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }

                Switch(
                    checked = config.useHardwareAcceleration,
                    onCheckedChange = onHardwareToggled,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = PrimaryIndigo,
                        checkedTrackColor = PrimaryIndigo.copy(alpha = 0.5f)
                    )
                )
            }
        }
    }
}
