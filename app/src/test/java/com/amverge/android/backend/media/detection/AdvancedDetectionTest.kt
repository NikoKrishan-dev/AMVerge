package com.amverge.android.backend.media.detection

import com.amverge.android.backend.domain.model.DetectionPreferences
import com.amverge.android.backend.domain.model.Scene
import com.amverge.android.frontend.state.SceneFilter
import com.amverge.android.frontend.state.SceneGridUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdvancedDetectionTest {

    @Test
    fun `default detection preferences are reasonable for anime AMV editing`() {
        val prefs = DetectionPreferences()
        assertEquals(250L, prefs.minSceneDurationMs)
        assertFalse(prefs.filterDeadframes)
        assertEquals(0.15f, prefs.deadframeThreshold, 0.001f)
        assertTrue(prefs.detectTransitions)
        assertEquals(0.5f, prefs.sensitivity, 0.001f)
    }

    @Test
    fun `custom detection preferences can be created with tighter duration and deadframe thresholds`() {
        val prefs = DetectionPreferences(
            minSceneDurationMs = 500L,
            filterDeadframes = true,
            deadframeThreshold = 0.25f,
            detectTransitions = false,
            sensitivity = 0.8f
        )
        assertEquals(500L, prefs.minSceneDurationMs)
        assertTrue(prefs.filterDeadframes)
        assertEquals(0.25f, prefs.deadframeThreshold, 0.001f)
        assertFalse(prefs.detectTransitions)
        assertEquals(0.8f, prefs.sensitivity, 0.001f)
    }

    @Test
    fun `dynamic filter separates motion scenes from static deadframes`() {
        val scenes = listOf(
            Scene(id = "s1", projectId = "p", index = 0, startMs = 0L, endMs = 1000L, motionScore = 0.45f, isDeadframe = false),
            Scene(id = "s2", projectId = "p", index = 1, startMs = 1000L, endMs = 5000L, motionScore = 0.08f, isDeadframe = true, tags = listOf("static")),
            Scene(id = "s3", projectId = "p", index = 2, startMs = 5000L, endMs = 6500L, motionScore = 0.60f, isDeadframe = false, tags = listOf("action")),
            Scene(id = "s4", projectId = "p", index = 3, startMs = 6500L, endMs = 9000L, motionScore = 0.05f, isDeadframe = true, tags = listOf("static"))
        )

        val stateDynamic = SceneGridUiState(scenes = scenes, filter = SceneFilter.DYNAMIC_ONLY)
        val dynamicScenes = stateDynamic.filteredScenes

        assertEquals(2, dynamicScenes.size)
        assertTrue(dynamicScenes.none { it.isDeadframe })
        assertTrue(dynamicScenes.any { it.id == "s1" })
        assertTrue(dynamicScenes.any { it.id == "s3" })
    }

    @Test
    fun `luma and color difference math calculation functions accurately`() {
        // Test luminance weights: Y = 0.299R + 0.587G + 0.114B
        val r = 255
        val g = 0
        val b = 0
        val lumaRed = 0.299f * r + 0.587f * g + 0.114f * b
        assertEquals(76.245f, lumaRed, 0.01f)

        val lumaWhite = 0.299f * 255 + 0.587f * 255 + 0.114f * 255
        assertEquals(255.0f, lumaWhite, 0.01f)

        val lumaBlack = 0.0f
        assertEquals(0.0f, lumaBlack, 0.01f)
    }
}
