package com.amverge.android.frontend.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amverge.android.backend.domain.model.Scene
import com.amverge.android.backend.domain.repository.ProjectRepository
import com.amverge.android.backend.domain.repository.SceneRepository
import com.amverge.android.backend.media.playback.ScenePlayerController
import com.amverge.android.core.result.OperationResult
import com.amverge.android.frontend.state.PreviewUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PreviewViewModel(
    private val projectId: String,
    private val sceneId: String,
    private val projectRepository: ProjectRepository,
    private val sceneRepository: SceneRepository,
    val playerController: ScenePlayerController
) : ViewModel() {

    private var currentSceneId: String = sceneId
    private val _uiState = MutableStateFlow(PreviewUiState())
    val uiState: StateFlow<PreviewUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val project = projectRepository.getProject(projectId)
            val scenes = sceneRepository.getScenes(projectId).sortedBy { it.index }
            val scene = scenes.find { it.id == currentSceneId } ?: sceneRepository.getScene(currentSceneId)

            _uiState.update {
                it.copy(
                    project = project,
                    scene = scene,
                    allScenes = scenes
                )
            }

            if (project != null && scene != null) {
                playerController.setLooping(true)
                playerController.playScene(
                    sourceUri = project.sourceUri,
                    startMs = scene.startMs,
                    endMs = scene.endMs
                )
            }
        }

        viewModelScope.launch {
            playerController.isPlaying.collectLatest { isPlaying ->
                _uiState.update { it.copy(isPlaying = isPlaying) }
            }
        }

        viewModelScope.launch {
            playerController.playbackError.collectLatest { error ->
                if (error != null) {
                    _uiState.update { it.copy(errorMessage = error) }
                }
            }
        }
    }

    fun playScene(scene: Scene) {
        val project = _uiState.value.project ?: return
        currentSceneId = scene.id
        _uiState.update { it.copy(scene = scene) }
        playerController.playScene(
            sourceUri = project.sourceUri,
            startMs = scene.startMs,
            endMs = scene.endMs
        )
    }

    fun playNextScene() {
        val current = _uiState.value.scene ?: return
        val all = _uiState.value.allScenes
        val currentIndex = all.indexOfFirst { it.id == current.id }
        if (currentIndex in 0 until (all.size - 1)) {
            playScene(all[currentIndex + 1])
        }
    }

    fun playPreviousScene() {
        val current = _uiState.value.scene ?: return
        val all = _uiState.value.allScenes
        val currentIndex = all.indexOfFirst { it.id == current.id }
        if (currentIndex > 0) {
            playScene(all[currentIndex - 1])
        }
    }

    fun togglePlayPause() {
        if (_uiState.value.isPlaying) {
            playerController.pause()
        } else {
            playerController.play()
        }
    }

    fun seekTo(positionMs: Long) {
        playerController.seekTo(positionMs)
    }

    fun stepForward() {
        playerController.stepFrameForward()
    }

    fun stepBackward() {
        playerController.stepFrameBackward()
    }

    fun toggleSpeed() {
        val nextSpeed = when (_uiState.value.playbackSpeed) {
            0.5f -> 1.0f
            1.0f -> 1.5f
            1.5f -> 2.0f
            else -> 0.5f
        }
        playerController.setPlaybackSpeed(nextSpeed)
        _uiState.update { it.copy(playbackSpeed = nextSpeed) }
    }

    fun toggleLooping() {
        val nextLoop = !_uiState.value.isLooping
        playerController.setLooping(nextLoop)
        _uiState.update { it.copy(isLooping = nextLoop) }
    }

    fun toggleFavorite() {
        viewModelScope.launch {
            val scene = _uiState.value.scene ?: return@launch
            val nextFav = !scene.isFavorite
            sceneRepository.toggleFavorite(scene.id, nextFav)
            val updated = scene.copy(isFavorite = nextFav)
            val allUpdated = _uiState.value.allScenes.map { if (it.id == scene.id) updated else it }
            _uiState.update { it.copy(scene = updated, allScenes = allUpdated) }
        }
    }

    fun splitCurrentScene(onSuccess: (Scene) -> Unit = {}) {
        viewModelScope.launch {
            val scene = _uiState.value.scene ?: return@launch
            val currentPosMs = playerController.player.currentPosition
            val splitAtMs = scene.startMs + currentPosMs

            if (splitAtMs <= scene.startMs + 100L || splitAtMs >= scene.endMs - 100L) {
                _uiState.update {
                    it.copy(errorMessage = "Position too close to cut boundaries to split")
                }
                return@launch
            }

            val result = sceneRepository.splitScene(scene.id, splitAtMs)
            if (result is OperationResult.Success) {
                val updatedScenes = sceneRepository.getScenes(projectId).sortedBy { it.index }
                val newActive = result.data.first
                _uiState.update {
                    it.copy(scene = newActive, allScenes = updatedScenes, errorMessage = null)
                }
                playScene(newActive)
                onSuccess(newActive)
            } else if (result is OperationResult.Failure) {
                _uiState.update { it.copy(errorMessage = result.error.message) }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        playerController.release()
    }
}
