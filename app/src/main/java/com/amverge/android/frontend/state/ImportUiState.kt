package com.amverge.android.frontend.state

import com.amverge.android.backend.domain.model.DetectionPreferences
import com.amverge.android.backend.domain.model.EpisodeProject

data class ImportUiState(
    val isLoading: Boolean = false,
    val progress: Float = 0f,
    val statusMessage: String? = null,
    val latestProject: EpisodeProject? = null,
    val isSettingsDialogOpen: Boolean = false,
    val detectionPreferences: DetectionPreferences = DetectionPreferences(),
    val errorMessage: String? = null
)
