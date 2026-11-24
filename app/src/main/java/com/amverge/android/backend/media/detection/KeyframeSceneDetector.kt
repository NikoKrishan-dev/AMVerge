package com.amverge.android.backend.media.detection

import com.amverge.android.backend.domain.model.Scene
import com.amverge.android.backend.domain.model.ThumbnailStatus
import com.amverge.android.backend.domain.model.TimeRange
import com.amverge.android.backend.domain.model.VideoPacketInfo
import com.amverge.android.core.common.MediaTime
import com.amverge.android.core.result.AppError
import com.amverge.android.core.result.OperationResult
import java.util.UUID

class KeyframeSceneDetector {

    /**
     * Detects scene intervals using video packet keyframe metadata.
     * Excludes unsafe open-GOP boundaries and merges short intervals under 250 ms.
     *
     * @param projectId ID of the owning project.
     * @param packets First video stream packets in demux/decode order.
     * @param durationMs Probed duration of the media in milliseconds.
     * @param originMs Stream start timestamp offset in milliseconds.
     * @param minDurationMs Minimum scene duration threshold (default 250 ms).
     * @return OperationResult containing chronological, non-overlapping Scene instances.
     */
    fun detectScenes(
        projectId: String,
        packets: List<VideoPacketInfo>,
        durationMs: Long,
        originMs: Long = 0L,
        minDurationMs: Long = MediaTime.MIN_SCENE_DURATION_MS
    ): OperationResult<List<Scene>> {
        if (durationMs <= 0L) {
            return OperationResult.Failure(
                AppError.InvalidMediaDuration("Media duration must be positive: $durationMs ms")
            )
        }

        if (packets.isEmpty()) {
            return OperationResult.Failure(
                AppError.NoVideoStream("No video packets found for scene detection")
            )
        }

        val hasKeyframes = packets.any { it.isKeyframe }
        if (!hasKeyframes) {
            return OperationResult.Failure(
                AppError.NoUsableKeyframes("No keyframes found in video stream")
            )
        }

        // 1. Filter safe keyframes excluding open-GOP leading picture boundaries
        val safeKeyframes = OpenGopBoundaryFilter.filterSafeKeyframes(packets)

        // 2. Extract timestamps in milliseconds
        val rawCutsMs = safeKeyframes
            .mapNotNull { it.ptsUs }
            .map { it / 1000L }

        // 3. Normalize cuts and construct contiguous intervals
        val intervals: List<TimeRange> = SceneBoundaryNormalizer.normalizeAndBuildIntervals(
            rawCutsMs = rawCutsMs,
            durationMs = durationMs,
            originMs = originMs,
            minDurationMs = minDurationMs
        )

        if (intervals.isEmpty()) {
            return OperationResult.Failure(
                AppError.DetectionFailed("Failed to construct valid scene intervals from keyframes")
            )
        }

        // 4. Map intervals to Scene domain objects
        val scenes = intervals.mapIndexed { index, interval ->
            Scene(
                id = UUID.randomUUID().toString(),
                projectId = projectId,
                index = index,
                startMs = interval.startMs,
                endMs = interval.endMs,
                thumbnailPath = null,
                thumbnailStatus = ThumbnailStatus.PENDING
            )
        }

        // 5. Verify invariants
        for (i in 0 until scenes.size - 1) {
            val current = scenes[i]
            val next = scenes[i + 1]
            if (current.endMs != next.startMs) {
                return OperationResult.Failure(
                    AppError.DetectionFailed("Non-contiguous scene boundaries detected at index $i")
                )
            }
        }

        return OperationResult.Success(scenes)
    }
}
