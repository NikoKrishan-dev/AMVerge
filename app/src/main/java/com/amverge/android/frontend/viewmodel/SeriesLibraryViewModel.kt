package com.amverge.android.frontend.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amverge.android.backend.domain.repository.ProjectRepository
import com.amverge.android.backend.domain.repository.ScenepackRepository
import com.amverge.android.backend.domain.repository.SeriesRepository
import com.amverge.android.backend.domain.usecase.ExportScenepackUseCase
import com.amverge.android.core.logging.Logger
import com.amverge.android.core.result.OperationResult
import com.amverge.android.frontend.state.SeriesLibraryUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SeriesLibraryViewModel(
    private val seriesRepository: SeriesRepository,
    private val scenepackRepository: ScenepackRepository,
    private val projectRepository: ProjectRepository,
    private val exportScenepackUseCase: ExportScenepackUseCase
) : ViewModel() {

    companion object {
        private const val TAG = "SeriesLibraryViewModel"
    }

    private val _uiState = MutableStateFlow(SeriesLibraryUiState())
    val uiState: StateFlow<SeriesLibraryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            seriesRepository.observeAllSeries().collectLatest { series ->
                _uiState.update { it.copy(seriesList = series) }
            }
        }

        viewModelScope.launch {
            scenepackRepository.observeAllScenepacks().collectLatest { packs ->
                _uiState.update { it.copy(scenepacks = packs) }
            }
        }

        viewModelScope.launch {
            projectRepository.observeAllProjects().collectLatest { projects ->
                _uiState.update { it.copy(allProjects = projects) }
            }
        }
    }

    fun createSeries(title: String, description: String? = null) {
        viewModelScope.launch {
            if (title.isBlank()) return@launch
            seriesRepository.createSeries(title.trim(), description?.trim()?.ifEmpty { null })
        }
    }

    fun assignEpisodeToSeries(projectId: String, seriesId: String?, episodeNumber: Int? = null) {
        viewModelScope.launch {
            projectRepository.assignSeries(projectId, seriesId, episodeNumber)
        }
    }

    fun deleteSeries(seriesId: String) {
        viewModelScope.launch {
            seriesRepository.deleteSeries(seriesId)
        }
    }

    fun deleteScenepack(scenepackId: String) {
        viewModelScope.launch {
            scenepackRepository.deleteScenepack(scenepackId)
        }
    }

    fun exportScenepackToZip(scenepackId: String, destinationTreeUri: String? = null) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isExportingPack = true,
                    exportingPackId = scenepackId,
                    exportProgress = 0.05f,
                    exportMessage = "Starting scenepack export...",
                    exportedZipLocation = null,
                    errorMessage = null
                )
            }

            val result = exportScenepackUseCase.exportScenepackToZip(
                scenepackId = scenepackId,
                destinationTreeUri = destinationTreeUri,
                onProgress = { progress, msg ->
                    _uiState.update {
                        it.copy(exportProgress = progress, exportMessage = msg)
                    }
                }
            )

            when (result) {
                is OperationResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isExportingPack = false,
                            exportingPackId = null,
                            exportProgress = 1.0f,
                            exportMessage = "Scenepack exported successfully!",
                            exportedZipLocation = result.data
                        )
                    }
                }
                is OperationResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            isExportingPack = false,
                            exportingPackId = null,
                            errorMessage = result.error.message
                        )
                    }
                }
            }
        }
    }

    fun clearStatus() {
        _uiState.update {
            it.copy(
                isExportingPack = false,
                exportingPackId = null,
                exportedZipLocation = null,
                errorMessage = null
            )
        }
    }
}
