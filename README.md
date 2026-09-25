# Video Shrink (Native Android "Same-to-Same" Video Compressor)

A native, 100% offline/local Android application built with **Jetpack Compose**, **Material 3**, and **FFmpegKit** that compresses video files down to smaller sizes while maintaining the original resolution and perceptually identical visual quality (**"Same-to-Same"**).

---

## Architecture & Technology Stack

- **UI & Design**: Modern Jetpack Compose with Material 3, Dark Theme, Edge-to-Edge display.
- **Architecture**: Clean Architecture with MVI/MVVM pattern, Coroutines, and StateFlow.
- **Processing Engine**: `com.arthenica:ffmpeg-kit-full` running completely on-device without remote servers.
- **Video Inspection & Playback**: Jetpack `androidx.media3:media3-exoplayer` and `media3-ui`.
- **Background Execution**: Android Foreground Service with WakeLock and ongoing progress notification (`ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC`).
- **Storage & Privacy**: Android Photo Picker (`ActivityResultContracts.PickVisualMedia`) and Scoped Storage MediaStore API (`Movies/VideoCompressor`). Zero broad storage permissions needed.

---

## "Same-to-Same" Compression Pipeline

The core challenge of video compression is achieving substantial size reduction without introducing blockiness, banding, edge ringing, or blurring. This app solves it using:

1. **Resolution Passthrough Filter**:
   ```
   -vf "scale=trunc(iw/2)*2:trunc(ih/2)*2"
   ```
   Ensures 100% of original pixel resolution is retained (4K, 1080p, 720p) while guaranteeing width/height are even (a requirement for HEVC macroblocks).

2. **HEVC / H.265 Codec**:
   Encodes via `libx265` (or hardware `hevc_mediacodec`), delivering ~50% better bitrate efficiency than H.264 at identical visual quality.

3. **Perceptually Calibrated Constant Rate Factor (CRF)**:
   - **Perceptually Lossless** (`CRF 19`, `preset: medium`): Negligible compression gain, zero visible artifacts.
   - **Balanced / Smart Shrink** (`CRF 23`, `preset: medium`): Typically 40–60% file size reduction with indistinguishable visual quality.
   - **High Compression** (`CRF 27`, `preset: faster`): Aggressive 65–80% reduction while retaining original frame resolution.
   - **Custom Mode**: User-selectable CRF (16–32), encoder speed preset, and audio mode.

4. **Apple & Android QuickTime Compatibility**:
   ```
   -tag:v hvc1 -pix_fmt yuv420p
   ```
   Tags HEVC video as `hvc1` and forces standard 8-bit `yuv420p` pixel format so the compressed MP4 plays universally across Android Gallery, iOS Photos, macOS QuickTime, and web browsers.

5. **Fast Streaming Startup**:
   ```
   -movflags +faststart
   ```
   Relocates the `moov` index atom to the beginning of the file for instantaneous playback start in ExoPlayer without waiting for full file buffering.

---

## Key Features

- **Side-by-Side & Split Wipe Player**:
  - **Draggable Split Slider**: Live interactive slider dividing original and compressed streams on the same frame.
  - **Side-by-Side Dual Mode**: Simultaneous side-by-side video rendering.
  - **Synchronized Playback**: Both players play, pause, and scrub together frame-accurately.
  - **Pinch-to-Zoom (1x to 4x) & Pan**: Zoom into skin texture, text, grass, and edges to inspect pixel-perfect fidelity.
- **Live Background Encoding**:
  - Real-time percentage, current encoding FPS, speed multiplier (e.g. 1.8x), and ETA remaining.
  - Foreground Service with notification and cancel action to prevent OS kill when app is minimized.
- **Gallery Save & Share**:
  - Scoped Storage compliant saving to `Movies/VideoCompressor` via `MediaStore.Video.Media`.
  - Android Share Sheet integration via `FileProvider`.

---

## Project Structure

```
video-compressor-android/
├── app/
│   ├── src/main/
│   │   ├── AndroidManifest.xml
│   │   ├── java/com/videocompressor/app/
│   │   │   ├── VideoCompressorApp.kt           # Application init & notification channel
│   │   │   ├── MainActivity.kt                 # Edge-to-edge Compose navigation
│   │   │   ├── domain/
│   │   │   │   ├── model/
│   │   │   │   │   ├── VideoMetadata.kt        # Video specs, dimensions & bitrates
│   │   │   │   │   ├── CompressionPreset.kt    # Profiles (Lossless, Balanced, High, Custom)
│   │   │   │   │   ├── CompressionConfig.kt    # Parameters passed to FFmpeg
│   │   │   │   │   ├── CompressionProgress.kt  # Real-time encoding stats (FPS, ETA, %)
│   │   │   │   │   └── CompressionResult.kt    # Before vs After comparison result
│   │   │   │   └── repository/
│   │   │   │       └── VideoCompressionRepository.kt
│   │   │   ├── data/
│   │   │   │   ├── compression/
│   │   │   │   │   ├── FFmpegVideoCompressor.kt    # FFmpeg command builder & runner
│   │   │   │   │   └── MediaMetadataExtractor.kt   # Metadata & track analysis
│   │   │   │   ├── storage/
│   │   │   │   │   ├── MediaStoreSaver.kt          # Scoped Storage gallery export
│   │   │   │   │   └── StorageHelper.kt            # Free space check & temp cleanup
│   │   │   │   ├── service/
│   │   │   │   │   └── VideoCompressionService.kt   # Foreground Service with notification
│   │   │   │   └── repository/
│   │   │   │       └── VideoCompressionRepositoryImpl.kt
│   │   │   ├── presentation/
│   │   │   │   ├── theme/                      # Sleek dark theme, colors & typography
│   │   │   │   ├── viewmodel/                  # CompressorViewModel & MVI State
│   │   │   │   └── ui/
│   │   │   │       ├── components/
│   │   │   │       │   ├── VideoInfoCard.kt
│   │   │   │       │   ├── PresetSelectorCard.kt
│   │   │   │       │   ├── CustomSettingsCard.kt
│   │   │   │       │   ├── StatComparisonRow.kt
│   │   │   │       │   └── SideBySideVideoPlayer.kt # Synchronized dual ExoPlayer + Zoom
│   │   │   │       └── screens/
│   │   │   │           ├── HomeScreen.kt
│   │   │   │           ├── CompressionProgressScreen.kt
│   │   │   │           └── ComparisonScreen.kt
│   │   │   └── util/
│   │   │       ├── Formatters.kt               # Byte, duration & bitrate formatters
│   │   │       └── FileProviderUtil.kt         # Secure URI sharing
│   │   └── res/
│   └── build.gradle.kts
├── gradle/
│   └── libs.versions.toml                      # Modern Gradle Version Catalog
├── build.gradle.kts
├── settings.gradle.kts
└── gradlew.bat
```

---

## How to Open and Run

1. Open **Android Studio** (Koala / Ladybug or newer recommended).
2. Choose **Open** and select:
   `C:\Users\Mitesh-Solanki\.gemini\antigravity\scratch\video-compressor-android`
3. Allow Gradle to sync dependencies from Google Maven and Maven Central.
4. Connect an Android device or launch an emulator (API 26 to 35).
5. Click **Run 'app'** (`Shift + F10`).
