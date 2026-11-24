package com.amverge.android.frontend.state

import android.net.Uri
import com.amverge.android.backend.domain.model.ai.AiToolResult
import com.amverge.android.backend.domain.model.ai.AiToolType
import com.amverge.android.backend.domain.model.ai.DepthColormap
import com.amverge.android.backend.domain.model.ai.DepthConfig
import com.amverge.android.backend.domain.model.ai.DepthModel
import com.amverge.android.backend.domain.model.ai.InterpolationConfig
import com.amverge.android.backend.domain.model.ai.InterpolationModel
import com.amverge.android.backend.domain.model.ai.UpscaleConfig
import com.amverge.android.backend.domain.model.ai.UpscaleModel
import com.amverge.android.backend.domain.model.ai.UpscaleQuality

data class AiToolsUiState(
    val selectedClipUri: Uri? = null,
    val selectedClipName: String? = null,
    val selectedClipDurationMs: Long? = null,
    val selectedClipWidth: Int? = null,
    val selectedClipHeight: Int? = null,
    val selectedClipFps: Float? = null,
    val isProbingClip: Boolean = false,
    val selectedTool: AiToolType = AiToolType.INTERPOLATION,
    val interpolationConfig: InterpolationConfig = InterpolationConfig(multiplier = 2),
    val upscaleConfig: UpscaleConfig = UpscaleConfig(scale = 2),
    val depthConfig: DepthConfig = DepthConfig(),
    val isProcessing: Boolean = false,
    val progress: Float = 0f,
    val message: String = "",
    val error: String? = null,
    val result: AiToolResult? = null,
    val isSavedToStorage: Boolean = false,
    val savedPathOrUri: String? = null
)
