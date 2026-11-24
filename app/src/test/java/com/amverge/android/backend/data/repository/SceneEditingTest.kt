package com.amverge.android.backend.data.repository

import com.amverge.android.backend.domain.model.Scene
import com.amverge.android.backend.domain.model.ThumbnailStatus
import com.amverge.android.frontend.state.SceneFilter
import com.amverge.android.frontend.state.SceneGridUiState
import com.amverge.android.frontend.state.SceneSortOrder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class SceneEditingTest {

    private fun createScene(
        index: Int,
        startMs: Long,
        endMs: Long,
        isFavorite: Boolean = false,
        tags: List<String> = emptyList(),
        isDeadframe: Boolean = false,
        motionScore: Float = 0.5f
    ): Scene {
        return Scene(
            id = UUID.randomUUID().toString(),
            projectId = "test-project",
            index = index,
            startMs = startMs,
            endMs = endMs,
            thumbnailPath = null,
            thumbnailStatus = ThumbnailStatus.PENDING,
            isFavorite = isFavorite,
            tags = tags,
            motionScore = motionScore,
            isDeadframe = isDeadframe
        )
    }

    @Test
    fun testContiguousMergeDetection() {
        val s1 = createScene(0, 0L, 1000L)
        val s2 = createScene(1, 1000L, 2500L)
        val s3 = createScene(2, 3000L, 4000L) // Gap: 2500 to 3000

        val stateContiguous = SceneGridUiState(
            scenes = listOf(s1, s2, s3),
            selectedSceneIds = setOf(s1.id, s2.id)
        )
        assertTrue(stateContiguous.canMergeSelected)

        val stateNonContiguous = SceneGridUiState(
            scenes = listOf(s1, s2, s3),
            selectedSceneIds = setOf(s1.id, s3.id)
        )
        assertFalse(stateNonContiguous.canMergeSelected)
    }

    @Test
    fun testDurationFilters() {
        val shortScene = createScene(0, 0L, 1000L) // 1.0s -> SHORT
        val medScene = createScene(1, 1000L, 3500L) // 2.5s -> MEDIUM
        val longScene = createScene(2, 3500L, 9000L) // 5.5s -> LONG

        val scenes = listOf(shortScene, medScene, longScene)

        val shortState = SceneGridUiState(scenes = scenes, filter = SceneFilter.SHORT)
        assertEquals(1, shortState.filteredScenes.size)
        assertEquals(shortScene.id, shortState.filteredScenes[0].id)

        val medState = SceneGridUiState(scenes = scenes, filter = SceneFilter.MEDIUM)
        assertEquals(1, medState.filteredScenes.size)
        assertEquals(medScene.id, medState.filteredScenes[0].id)

        val longState = SceneGridUiState(scenes = scenes, filter = SceneFilter.LONG)
        assertEquals(1, longState.filteredScenes.size)
        assertEquals(longScene.id, longState.filteredScenes[0].id)
    }

    @Test
    fun testSearchAndSort() {
        val s1 = createScene(0, 0L, 2000L, tags = listOf("action", "fight"), motionScore = 0.8f)
        val s2 = createScene(1, 2000L, 3000L, tags = listOf("dialogue"), motionScore = 0.2f)
        val s3 = createScene(2, 3000L, 7000L, tags = listOf("scenery"), motionScore = 0.1f)

        val scenes = listOf(s1, s2, s3)

        // Search by scene index "#2" -> s2 (1-based index 2)
        val searchIndexState = SceneGridUiState(scenes = scenes, searchQuery = "#2")
        assertEquals(1, searchIndexState.filteredScenes.size)
        assertEquals(s2.id, searchIndexState.filteredScenes[0].id)

        // Search by tag "action"
        val searchTagState = SceneGridUiState(scenes = scenes, searchQuery = "action")
        assertEquals(1, searchTagState.filteredScenes.size)
        assertEquals(s1.id, searchTagState.filteredScenes[0].id)

        // Sort by Shortest First: s2 (1000ms), s1 (2000ms), s3 (4000ms)
        val sortShortestState = SceneGridUiState(scenes = scenes, sortOrder = SceneSortOrder.SHORTEST_FIRST)
        assertEquals(s2.id, sortShortestState.filteredScenes[0].id)
        assertEquals(s1.id, sortShortestState.filteredScenes[1].id)
        assertEquals(s3.id, sortShortestState.filteredScenes[2].id)

        // Sort by Most Dynamic: s1 (0.8), s2 (0.2), s3 (0.1)
        val sortDynamicState = SceneGridUiState(scenes = scenes, sortOrder = SceneSortOrder.MOST_DYNAMIC)
        assertEquals(s1.id, sortDynamicState.filteredScenes[0].id)
        assertEquals(s2.id, sortDynamicState.filteredScenes[1].id)
        assertEquals(s3.id, sortDynamicState.filteredScenes[2].id)
    }
}
