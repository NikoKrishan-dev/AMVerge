package com.amverge.android.frontend.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amverge.android.backend.domain.model.Scene
import com.amverge.android.backend.domain.model.ai.AiToolRequest
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
import com.amverge.android.backend.domain.usecase.RunAiToolUseCase
import com.amverge.android.backend.media.cutting.SceneExtractor
import com.amverge.android.backend.media.ffmpeg.MediaEngine
import com.amverge.android.core.logging.Logger
import com.amverge.android.core.result.OperationResult
import com.amverge.android.frontend.state.AiToolsUiState
import com.amverge.android.platform.storage.SafUriAccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class AiToolsViewModel(
    private val context: Context,
    private val mediaEngine: MediaEngine,
    private val sceneExtractor: SceneExtractor,
    private val runAiToolUseCase: RunAiToolUseCase,
    private val safUriAccess: SafUriAccess
) : ViewModel() {

    companion object {
        private const val TAG = "AiToolsViewModel"
    }

    private val _uiState = MutableStateFlow(AiToolsUiState())
    val uiState: StateFlow<AiToolsUiState> = _uiState.asStateFlow()

    fun selectTool(tool: AiToolType) {
        _uiState.update { it.copy(selectedTool = tool, error = null, result = null, isSavedToStorage = false) }
    }

    fun setInterpolationMultiplier(multiplier: Int) {
        _uiState.update {
            it.copy(
                interpolationConfig = it.interpolationConfig.copy(multiplier = multiplier),
                result = null
            )
        }
    }

    fun setInterpolationModel(model: InterpolationModel) {
        _uiState.update {
            it.copy(
                interpolationConfig = it.interpolationConfig.copy(model = model),
                result = null
            )
        }
    }

    fun setUpscaleScale(scale: Int) {
        _uiState.update {
            it.copy(
                upscaleConfig = it.upscaleConfig.copy(scale = scale),
                result = null
            )
        }
    }

    fun setUpscaleModel(model: UpscaleModel) {
        _uiState.update {
            it.copy(
                upscaleConfig = it.upscaleConfig.copy(model = model),
                result = null
            )
        }
    }

    fun setUpscaleQuality(quality: UpscaleQuality) {
        _uiState.update {
            it.copy(
                upscaleConfig = it.upscaleConfig.copy(quality = quality),
                result = null
            )
        }
    }

    fun setDepthModel(model: DepthModel) {
        _uiState.update {
            it.copy(
                depthConfig = it.depthConfig.copy(model = model),
                result = null
            )
        }
    }

    fun setDepthColormap(colormap: DepthColormap) {
        _uiState.update {
            val isGray = (colormap == DepthColormap.GRAYSCALE)
            it.copy(
                depthConfig = it.depthConfig.copy(colormap = colormap, grayscale = isGray),
                result = null
            )
        }
    }

    fun onClipSelected(uri: Uri) {
        safUriAccess.takePersistableReadPermission(uri)
        val displayName = safUriAccess.getDisplayName(uri)
        _uiState.update {
            it.copy(
                selectedClipUri = uri,
                selectedClipName = displayName,
                isProbingClip = true,
                error = null,
                result = null,
                isSavedToStorage = false
            )
        }

        viewModelScope.launch {
            val probeResult = mediaEngine.probe(uri.toString())
            if (probeResult is OperationResult.Success) {
                val info = probeResult.data
                _uiState.update {
                    it.copy(
                        isProbingClip = false,
                        selectedClipDurationMs = info.durationMs,
                        selectedClipWidth = info.width,
                        selectedClipHeight = info.height,
                        selectedClipFps = info.frameRate?.toFloat()
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isProbingClip = false,
                        selectedClipDurationMs = null,
                        selectedClipWidth = null,
                        selectedClipHeight = null,
                        selectedClipFps = null
                    )
                }
            }
        }
    }

    fun clearSelectedClip() {
        _uiState.update {
            it.copy(
                selectedClipUri = null,
                selectedClipName = null,
                selectedClipDurationMs = null,
                selectedClipWidth = null,
                selectedClipHeight = null,
                selectedClipFps = null,
                error = null,
                result = null,
                isSavedToStorage = false
            )
        }
    }

    fun runAiToolOnSelectedClip() {
        val uri = _uiState.value.selectedClipUri ?: return
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isProcessing = true,
                    progress = 0.05f,
                    message = "Preparing input video clip...",
                    error = null,
                    result = null,
                    isSavedToStorage = false
                )
            }

            val stagingDir = File(context.cacheDir, "ai_staging_input_${UUID.randomUUID()}").apply { mkdirs() }
            val inputFileName = _uiState.value.selectedClipName ?: "input_clip.mp4"
            val stagedFile = File(stagingDir, inputFileName)

            val copied = withContext(Dispatchers.IO) {
                try {
                    context.contentResolver.openInputStream(uri)?.use { inputStream ->
                        FileOutputStream(stagedFile).use { outputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }
                    stagedFile.exists() && stagedFile.length() > 0L
                } catch (e: Exception) {
                    Logger.e(TAG, "Failed to copy selected clip to staging", e)
                    false
                }
            }

            if (!copied) {
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        error = "Failed to access or copy input video clip."
                    )
                }
                stagingDir.deleteRecursively()
                return@launch
            }

            val current = _uiState.value
            val request = AiToolRequest(
                clipName = stagedFile.nameWithoutExtension,
                sourcePathOrUri = stagedFile.absolutePath,
                toolType = current.selectedTool,
                interpolationConfig = current.interpolationConfig,
                upscaleConfig = current.upscaleConfig,
                depthConfig = current.depthConfig
            )

            val result = runAiToolUseCase(request) { _, progress, message ->
                _uiState.update { it.copy(progress = progress, message = message) }
            }

            when (result) {
                is OperationResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isProcessing = false,
                            progress = 1.0f,
                            message = "AI Processing complete!",
                            result = result.data
                        )
                    }
                }
                is OperationResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            isProcessing = false,
                            error = result.error.message
                        )
                    }
                }
            }

            stagingDir.deleteRecursively()
        }
    }

    fun runAiTool(sourceUri: String, scene: Scene) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isProcessing = true,
                    progress = 0.05f,
                    message = "Preparing scene clip...",
                    error = null,
                    result = null,
                    isSavedToStorage = false
                )
            }

            val stagingDir = File(context.cacheDir, "ai_clip_prep_${UUID.randomUUID()}").apply { mkdirs() }
            val cutResult = sceneExtractor.extractSceneToStaging(sourceUri, scene, stagingDir)

            if (cutResult is OperationResult.Failure) {
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        error = "Failed to extract scene clip: ${cutResult.error.message}"
                    )
                }
                stagingDir.deleteRecursively()
                return@launch
            }

            val stagedClip = (cutResult as OperationResult.Success).data

            val current = _uiState.value
            val request = AiToolRequest(
                clipName = "scene_${scene.index + 1}",
                sourcePathOrUri = stagedClip.absolutePath,
                toolType = current.selectedTool,
                interpolationConfig = current.interpolationConfig,
                upscaleConfig = current.upscaleConfig,
                depthConfig = current.depthConfig
            )

            val result = runAiToolUseCase(request) { _, progress, message ->
                _uiState.update { it.copy(progress = progress, message = message) }
            }

            when (result) {
                is OperationResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isProcessing = false,
                            progress = 1.0f,
                            message = "AI Processing complete!",
                            result = result.data
                        )
                    }
                }
                is OperationResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            isProcessing = false,
                            error = result.error.message
                        )
                    }
                }
            }

            stagingDir.deleteRecursively()
        }
    }

    fun saveResultToStorage(result: AiToolResult) {
        viewModelScope.launch {
            val fileName = result.processedVideo.name
            val savedUri = safUriAccess.saveVideoToSharedStorage(result.processedVideo, fileName)
            _uiState.update {
                it.copy(
                    isSavedToStorage = true,
                    savedPathOrUri = savedUri?.toString() ?: result.processedVideo.absolutePath
                )
            }
        }
    }

    fun reset() {
        _uiState.update { AiToolsUiState() }
    }
}
