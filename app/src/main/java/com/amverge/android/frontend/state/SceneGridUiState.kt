package com.amverge.android.frontend.state

import com.amverge.android.backend.domain.model.DetectionPreferences
import com.amverge.android.backend.domain.model.EpisodeProject
import com.amverge.android.backend.domain.model.Scene
import com.amverge.android.backend.domain.model.Scenepack

data class SceneGridUiState(
    val project: EpisodeProject? = null,
    val scenes: List<Scene> = emptyList(),
    val selectedSceneIds: Set<String> = emptySet(),
    val filter: SceneFilter = SceneFilter.ALL,
    val sortOrder: SceneSortOrder = SceneSortOrder.CHRONOLOGICAL,
    val searchQuery: String = "",
    val isExportDialogOpen: Boolean = false,
    val isScenepackDialogOpen: Boolean = false,
    val isSettingsDialogOpen: Boolean = false,
    val isSearchVisible: Boolean = false,
    val detectionPreferences: DetectionPreferences = DetectionPreferences(),
    val availableScenepacks: List<Scenepack> = emptyList(),
    val exportProgress: Float? = null,
    val exportMessage: String? = null,
    val errorMessage: String? = null
) {
    val selectedCount: Int get() = selectedSceneIds.size
    val isSelectionActive: Boolean get() = selectedSceneIds.isNotEmpty()

    val canMergeSelected: Boolean
        get() {
            if (selectedSceneIds.size < 2) return false
            val selected = scenes.filter { it.id in selectedSceneIds }.sortedBy { it.startMs }
            for (i in 0 until selected.size - 1) {
                if (selected[i].endMs != selected[i + 1].startMs) return false
            }
            return true
        }

    val filteredScenes: List<Scene>
        get() {
            var list = when (filter) {
                SceneFilter.ALL -> scenes
                SceneFilter.DYNAMIC_ONLY -> scenes.filter { !it.isDeadframe }
                SceneFilter.FAVORITES -> scenes.filter { it.isFavorite }
                SceneFilter.ACTION -> scenes.filter {
                    it.tags.any { tag -> tag.contains("action", ignoreCase = true) } || it.durationMs < 1500L
                }
                SceneFilter.SHORT -> scenes.filter { it.durationMs < 1500L }
                SceneFilter.MEDIUM -> scenes.filter { it.durationMs in 1500L..4000L }
                SceneFilter.LONG -> scenes.filter { it.durationMs > 4000L }
            }

            if (searchQuery.isNotBlank()) {
                val query = searchQuery.trim().lowercase()
                val indexQuery = query.removePrefix("#").toIntOrNull()
                list = list.filter { scene ->
                    if (indexQuery != null && (scene.index + 1) == indexQuery) {
                        true
                    } else {
                        scene.tags.any { it.lowercase().contains(query) } ||
                        (scene.index + 1).toString() == query
                    }
                }
            }

            return when (sortOrder) {
                SceneSortOrder.CHRONOLOGICAL -> list.sortedBy { it.index }
                SceneSortOrder.SHORTEST_FIRST -> list.sortedBy { it.durationMs }
                SceneSortOrder.LONGEST_FIRST -> list.sortedByDescending { it.durationMs }
                SceneSortOrder.MOST_DYNAMIC -> list.sortedByDescending { it.motionScore }
            }
        }
}
