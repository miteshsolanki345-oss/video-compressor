package com.videocompressor.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.videocompressor.app.presentation.theme.BackgroundDark
import com.videocompressor.app.presentation.theme.VideoCompressorTheme
import com.videocompressor.app.presentation.ui.screens.ComparisonScreen
import com.videocompressor.app.presentation.ui.screens.CompressionProgressScreen
import com.videocompressor.app.presentation.ui.screens.HomeScreen
import com.videocompressor.app.presentation.viewmodel.CompressorUiEvent
import com.videocompressor.app.presentation.viewmodel.CompressorUiState
import com.videocompressor.app.presentation.viewmodel.CompressorViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            VideoCompressorTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = BackgroundDark
                ) {
                    CompressorApp()
                }
            }
        }
    }
}

@Composable
fun CompressorApp(viewModel: CompressorViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()

    // Request notification permission for Android 13+ (API 33+)
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { /* Permission granted or denied */ }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // Intercept back button when compressing or completed
    BackHandler(enabled = uiState !is CompressorUiState.Idle) {
        when (uiState) {
            is CompressorUiState.Compressing -> {
                viewModel.onEvent(CompressorUiEvent.OnCancelCompressionClicked)
            }
            is CompressorUiState.Completed -> {
                viewModel.onEvent(CompressorUiEvent.OnResetClicked)
            }
            is CompressorUiState.Ready -> {
                viewModel.onEvent(CompressorUiEvent.OnResetClicked)
            }
            else -> {}
        }
    }

    AnimatedContent(
        targetState = uiState,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "screenTransition"
    ) { state ->
        when (state) {
            is CompressorUiState.Compressing -> {
                CompressionProgressScreen(
                    state = state,
                    onCancelClicked = { viewModel.onEvent(CompressorUiEvent.OnCancelCompressionClicked) }
                )
            }
            is CompressorUiState.Completed -> {
                ComparisonScreen(
                    state = state,
                    onEvent = viewModel::onEvent
                )
            }
            else -> {
                HomeScreen(
                    uiState = state,
                    onEvent = viewModel::onEvent
                )
            }
        }
    }
}
