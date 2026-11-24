package com.amverge.android.backend.domain.model.ai

import java.io.File

enum class AiToolType {
    INTERPOLATION,
    UPSCALE,
    DEPTH_MAP
}

enum class InterpolationModel(val displayName: String) {
    RIFE_V4_LITE("RIFE v4.6 Lite"),
    RIFE_ANIME("RIFE Anime")
}

data class InterpolationConfig(
    val multiplier: Int = 2, // 2x, 4x, 8x, 16x
    val model: InterpolationModel = InterpolationModel.RIFE_V4_LITE,
    val maxResolution: Int = 720
) {
    init {
        require(multiplier in listOf(2, 4, 8, 16)) { "Multiplier must be 2, 4, 8, or 16" }
    }
}

enum class UpscaleModel(val displayName: String, val nativeScale: Int) {
    FSR_SPATIAL("FSR Spatial (Fast)", 2),
    REAL_ESRGAN_ANIME_2X("Real-ESRGAN Anime 2x", 2),
    REAL_ESRGAN_ANIME_4X("Real-ESRGAN Anime 4x", 4)
}

enum class UpscaleQuality {
    FAST,
    BALANCED,
    HIGH
}

data class UpscaleConfig(
    val scale: Int = 2, // 2x or 4x
    val model: UpscaleModel = UpscaleModel.REAL_ESRGAN_ANIME_2X,
    val quality: UpscaleQuality = UpscaleQuality.HIGH
) {
    init {
        require(scale == 2 || scale == 4) { "Scale must be 2 or 4" }
    }
}

enum class DepthModel(val displayName: String) {
    DEPTH_ANYTHING_V2_SMALL("Depth Anything V2 - Small (Sharp)"),
    MIDAS_SMALL("MiDaS v2.1 - Small (Fast)")
}

enum class DepthColormap(val displayName: String) {
    INFERNO("Inferno"),
    VIRIDIS("Viridis"),
    TURBO("Turbo"),
    PLASMA("Plasma"),
    MAGMA("Magma"),
    GRAYSCALE("Grayscale")
}

data class DepthConfig(
    val model: DepthModel = DepthModel.DEPTH_ANYTHING_V2_SMALL,
    val colormap: DepthColormap = DepthColormap.INFERNO,
    val grayscale: Boolean = false
)

data class AiToolRequest(
    val clipName: String,
    val sourcePathOrUri: String,
    val toolType: AiToolType,
    val interpolationConfig: InterpolationConfig = InterpolationConfig(),
    val upscaleConfig: UpscaleConfig = UpscaleConfig(),
    val depthConfig: DepthConfig = DepthConfig(),
    val targetDirectory: File? = null
)

data class AiToolResult(
    val toolType: AiToolType,
    val originalVideo: File,
    val processedVideo: File,
    val durationMs: Long,
    val width: Int,
    val height: Int,
    val fps: Float,
    val frameCount: Int
)
