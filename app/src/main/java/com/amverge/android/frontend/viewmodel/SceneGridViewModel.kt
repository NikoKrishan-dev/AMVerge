package com.amverge.android.frontend.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amverge.android.backend.domain.model.DetectionPreferences
import com.amverge.android.backend.domain.model.ThumbnailStatus
import com.amverge.android.backend.domain.repository.DetectionPreferencesRepository
import com.amverge.android.backend.domain.repository.ProjectRepository
import com.amverge.android.backend.domain.repository.SceneRepository
import com.amverge.android.backend.domain.repository.ScenepackRepository
import com.amverge.android.backend.media.thumbnail.SceneThumbnailGenerator
import com.amverge.android.core.logging.Logger
import com.amverge.android.frontend.state.SceneFilter
import com.amverge.android.frontend.state.SceneGridUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SceneGridViewModel(
    private val projectId: String,
    private val projectRepository: ProjectRepository,
    private val sceneRepository: SceneRepository,
    private val thumbnailGenerator: SceneThumbnailGenerator,
    private val scenepackRepository: ScenepackRepository? = null,
    private val detectionPreferencesRepository: DetectionPreferencesRepository? = null
) : ViewModel() {

    companion object {
        private const val TAG = "SceneGridViewModel"
    }

    private val _uiState = MutableStateFlow(SceneGridUiState())
    val uiState: StateFlow<SceneGridUiState> = _uiState.asStateFlow()

    private var hasTriggeredThumbRecovery = false

    init {
        viewModelScope.launch {
            projectRepository.observeProject(projectId).collectLatest { project ->
                _uiState.update { it.copy(project = project) }
            }
        }
        viewModelScope.launch {
            sceneRepository.observeScenes(projectId)
                .conflate()
                .collectLatest { scenes ->
                    _uiState.update { it.copy(scenes = scenes) }

                    // Auto-recover missing or failed thumbnails once (without blocking main-thread disk I/O)
                    if (!hasTriggeredThumbRecovery && scenes.isNotEmpty()) {
                        val missing = scenes.any { scene ->
                            scene.thumbnailStatus != ThumbnailStatus.READY ||
                            scene.thumbnailPath == null
                        }
                        if (missing) {
                            hasTriggeredThumbRecovery = true
                            generateMissingThumbnails()
                        }
                    }
                }
        }
        scenepackRepository?.let { repo ->
            viewModelScope.launch {
                repo.observeAllScenepacks().collectLatest { packs ->
                    _uiState.update { it.copy(availableScenepacks = packs) }
                }
            }
        }
        detectionPreferencesRepository?.let { repo ->
            viewModelScope.launch {
                repo.observePreferences().collectLatest { prefs ->
                    _uiState.update { it.copy(detectionPreferences = prefs) }
                }
            }
        }
    }

    fun openSettingsDialog() {
        _uiState.update { it.copy(isSettingsDialogOpen = true) }
    }

    fun closeSettingsDialog() {
        _uiState.update { it.copy(isSettingsDialogOpen = false) }
    }

    fun saveDetectionPreferences(prefs: DetectionPreferences) {
        viewModelScope.launch {
            detectionPreferencesRepository?.updatePreferences(prefs)
            _uiState.update { it.copy(detectionPreferences = prefs) }
        }
    }

    fun toggleFavorite(sceneId: String) {
        viewModelScope.launch {
            val scene = _uiState.value.scenes.find { it.id == sceneId } ?: return@launch
            val newFav = !scene.isFavorite
            sceneRepository.toggleFavorite(sceneId, newFav)
        }
    }

    fun setFilter(filter: SceneFilter) {
        _uiState.update { it.copy(filter = filter) }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun toggleSearchVisibility() {
        _uiState.update {
            val next = !it.isSearchVisible
            it.copy(isSearchVisible = next, searchQuery = if (!next) "" else it.searchQuery)
        }
    }

    fun setSortOrder(order: com.amverge.android.frontend.state.SceneSortOrder) {
        _uiState.update { it.copy(sortOrder = order) }
    }

    fun mergeSelectedScenes() {
        viewModelScope.launch {
            val selectedIds = _uiState.value.selectedSceneIds.toList()
            if (selectedIds.size < 2) return@launch

            val result = sceneRepository.mergeScenes(projectId, selectedIds)
            if (result is com.amverge.android.core.result.OperationResult.Success) {
                clearSelection()
            } else if (result is com.amverge.android.core.result.OperationResult.Failure) {
                _uiState.update { it.copy(errorMessage = result.error.message) }
            }
        }
    }

    fun deleteSelectedScenes() {
        viewModelScope.launch {
            val selectedIds = _uiState.value.selectedSceneIds.toList()
            if (selectedIds.isEmpty()) return@launch

            val result = sceneRepository.deleteScenes(projectId, selectedIds)
            if (result is com.amverge.android.core.result.OperationResult.Success) {
                clearSelection()
            } else if (result is com.amverge.android.core.result.OperationResult.Failure) {
                _uiState.update { it.copy(errorMessage = result.error.message) }
            }
        }
    }

    fun toggleFavoriteSelected() {
        viewModelScope.launch {
            val selectedIds = _uiState.value.selectedSceneIds
            if (selectedIds.isEmpty()) return@launch

            val scenes = _uiState.value.scenes.filter { it.id in selectedIds }
            val allFav = scenes.all { it.isFavorite }
            val newTarget = !allFav

            scenes.forEach { scene ->
                sceneRepository.toggleFavorite(scene.id, newTarget)
            }
        }
    }

    fun invertSelection() {
        _uiState.update { current ->
            val allIds = current.scenes.map { it.id }.toSet()
            val inverted = allIds - current.selectedSceneIds
            current.copy(selectedSceneIds = inverted)
        }
    }

    fun openScenepackDialog() {
        _uiState.update { it.copy(isScenepackDialogOpen = true) }
    }

    fun closeScenepackDialog() {
        _uiState.update { it.copy(isScenepackDialogOpen = false) }
    }

    fun createScenepack(name: String, description: String? = null, onCreated: () -> Unit = {}) {
        viewModelScope.launch {
            val selectedIds = _uiState.value.selectedSceneIds.toList()
            scenepackRepository?.createScenepack(name, description, selectedIds)
            closeScenepackDialog()
            clearSelection()
            onCreated()
        }
    }

    fun addSelectedToScenepack(scenepackId: String, onAdded: () -> Unit = {}) {
        viewModelScope.launch {
            val selectedIds = _uiState.value.selectedSceneIds.toList()
            scenepackRepository?.addScenesToScenepack(scenepackId, selectedIds)
            closeScenepackDialog()
            clearSelection()
            onAdded()
        }
    }

    fun generateMissingThumbnails() {
        viewModelScope.launch {
            val project = projectRepository.getProject(projectId) ?: return@launch
            Logger.i(TAG, "Triggering thumbnail regeneration for project $projectId")
            thumbnailGenerator.generateThumbnailsForProject(
                projectId = projectId,
                sourceUri = project.sourceUri
            )
        }
    }

    fun toggleSceneSelection(sceneId: String) {
        _uiState.update { current ->
            val updated = current.selectedSceneIds.toMutableSet()
            if (updated.contains(sceneId)) {
                updated.remove(sceneId)
            } else {
                updated.add(sceneId)
            }
            current.copy(selectedSceneIds = updated)
        }
    }

    fun selectAll() {
        _uiState.update { current ->
            current.copy(selectedSceneIds = current.scenes.map { it.id }.toSet())
        }
    }

    fun clearSelection() {
        _uiState.update { it.copy(selectedSceneIds = emptySet()) }
    }

    fun openExportDialog() {
        _uiState.update { it.copy(isExportDialogOpen = true) }
    }

    fun closeExportDialog() {
        _uiState.update { it.copy(isExportDialogOpen = false) }
    }
}
