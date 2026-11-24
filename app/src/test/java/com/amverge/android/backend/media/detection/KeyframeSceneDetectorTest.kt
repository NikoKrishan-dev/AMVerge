package com.amverge.android.backend.media.detection

import com.amverge.android.backend.domain.model.VideoPacketInfo
import com.amverge.android.core.result.AppError
import com.amverge.android.core.result.OperationResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KeyframeSceneDetectorTest {

    private val detector = KeyframeSceneDetector()
    private val testProjectId = "test-project-123"

    @Test
    fun `detectScenes with closed GOPs creates contiguous intervals`() {
        // Closed GOP sequence: 3 keyframes at 0s, 2s, 4s in 5s video
        val packets = listOf(
            VideoPacketInfo(ptsUs = 0L, dtsUs = 0L, isKeyframe = true, demuxIndex = 0),
            VideoPacketInfo(ptsUs = 1_000_000L, dtsUs = 1_000_000L, isKeyframe = false, demuxIndex = 1),
            VideoPacketInfo(ptsUs = 2_000_000L, dtsUs = 2_000_000L, isKeyframe = true, demuxIndex = 2),
            VideoPacketInfo(ptsUs = 3_000_000L, dtsUs = 3_000_000L, isKeyframe = false, demuxIndex = 3),
            VideoPacketInfo(ptsUs = 4_000_000L, dtsUs = 4_000_000L, isKeyframe = true, demuxIndex = 4)
        )

        val result = detector.detectScenes(
            projectId = testProjectId,
            packets = packets,
            durationMs = 5000L
        )

        assertTrue(result.isSuccess)
        val scenes = (result as OperationResult.Success).data
        assertEquals(3, scenes.size)

        // Scene 0: [0, 2000)
        assertEquals(0, scenes[0].index)
        assertEquals(0L, scenes[0].startMs)
        assertEquals(2000L, scenes[0].endMs)

        // Scene 1: [2000, 4000)
        assertEquals(1, scenes[1].index)
        assertEquals(2000L, scenes[1].startMs)
        assertEquals(4000L, scenes[1].endMs)

        // Scene 2: [4000, 5000)
        assertEquals(2, scenes[2].index)
        assertEquals(4000L, scenes[2].startMs)
        assertEquals(5000L, scenes[2].endMs)
    }

    @Test
    fun `detectScenes filters out open GOP keyframe with leading B-frames`() {
        // Packet sequence:
        // Keyframe 1 at 0s (PTS=0)
        // Packet at 1s (PTS=1s)
        // Keyframe 2 at 2s (PTS=2s), but followed in demux order by B-frame with PTS=1.5s (< 2.0s) -> OPEN GOP leading picture!
        // Keyframe 3 at 4s (PTS=4s), normal closed GOP
        val packets = listOf(
            VideoPacketInfo(ptsUs = 0L, dtsUs = 0L, isKeyframe = true, demuxIndex = 0),
            VideoPacketInfo(ptsUs = 1_000_000L, dtsUs = 1_000_000L, isKeyframe = false, demuxIndex = 1),
            // Unsafe open-GOP keyframe:
            VideoPacketInfo(ptsUs = 2_000_000L, dtsUs = 2_000_000L, isKeyframe = true, demuxIndex = 2),
            VideoPacketInfo(ptsUs = 1_500_000L, dtsUs = 2_040_000L, isKeyframe = false, demuxIndex = 3), // PTS < 2.0s!
            // Safe closed keyframe:
            VideoPacketInfo(ptsUs = 4_000_000L, dtsUs = 4_000_000L, isKeyframe = true, demuxIndex = 4),
            VideoPacketInfo(ptsUs = 4_500_000L, dtsUs = 4_500_000L, isKeyframe = false, demuxIndex = 5)
        )

        val result = detector.detectScenes(
            projectId = testProjectId,
            packets = packets,
            durationMs = 6000L
        )

        assertTrue(result.isSuccess)
        val scenes = (result as OperationResult.Success).data

        // Keyframe at 2s must be excluded, only cut at 4s should remain
        assertEquals(2, scenes.size)
        assertEquals(0L, scenes[0].startMs)
        assertEquals(4000L, scenes[0].endMs)

        assertEquals(4000L, scenes[1].startMs)
        assertEquals(6000L, scenes[1].endMs)
    }

    @Test
    fun `detectScenes merges short scenes under 250ms`() {
        // Keyframes at 0ms, 100ms (too short!), 1000ms, 2000ms in 3000ms video
        val packets = listOf(
            VideoPacketInfo(ptsUs = 0L, dtsUs = 0L, isKeyframe = true, demuxIndex = 0),
            VideoPacketInfo(ptsUs = 100_000L, dtsUs = 100_000L, isKeyframe = true, demuxIndex = 1), // 100ms cut
            VideoPacketInfo(ptsUs = 1_000_000L, dtsUs = 1_000_000L, isKeyframe = true, demuxIndex = 2),
            VideoPacketInfo(ptsUs = 2_000_000L, dtsUs = 2_000_000L, isKeyframe = true, demuxIndex = 3)
        )

        val result = detector.detectScenes(
            projectId = testProjectId,
            packets = packets,
            durationMs = 3000L
        )

        assertTrue(result.isSuccess)
        val scenes = (result as OperationResult.Success).data

        // 100ms cut should be merged into [0, 1000)
        assertEquals(3, scenes.size)
        assertEquals(0L, scenes[0].startMs)
        assertEquals(1000L, scenes[0].endMs)
        assertTrue(scenes[0].durationMs >= 250L)

        assertEquals(1000L, scenes[1].startMs)
        assertEquals(2000L, scenes[1].endMs)

        assertEquals(2000L, scenes[2].startMs)
        assertEquals(3000L, scenes[2].endMs)
    }

    @Test
    fun `detectScenes normalizes non-zero origin timestamp`() {
        // Stream starts at 10.0s (10_000_000 us), keyframe at 12.0s, duration 5.0s (5000 ms)
        val packets = listOf(
            VideoPacketInfo(ptsUs = 10_000_000L, dtsUs = 10_000_000L, isKeyframe = true, demuxIndex = 0),
            VideoPacketInfo(ptsUs = 12_000_000L, dtsUs = 12_000_000L, isKeyframe = true, demuxIndex = 1)
        )

        val result = detector.detectScenes(
            projectId = testProjectId,
            packets = packets,
            durationMs = 5000L,
            originMs = 10_000L
        )

        assertTrue(result.isSuccess)
        val scenes = (result as OperationResult.Success).data
        assertEquals(2, scenes.size)
        assertEquals(0L, scenes[0].startMs)
        assertEquals(2000L, scenes[0].endMs)
        assertEquals(2000L, scenes[1].startMs)
        assertEquals(5000L, scenes[1].endMs)
    }

    @Test
    fun `detectScenes fails when no keyframes are present`() {
        val packets = listOf(
            VideoPacketInfo(ptsUs = 0L, dtsUs = 0L, isKeyframe = false, demuxIndex = 0),
            VideoPacketInfo(ptsUs = 1_000_000L, dtsUs = 1_000_000L, isKeyframe = false, demuxIndex = 1)
        )

        val result = detector.detectScenes(
            projectId = testProjectId,
            packets = packets,
            durationMs = 5000L
        )

        assertTrue(result.isFailure)
        val error = (result as OperationResult.Failure).error
        assertTrue(error is AppError.NoUsableKeyframes)
    }
}
