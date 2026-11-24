package com.amverge.android.frontend.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amverge.android.backend.domain.model.ExportRequest
import com.amverge.android.backend.domain.usecase.ExportScenesUseCase
import com.amverge.android.core.result.OperationResult
import com.amverge.android.frontend.state.ExportUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ExportViewModel(
    private val exportScenesUseCase: ExportScenesUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExportUiState())
    val uiState: StateFlow<ExportUiState> = _uiState.asStateFlow()

    fun startExport(request: ExportRequest) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isExporting = true,
                    progress = 0f,
                    message = "Starting export...",
                    errorMessage = null,
                    isComplete = false
                )
            }

            val result = exportScenesUseCase(
                request = request,
                onProgress = { stage, progress, message ->
                    _uiState.update {
                        it.copy(
                            progress = progress,
                            message = message ?: stage.name
                        )
                    }
                }
            )

            when (result) {
                is OperationResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isExporting = false,
                            isComplete = true,
                            progress = 1f,
                            message = "Export complete",
                            exportedFiles = result.data
                        )
                    }
                }
                is OperationResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            isExporting = false,
                            errorMessage = result.error.message
                        )
                    }
                }
            }
        }
    }

    fun reset() {
        _uiState.update { ExportUiState() }
    }
}
