package com.amverge.android.backend.media.detection

import com.amverge.android.backend.domain.model.TimeRange
import com.amverge.android.core.common.MediaTime

object SceneBoundaryNormalizer {

    /**
     * Normalizes raw cut points into a contiguous, non-overlapping sequence of scene intervals
     * covering the entire media duration from 0 to durationMs.
     *
     * @param rawCutsMs List of raw keyframe timestamps in milliseconds.
     * @param durationMs Total duration of the media in milliseconds.
     * @param originMs Stream start timestamp to offset against (normalized to 0).
     * @param minDurationMs Minimum allowed scene duration (default 250 ms).
     */
    fun normalizeAndBuildIntervals(
        rawCutsMs: List<Long>,
        durationMs: Long,
        originMs: Long = 0L,
        minDurationMs: Long = MediaTime.MIN_SCENE_DURATION_MS
    ): List<TimeRange> {
        if (durationMs <= 0) return emptyList()

        if (durationMs <= minDurationMs) {
            // Entire media is shorter than minimum scene duration; return single scene
            return listOf(TimeRange(startMs = 0L, endMs = durationMs))
        }

        // 1. Normalize against origin
        val normalizedCuts = rawCutsMs
            .map { it - originMs }
            .filter { it > 0L && it < durationMs }
            .sorted()

        // 2. Deduplicate cuts that are too close (within 10ms tolerance)
        val deduplicated = mutableListOf<Long>()
        for (cut in normalizedCuts) {
            if (deduplicated.isEmpty() || cut - deduplicated.last() >= 10L) {
                deduplicated.add(cut)
            }
        }

        // 3. Merge short intervals (< minDurationMs)
        // A scene is [currentStart, cut]. It must be >= minDurationMs,
        // and the remaining duration (durationMs - cut) must also be >= minDurationMs.
        val validCuts = mutableListOf<Long>()
        var lastCut = 0L

        for (cut in deduplicated) {
            val sceneDuration = cut - lastCut
            val remainingAfterCut = durationMs - cut

            if (sceneDuration >= minDurationMs && remainingAfterCut >= minDurationMs) {
                validCuts.add(cut)
                lastCut = cut
            }
        }

        // 4. Build contiguous intervals [0, c1), [c1, c2), ... [cN, durationMs)
        val intervals = mutableListOf<TimeRange>()
        var intervalStart = 0L

        for (cut in validCuts) {
            if (cut > intervalStart) {
                intervals.add(TimeRange(startMs = intervalStart, endMs = cut))
                intervalStart = cut
            }
        }

        if (durationMs > intervalStart) {
            intervals.add(TimeRange(startMs = intervalStart, endMs = durationMs))
        }

        return intervals
    }
}
