# AMVerge Android

High-performance native Android application for anime video editing, hardware-accelerated scene detection, timeline inspection, scenepack archiving, and on-device neural video processing.

AMVerge Android ports the full desktop AMVerge workflow to mobile devices, enabling anime music video (AMV) editors, archivists, and content creators to extract cuts, generate DaVinci Resolve and Premiere Pro timelines, and run AI neural passes directly on Android.

---

## Key Features

### 1. Hardware-Accelerated Scene Detection
- **Native Keyframe Demuxing:** Uses Android's `MediaExtractor` to parse video container packet flags and keyframe timestamps directly, extracting scene boundaries in seconds without decoding every uncompressed frame.
- **Open-GOP Filtering:** Automatically detects and filters out unsafe open-GOP boundaries containing leading pictures to eliminate seek stutter and visual tearing.
- **Deadframe Static Shot Filter:** Analyzes motion variance to classify scenes as dynamic action shots or static talking-head cuts.
- **Transition Detection:** Identifies gradual transitions such as fades and dips to black or white.
- **Configurable Settings:** Adjustable parameters for minimum scene duration (ms), motion threshold, and transition sensitivity.

### 2. Timeline, Card Scrubbing & Cut Inspection
- **Interactive Timeline Overview:** Proportional bar mapping all detected cuts across the full video duration with interactive horizontal drag scrubbing.
- **Card Thumbnail Scrubbing:** Horizontal touch gestures across any cut card dynamically scrub through that cut with real-time delta timecodes.
- **Scene Preview Console:** Full-resolution player with frame-by-frame stepping (+/-1 frame), playhead split cut, A/B looping, and variable playback speeds (0.25x to 2.0x).
- **Multi-Selection Dock:** Ergonomic bottom dock for batch operations: Select All, Invert Selection, Merge adjacent cuts, Delete, Star/Favorite, and direct Scenepack bundling.

### 3. Professional Export & Desktop Editor Parity
- **Non-Destructive CMX 3600 EDL Export:** Generates industry-standard Edit Decision List (EDL) manifests with precise 24/30/60 fps timecodes ready to import directly into DaVinci Resolve, Adobe Premiere Pro, and Final Cut Pro.
- **Multi-Ratio Social Cropping:** Aspect ratio crop engine supporting Original, 9:16 (TikTok, Reels, Shorts), 1:1 (Square), and 4:3 (Retro).
- **Audio Control:** Retain original audio or mute audio tracks for clean, audio-free AMV cut export.
- **Batch & Merged Rendering:** Stream copy cuts or concatenated chronological reels exported directly to device storage (`Movies/AMVerge`).

### 4. Anime Series Library & Scenepack Archiving
- **Series Library:** Organizes imported episodes by anime title (e.g. Jujutsu Kaisen, Demon Slayer) for multi-episode project tracking.
- **Scenepack Creator:** Collects curated cuts across episodes into organized collections.
- **ZIP Packaging:** Automatically exports cuts and a CMX 3600 EDL manifest into portable `.zip` archives delivered to `Movies/AMVerge`.

### 5. On-Device AI Neural Suite
- **16x Optical Flow (RIFE):** Frame interpolation up to 120 fps slow motion with RIFE v4.6 Lite and RIFE Anime model presets.
- **4x Video Super-Resolution (Real-ESRGAN):** Neural video upscaler with Fast, Anime, and Quality sharpness configurations.
- **MiDaS v3.1 3D Depth Map:** Extracts depth maps with customizable colormaps (Inferno, Viridis, Turbo, Plasma, Grayscale) for 3D parallax effects.
- **AI Studio Screen:** Dedicated workspace to test any video clip on device with live progress tracking and side-by-side video playback.

---

## Technical Specifications

| Component | Technology |
| :--- | :--- |
| **Language** | Kotlin 2.0 |
| **UI Framework** | Jetpack Compose (Material 3) |
| **Architecture** | Clean Architecture (Domain / Data / Frontend) |
| **Local Database** | Room (SQLite) with Coroutines and Flow |
| **Media Playback** | AndroidX Media3 (ExoPlayer 1.4.1) |
| **Demuxing & Media** | Android MediaExtractor, FFmpeg / FFprobe |
| **Neural Runtime** | ONNX Runtime Mobile |
| **Image Loading** | Coil Compose |
| **Android Target** | Min SDK 26 (Android 8.0), Target SDK 35 (Android 15) |
| **Page Alignment** | Android 15 16 KB ELF page alignment for all native C++ binaries |
| **JDK Version** | JDK 21 |

