package com.amverge.android.frontend.state

import com.amverge.android.backend.domain.model.EpisodeProject
import com.amverge.android.backend.domain.model.Scenepack
import com.amverge.android.backend.domain.model.Series

data class SeriesLibraryUiState(
    val seriesList: List<Series> = emptyList(),
    val scenepacks: List<Scenepack> = emptyList(),
    val allProjects: List<EpisodeProject> = emptyList(),
    val isExportingPack: Boolean = false,
    val exportingPackId: String? = null,
    val exportProgress: Float = 0f,
    val exportMessage: String? = null,
    val exportedZipLocation: String? = null,
    val errorMessage: String? = null
)
