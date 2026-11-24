package com.amverge.android.backend.domain.model

import com.amverge.android.frontend.state.SceneFilter
import com.amverge.android.frontend.state.SceneGridUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScenepackTest {

    @Test
    fun `scene filtering separates favorites and action cuts properly`() {
        val scenes = listOf(
            Scene(id = "s1", projectId = "p", index = 0, startMs = 0L, endMs = 2000L, isFavorite = true),
            Scene(id = "s2", projectId = "p", index = 1, startMs = 2000L, endMs = 3000L, isFavorite = false, tags = listOf("action")),
            Scene(id = "s3", projectId = "p", index = 2, startMs = 3000L, endMs = 7000L, isFavorite = false),
            Scene(id = "s4", projectId = "p", index = 3, startMs = 7000L, endMs = 8000L, isFavorite = true, tags = listOf("action", "fight"))
        )

        val stateAll = SceneGridUiState(scenes = scenes, filter = SceneFilter.ALL)
        assertEquals(4, stateAll.filteredScenes.size)

        val stateFavs = SceneGridUiState(scenes = scenes, filter = SceneFilter.FAVORITES)
        assertEquals(2, stateFavs.filteredScenes.size)
        assertTrue(stateFavs.filteredScenes.all { it.isFavorite })

        val stateAction = SceneGridUiState(scenes = scenes, filter = SceneFilter.ACTION)
        // s2 (action tag), s4 (action tag), and short scenes (< 1500ms) like s2 (1000ms) and s4 (1000ms)
        assertTrue(stateAction.filteredScenes.any { it.id == "s2" })
        assertTrue(stateAction.filteredScenes.any { it.id == "s4" })
    }

    @Test
    fun `scenepack metadata and properties are initialized correctly`() {
        val pack = Scenepack(
            id = "pack-1",
            name = "Gojo Best Cuts",
            description = "High motion scenes from Shibuya Incident",
            sceneCount = 8
        )

        assertEquals("pack-1", pack.id)
        assertEquals("Gojo Best Cuts", pack.name)
        assertEquals(8, pack.sceneCount)
    }

    @Test
    fun `series metadata and properties are initialized correctly`() {
        val series = Series(
            id = "series-1",
            title = "Jujutsu Kaisen",
            description = "Season 2 Shibuya arc",
            episodeCount = 12
        )

        assertEquals("series-1", series.id)
        assertEquals("Jujutsu Kaisen", series.title)
        assertEquals(12, series.episodeCount)
    }
}
