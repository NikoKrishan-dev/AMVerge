package com.amverge.android.backend.domain.usecase

import com.amverge.android.backend.domain.model.Scene
import org.junit.Assert.assertEquals
import org.junit.Test

class ExportOrderTest {

    @Test
    fun `sorting scenes for export preserves chronological index order regardless of tap order`() {
        // User tapped scene 5, then scene 1, then scene 3, then scene 2
        val tappedScenes = listOf(
            Scene(id = "s5", projectId = "p", index = 4, startMs = 4000L, endMs = 5000L),
            Scene(id = "s1", projectId = "p", index = 0, startMs = 0L, endMs = 1000L),
            Scene(id = "s3", projectId = "p", index = 2, startMs = 2000L, endMs = 3000L),
            Scene(id = "s2", projectId = "p", index = 1, startMs = 1000L, endMs = 2000L)
        )

        val orderedForExport = tappedScenes.sortedBy { it.index }

        assertEquals(0, orderedForExport[0].index)
        assertEquals(1, orderedForExport[1].index)
        assertEquals(2, orderedForExport[2].index)
        assertEquals(4, orderedForExport[3].index)

        assertEquals("s1", orderedForExport[0].id)
        assertEquals("s2", orderedForExport[1].id)
        assertEquals("s3", orderedForExport[2].id)
        assertEquals("s5", orderedForExport[3].id)
    }
}
