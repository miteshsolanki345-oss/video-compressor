package com.videocompressor.app.presentation.ui.components

import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Compare
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.ViewColumn
import androidx.compose.material.icons.rounded.ZoomIn
import androidx.compose.material.icons.rounded.ZoomOutMap
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.videocompressor.app.presentation.theme.AccentEmerald
import com.videocompressor.app.presentation.theme.PrimaryIndigo
import com.videocompressor.app.presentation.theme.SurfaceDarkElevated
import com.videocompressor.app.presentation.theme.SurfaceDarkStroke
import com.videocompressor.app.presentation.theme.TextPrimary
import com.videocompressor.app.presentation.theme.TextSecondary
import com.videocompressor.app.util.Formatters
import kotlinx.coroutines.delay
import java.io.File
import kotlin.math.roundToInt

enum class ComparisonMode {
    SPLIT_SLIDER,
    SIDE_BY_SIDE
}

@OptIn(UnstableApi::class)
@Composable
fun SideBySideVideoPlayer(
    originalFilePath: String,
    compressedFilePath: String,
    aspectRatio: Float = 16f / 9f,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Dual ExoPlayer instances
    val playerOriginal = remember {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_ALL
            volume = 0f // Mute original to avoid echo
            setMediaItem(MediaItem.fromUri(Uri.fromFile(File(originalFilePath))))
            prepare()
        }
    }

    val playerCompressed = remember {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_ALL
            volume = 1f // Play audio from compressed file
            setMediaItem(MediaItem.fromUri(Uri.fromFile(File(compressedFilePath))))
            prepare()
        }
    }

    var isPlaying by remember { mutableStateOf(false) }
    var currentPositionMs by remember { mutableLongStateOf(0L) }
    var totalDurationMs by remember { mutableLongStateOf(1L) }
    var comparisonMode by remember { mutableStateOf(ComparisonMode.SPLIT_SLIDER) }

    // Split slider position (0.0f = all compressed, 1.0f = all original, 0.5f = half-half)
    var splitFraction by remember { mutableFloatStateOf(0.5f) }

    // Zoom & Pan state
    var zoomScale by remember { mutableFloatStateOf(1f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }

    val transformState = rememberTransformableState { zoomChange, offsetChange, _ ->
        zoomScale = (zoomScale * zoomChange).coerceIn(1f, 4f)
        if (zoomScale > 1f) {
            panOffset += offsetChange
        } else {
            panOffset = Offset.Zero
        }
    }

    // Synchronize play state
    DisposableEffect(Unit) {
        onDispose {
            playerOriginal.release()
            playerCompressed.release()
        }
    }

    // Periodic progress polling for seek bar
    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            currentPositionMs = playerCompressed.currentPosition
            totalDurationMs = playerCompressed.duration.coerceAtLeast(1L)
            delay(100)
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceDarkElevated)
            .border(1.dp, SurfaceDarkStroke, RoundedCornerShape(16.dp))
            .padding(12.dp)
    ) {
        Column {
            // Mode and Zoom Controls Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // View Mode Toggle (Split vs Side-by-Side)
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(2.dp)
                ) {
                    IconButton(
                        onClick = { comparisonMode = ComparisonMode.SPLIT_SLIDER },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Compare,
                            contentDescription = "Split Slider Mode",
                            tint = if (comparisonMode == ComparisonMode.SPLIT_SLIDER) PrimaryIndigo else TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = { comparisonMode = ComparisonMode.SIDE_BY_SIDE },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ViewColumn,
                            contentDescription = "Side by Side Mode",
                            tint = if (comparisonMode == ComparisonMode.SIDE_BY_SIDE) PrimaryIndigo else TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Zoom Indicator / Reset Button
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (zoomScale > 1f) {
                        IconButton(
                            onClick = {
                                zoomScale = 1f
                                panOffset = Offset.Zero
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ZoomOutMap,
                                contentDescription = "Reset Zoom",
                                tint = AccentEmerald,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.Rounded.ZoomIn,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${String.format("%.1f", zoomScale)}x (Pinch to inspect)",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (zoomScale > 1f) AccentEmerald else TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Main Video Inspection Viewport
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(aspectRatio.coerceIn(0.5f, 2.2f))
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black)
                    .clipToBounds()
                    .transformable(state = transformState)
                    .graphicsLayer {
                        scaleX = zoomScale
                        scaleY = zoomScale
                        translationX = panOffset.x
                        translationY = panOffset.y
                    }
            ) {
                val containerWidth = maxWidth
                val containerHeight = maxHeight

                if (comparisonMode == ComparisonMode.SPLIT_SLIDER) {
                    // Split-Slider Mode:
                    // Original video on background
                    AndroidView(
                        factory = { ctx ->
                            PlayerView(ctx).apply {
                                player = playerOriginal
                                useController = false
                                layoutParams = FrameLayout.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    // Compressed video clipped to right of slider
                    val clipWidth = containerWidth * (1f - splitFraction)
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .width(clipWidth)
                            .height(containerHeight)
                            .clipToBounds()
                    ) {
                        AndroidView(
                            factory = { ctx ->
                                PlayerView(ctx).apply {
                                    player = playerCompressed
                                    useController = false
                                    layoutParams = FrameLayout.LayoutParams(
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                        ViewGroup.LayoutParams.MATCH_PARENT
                                    )
                                }
                            },
                            modifier = Modifier
                                .width(containerWidth)
                                .height(containerHeight)
                                .align(Alignment.CenterEnd)
                        )
                    }

                    // Draggable Vertical Divider Line
                    val sliderX = containerWidth * splitFraction
                    Box(
                        modifier = Modifier
                            .offset { IntOffset(x = sliderX.toPx().roundToInt() - 16, y = 0) }
                            .width(32.dp)
                            .height(containerHeight)
                            .pointerInput(Unit) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    val newFraction = (splitFraction + (dragAmount.x / size.width.toFloat()))
                                    splitFraction = newFraction.coerceIn(0.05f, 0.95f)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        // Vertical hairline
                        Box(
                            modifier = Modifier
                                .width(2.dp)
                                .height(containerHeight)
                                .background(Color.White)
                        )
                        // Center grab handle
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                                .border(2.dp, PrimaryIndigo, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Compare,
                                contentDescription = null,
                                tint = PrimaryIndigo,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    // Badges on viewport corners
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.Black.copy(alpha = 0.7f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "ORIGINAL",
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(AccentEmerald.copy(alpha = 0.85f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "COMPRESSED (HEVC)",
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                } else {
                    // Side-by-side Dual View
                    Row(modifier = Modifier.fillMaxSize()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(containerHeight)
                                .border(0.5.dp, SurfaceDarkStroke)
                        ) {
                            AndroidView(
                                factory = { ctx ->
                                    PlayerView(ctx).apply {
                                        player = playerOriginal
                                        useController = false
                                        layoutParams = FrameLayout.LayoutParams(
                                            ViewGroup.LayoutParams.MATCH_PARENT,
                                            ViewGroup.LayoutParams.MATCH_PARENT
                                        )
                                    }
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                            Text(
                                text = "ORIGINAL",
                                color = Color.White,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                modifier = Modifier
                                    .padding(6.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color.Black.copy(alpha = 0.6f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(containerHeight)
                                .border(0.5.dp, SurfaceDarkStroke)
                        ) {
                            AndroidView(
                                factory = { ctx ->
                                    PlayerView(ctx).apply {
                                        player = playerCompressed
                                        useController = false
                                        layoutParams = FrameLayout.LayoutParams(
                                            ViewGroup.LayoutParams.MATCH_PARENT,
                                            ViewGroup.LayoutParams.MATCH_PARENT
                                        )
                                    }
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                            Text(
                                text = "COMPRESSED",
                                color = Color.White,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                modifier = Modifier
                                    .padding(6.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(AccentEmerald.copy(alpha = 0.8f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Synchronized Playback Controls & Scrubber
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        if (isPlaying) {
                            playerOriginal.pause()
                            playerCompressed.pause()
                            isPlaying = false
                        } else {
                            // Sync playback position
                            val pos = playerCompressed.currentPosition
                            playerOriginal.seekTo(pos)
                            playerOriginal.play()
                            playerCompressed.play()
                            isPlaying = true
                        }
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(PrimaryIndigo)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Slider(
                        value = currentPositionMs.toFloat().coerceIn(0f, totalDurationMs.toFloat()),
                        onValueChange = { newPos ->
                            currentPositionMs = newPos.toLong()
                            playerOriginal.seekTo(newPos.toLong())
                            playerCompressed.seekTo(newPos.toLong())
                        },
                        valueRange = 0f..totalDurationMs.toFloat(),
                        colors = SliderDefaults.colors(
                            thumbColor = PrimaryIndigo,
                            activeTrackColor = PrimaryIndigo
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = Formatters.formatDuration(currentPositionMs),
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                        Text(
                            text = Formatters.formatDuration(totalDurationMs),
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                }
            }
        }
    }
}
