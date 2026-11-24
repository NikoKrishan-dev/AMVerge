package com.amverge.android.backend.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class TimeRangeTest {

    @Test
    fun `valid range computes duration correctly`() {
        val range = TimeRange(startMs = 1000L, endMs = 3500L)
        assertEquals(2500L, range.durationMs)
    }

    @Test
    fun `negative startMs throws IllegalArgumentException`() {
        assertThrows(IllegalArgumentException::class.java) {
            TimeRange(startMs = -10L, endMs = 1000L)
        }
    }

    @Test
    fun `endMs equal or less than startMs throws IllegalArgumentException`() {
        assertThrows(IllegalArgumentException::class.java) {
            TimeRange(startMs = 1000L, endMs = 1000L)
        }
        assertThrows(IllegalArgumentException::class.java) {
            TimeRange(startMs = 2000L, endMs = 1000L)
        }
    }
}
