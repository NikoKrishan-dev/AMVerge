package com.amverge.android.frontend.state

import com.amverge.android.backend.domain.model.EpisodeProject
import com.amverge.android.backend.domain.model.Scene

data class PreviewUiState(
    val project: EpisodeProject? = null,
    val scene: Scene? = null,
    val allScenes: List<Scene> = emptyList(),
    val isPlaying: Boolean = false,
    val playbackSpeed: Float = 1.0f,
    val isLooping: Boolean = true,
    val currentPositionMs: Long = 0L,
    val errorMessage: String? = null
)

data class ExportUiState(
    val isExporting: Boolean = false,
    val progress: Float = 0f,
    val message: String? = null,
    val isComplete: Boolean = false,
    val exportedFiles: List<String> = emptyList(),
    val errorMessage: String? = null
)
