package com.amverge.android.frontend.components

import com.amverge.android.backend.domain.model.Scene
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class TimelineOverviewTest {

    @Test
    fun `timeline proportional slice math correctly maps duration fractions`() {
        val scenes = listOf(
            Scene(id = "s1", projectId = "p", index = 0, startMs = 0L, endMs = 2000L),
            Scene(id = "s2", projectId = "p", index = 1, startMs = 2000L, endMs = 5000L),
            Scene(id = "s3", projectId = "p", index = 2, startMs = 5000L, endMs = 10000L)
        )
        val totalDurationMs = 10000L

        // s1: 2000 / 10000 = 0.20 (20%)
        // s2: 3000 / 10000 = 0.30 (30%)
        // s3: 5000 / 10000 = 0.50 (50%)
        val f1 = scenes[0].durationMs.toFloat() / totalDurationMs
        val f2 = scenes[1].durationMs.toFloat() / totalDurationMs
        val f3 = scenes[2].durationMs.toFloat() / totalDurationMs

        assertEquals(0.20f, f1, 0.001f)
        assertEquals(0.30f, f2, 0.001f)
        assertEquals(0.50f, f3, 0.001f)
        assertEquals(1.0f, f1 + f2 + f3, 0.001f)
    }

    @Test
    fun `scene lookup by timestamp locates correct scene on timeline scrub`() {
        val scenes = listOf(
            Scene(id = "s1", projectId = "p", index = 0, startMs = 0L, endMs = 2000L),
            Scene(id = "s2", projectId = "p", index = 1, startMs = 2000L, endMs = 5000L),
            Scene(id = "s3", projectId = "p", index = 2, startMs = 5000L, endMs = 10000L)
        )

        // Lookup at 1500ms -> s1
        val match1 = scenes.find { 1500L in it.startMs..it.endMs }
        assertNotNull(match1)
        assertEquals("s1", match1?.id)

        // Lookup at 3200ms -> s2
        val match2 = scenes.find { 3200L in it.startMs..it.endMs }
        assertNotNull(match2)
        assertEquals("s2", match2?.id)

        // Lookup at 8000ms -> s3
        val match3 = scenes.find { 8000L in it.startMs..it.endMs }
        assertNotNull(match3)
        assertEquals("s3", match3?.id)
    }
}
