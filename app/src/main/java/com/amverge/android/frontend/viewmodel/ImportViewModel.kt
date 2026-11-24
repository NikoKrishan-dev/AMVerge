package com.amverge.android.frontend.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amverge.android.backend.domain.model.DetectionPreferences
import com.amverge.android.backend.domain.model.EpisodeProject
import com.amverge.android.backend.domain.repository.DetectionPreferencesRepository
import com.amverge.android.backend.domain.repository.ProjectRepository
import com.amverge.android.backend.domain.usecase.ImportEpisodeUseCase
import com.amverge.android.core.logging.Logger
import com.amverge.android.core.result.OperationResult
import com.amverge.android.frontend.state.ImportUiState
import com.amverge.android.platform.storage.SafUriAccess
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ImportViewModel(
    private val importEpisodeUseCase: ImportEpisodeUseCase,
    private val projectRepository: ProjectRepository,
    private val safUriAccess: SafUriAccess,
    private val detectionPreferencesRepository: DetectionPreferencesRepository? = null
) : ViewModel() {

    companion object {
        private const val TAG = "ImportViewModel"
    }

    private val _uiState = MutableStateFlow(ImportUiState())
    val uiState: StateFlow<ImportUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            projectRepository.observeAllProjects().collectLatest { projects ->
                _uiState.update { it.copy(latestProject = projects.firstOrNull()) }
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

    fun onVideoSelected(uri: Uri, onImportSuccess: (EpisodeProject) -> Unit) {
        viewModelScope.launch {
            val flags = safUriAccess.takePersistableReadPermission(uri)
            val displayName = safUriAccess.getDisplayName(uri)

            _uiState.update {
                it.copy(
                    isLoading = true,
                    progress = 0f,
                    statusMessage = "Starting import for $displayName...",
                    errorMessage = null
                )
            }

            val result = importEpisodeUseCase(
                sourceUri = uri.toString(),
                displayName = displayName,
                persistedReadFlags = flags,
                onProgress = { stage, progress, message ->
                    _uiState.update {
                        it.copy(
                            progress = progress,
                            statusMessage = message ?: stage.name
                        )
                    }
                }
            )

            when (result) {
                is OperationResult.Success -> {
                    Logger.i(TAG, "Import succeeded: ${result.data.displayName}")
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            progress = 1f,
                            statusMessage = "Import completed",
                            latestProject = result.data
                        )
                    }
                    onImportSuccess(result.data)
                }
                is OperationResult.Failure -> {
                    Logger.e(TAG, "Import failed: ${result.error.message}")
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = result.error.message
                        )
                    }
                }
            }
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