---

## Project Structure

```text
app/src/main/
├── AndroidManifest.xml
└── java/com/amverge/android/
    ├── app/
    │   ├── AMVergeApplication.kt      # Application entrypoint & dependency injection
    │   ├── MainActivity.kt            # Single Activity Compose container
    │   ├── AppContainer.kt            # Central dependency registry
    │   └── navigation/
    │       ├── Screen.kt              # Type-safe navigation routes
    │       └── AMVergeNavHost.kt      # Compose navigation graph
    │
    ├── frontend/
    │   ├── theme/                     # Material 3 colors, typography, theme
    │   ├── state/                     # UI view states (Import, SceneGrid, Preview, Export, AI)
    │   ├── viewmodel/                 # ViewModels (StateFlow state holders)
    │   ├── components/                # Modular UI components (SceneCard, Dialogs, Timeline)
    │   └── screens/
    │       ├── import/                # ImportScreen & Studio Hub
    │       ├── scenes/                # SceneGridScreen (adaptive cut grid)
    │       ├── preview/               # ScenePreviewScreen (cut inspector & player)
    │       ├── library/               # SeriesLibraryScreen & Scenepacks
    │       └── ai/                    # AiStudioScreen (on-device neural passes)
    │
    ├── backend/
    │   ├── domain/
    │   │   ├── model/                 # EpisodeProject, Scene, Scenepack, Series, ExportRequest
    │   │   ├── repository/            # ProjectRepository, SceneRepository, SeriesRepository
    │   │   └── usecase/               # Scene detection, export, and AI pipeline usecases
    │   ├── data/
    │   │   ├── database/              # Room database, entities, and DAOs
    │   │   ├── mapper/                # Entity-to-Domain mappers
    │   │   └── repository/            # Repository implementations
    │   └── media/
    │       ├── ffmpeg/                # MediaEngine, FFprobe metadata extraction
    │       ├── detection/             # Hardware MediaExtractor keyframe detector & filters
    │       ├── edl/                   # CMX 3600 timeline manifest generator
    │       ├── ai/                    # RIFE flow, Real-ESRGAN upscaler, MiDaS depth estimator
    │       ├── cutting/               # Non-destructive stream copy cut extractor
    │       ├── merging/               # FFmpeg concat demuxer video merger
    │       └── thumbnail/             # High-speed scene poster generator
    │
    ├── platform/
    │   ├── storage/                   # Storage Access Framework (SAF) URI permission manager
    │   ├── notification/              # Android notification channels & foreground progress
    │   └── worker/                    # WorkManager background tasks
    │
    └── core/
        ├── common/                    # MediaTime conversion utilities
        ├── result/                    # OperationResult monad
        └── logging/                   # Structured logging
```

---

## Build & Installation Instructions

### Prerequisites
1. **Java Development Kit (JDK):** Version 21 (Required).
2. **Android SDK:** Platform 35 (Android 15), Build Tools 35.0.0.
3. **Android NDK:** Version 26.1.10909125 or higher (configured with 16 KB page-size support).

### Building via Command Line

1. **Clone the repository:**
   ```bash
   git clone https://github.com/your-username/amverge-android.git
   cd amverge-android
   ```

2. **Execute Unit Tests:**
   ```bash
   ./gradlew testDebugUnitTest --no-daemon
   ```

3. **Assemble the Debug APK:**
   ```bash
   ./gradlew assembleDebug --no-daemon
   ```

4. **Locate the Output Binary:**
   The assembled APK will be available at:
   ```text
   app/build/outputs/apk/debug/app-debug.apk
   ```

5. **Install on Device via ADB:**
   ```bash
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```

---

## Output Directories on Device

All exported files are written to standard user-accessible storage:
- **Individual and Merged Cuts:** `Primary Storage > Movies > AMVerge`
- **EDL Timelines (.edl):** `Primary Storage > Movies > AMVerge`
- **Scenepack Archives (.zip):** `Primary Storage > Movies > AMVerge`
- **AI Neural Processed Outputs:** `Primary Storage > Movies > AMVerge`

---

## Acknowledgements & Credits

AMVerge Android builds upon open-source research, neural network architectures, and multimedia engineering libraries. We gratefully credit and acknowledge the following projects and research teams:

### AI & Neural Network Models

- **RIFE (Real-Time Intermediate Flow Estimation)**
  - **Research & Authors:** Zhewei Huang, Tianyuan Zhang, Wen Heng, Boxin Shi, Shuchang Zhou (Megvii Research, Peking University).
  - **Publication:** *Real-Time Intermediate Flow Estimation for Video Frame Interpolation*, ECCV 2022.
  - **Repository:** [https://github.com/hzwer/Practical-RIFE](https://github.com/hzwer/Practical-RIFE)
  - **Usage:** Powers the on-device 16x Optical Flow frame interpolation engine up to 120 fps.

- **Real-ESRGAN**
  - **Research & Authors:** Xintao Wang, Liangbin Xie, Chao Dong, Ying Shan (ARC Lab, Tencent PCG; Shenzhen Institute of Advanced Technology).
  - **Publication:** *Real-ESRGAN: Training Real-World Blind Super-Resolution with Pure Synthetic Data*, ICCV Workshops 2021.
  - **Repository:** [https://github.com/xinntao/Real-ESRGAN](https://github.com/xinntao/Real-ESRGAN)
  - **Usage:** Powers the 4x video super-resolution and anime sharpness enhancement engine.

- **MiDaS (Monocular Depth Estimation)**
  - **Research & Authors:** Rene Ranftl, Katrin Lasinger, David Hafner, Konrad Schindler, Vladlen Koltun (Intel Intelligent Systems Lab, ETH Zurich).
  - **Publication:** *Towards Robust Monocular Depth Estimation: Mixing Datasets for Zero-Shot Cross-Dataset Transfer*, IEEE TPAMI 2022.
  - **Repository:** [https://github.com/isl-org/MiDaS](https://github.com/isl-org/MiDaS)
  - **Usage:** Powers on-device 3D monocular depth map extraction and color-mapped parallax visualization (`midas_small.tflite`).

### Core Multimedia & System Libraries

- **TensorFlow Lite (TFLite)**
  - **Maintainer:** Google LLC / TensorFlow Team
  - **Repository:** [https://github.com/tensorflow/tensorflow](https://github.com/tensorflow/tensorflow)
  - **License:** Apache License 2.0
  - **Usage:** Mobile on-device neural network execution runtime.

- **FFmpeg & FFmpegKit**
  - **Maintainers:** The FFmpeg Project & Taner Sener
  - **Repository:** [https://github.com/arthenica/ffmpeg-kit](https://github.com/arthenica/ffmpeg-kit)
  - **License:** LGPL 3.0 / GPL 3.0
  - **Usage:** Video container parsing, stream-copy cut extraction, audio demuxing, and concat demuxer video merging.

- **AndroidX Media3 & ExoPlayer**
  - **Maintainer:** Google LLC / Android Jetpack Team
  - **Repository:** [https://github.com/androidx/media](https://github.com/androidx/media)
  - **License:** Apache License 2.0
  - **Usage:** High-performance, low-latency video playback, clipping window preview, and frame stepping.

- **Jetpack Compose & Material Design 3**
  - **Maintainer:** Google LLC / Android Open Source Project (AOSP)
  - **License:** Apache License 2.0
  - **Usage:** Declarative UI toolkit, responsive layout grid, and Material 3 design system.

- **Room Persistence Library & Android Jetpack**
  - **Maintainer:** Google LLC
  - **License:** Apache License 2.0
  - **Usage:** Robust SQLite database abstraction, asynchronous Flow observation, and WorkManager background tasks.

- **Coil (Coroutine Image Loader)**
  - **Maintainer:** Colin White and Coil Contributors
  - **Repository:** [https://github.com/coil-kt/coil](https://github.com/coil-kt/coil)
  - **License:** Apache License 2.0
  - **Usage:** Asynchronous Kotlin coroutine-backed image and poster thumbnail loading.

- **Kotlin & KotlinX Coroutines**
  - **Maintainer:** JetBrains s.r.o.
  - **Repository:** [https://github.com/Kotlin/kotlinx.coroutines](https://github.com/Kotlin/kotlinx.coroutines)
  - **License:** Apache License 2.0
  - **Usage:** Reactive asynchronous pipelines, flow operators, and concurrency primitives.

- **CMX 3600 Specification**
  - **Standard:** SMPTE / CMX Systems
  - **Usage:** Industry-standard Edit Decision List (EDL) format enabling direct timeline interchange with DaVinci Resolve, Adobe Premiere Pro, and Final Cut Pro.

---

## License

This project is licensed under the Apache License 2.0. See the `LICENSE` file for details.
