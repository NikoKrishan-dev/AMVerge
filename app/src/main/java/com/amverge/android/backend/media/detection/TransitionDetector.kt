package com.amverge.android.backend.media.detection

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import com.amverge.android.backend.domain.model.TimeRange
import com.amverge.android.core.logging.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TransitionDetector(
    private val context: Context
) {
    companion object {
        private const val TAG = "TransitionDetector"
        private const val SAMPLE_WIDTH = 32
        private const val SAMPLE_HEIGHT = 18
        private const val FADE_BLACK_LUMA = 18.0f
        private const val FADE_WHITE_LUMA = 238.0f
    }

    /**
     * Refines intervals by inspecting long scene spans for black/white dip transitions.
     */
    suspend fun detectTransitions(
        sourceUri: String,
        intervals: List<TimeRange>,
        minSceneDurationMs: Long = 250L
    ): List<TimeRange> = withContext(Dispatchers.IO) {
        if (intervals.isEmpty()) return@withContext emptyList()

        val retriever = MediaMetadataRetriever()
        try {
            val uri = Uri.parse(sourceUri)
            if (uri.scheme == "content") {
                retriever.setDataSource(context, uri)
            } else {
                retriever.setDataSource(sourceUri)
            }

            val refinedIntervals = mutableListOf<TimeRange>()

            for (interval in intervals) {
                val duration = interval.endMs - interval.startMs
                // Only inspect long intervals (> 4 seconds) where gradual dissolves or black dips typically happen
                if (duration <= 4000L) {
                    refinedIntervals.add(interval)
                    continue
                }

                val transitionCuts = findTransitionCutsInInterval(
                    retriever = retriever,
                    startMs = interval.startMs,
                    endMs = interval.endMs,
                    minDurationMs = minSceneDurationMs
                )

                if (transitionCuts.isEmpty()) {
                    refinedIntervals.add(interval)
                } else {
                    var currentStart = interval.startMs
                    for (cut in transitionCuts) {
                        if (cut - currentStart >= minSceneDurationMs && interval.endMs - cut >= minSceneDurationMs) {
                            refinedIntervals.add(TimeRange(currentStart, cut))
                            currentStart = cut
                        }
                    }
                    if (interval.endMs - currentStart >= minSceneDurationMs) {
                        refinedIntervals.add(TimeRange(currentStart, interval.endMs))
                    } else if (refinedIntervals.isNotEmpty()) {
                        // Merge tail into previous
                        val last = refinedIntervals.removeAt(refinedIntervals.size - 1)
                        refinedIntervals.add(TimeRange(last.startMs, interval.endMs))
                    } else {
                        refinedIntervals.add(interval)
                    }
                }
            }

            refinedIntervals
        } catch (e: Exception) {
            Logger.w(TAG, "Transition detection failed, using original intervals", e)
            intervals
        } finally {
            runCatching { retriever.release() }
        }
    }

    private fun findTransitionCutsInInterval(
        retriever: MediaMetadataRetriever,
        startMs: Long,
        endMs: Long,
        minDurationMs: Long
    ): List<Long> {
        val cuts = mutableListOf<Long>()
        val stepMs = 1000L // 1-second coarse step
        var t = startMs + minDurationMs

        while (t < endMs - minDurationMs) {
            val timeUs = t * 1000L
            val bm = retriever.getScaledFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST, SAMPLE_WIDTH, SAMPLE_HEIGHT)
                ?: retriever.getFrameAtTime(timeUs)

            if (bm != null) {
                val avgLuma = calculateAverageLuma(bm)
                // Check if this point is a fade to black or flash to white
                if (avgLuma <= FADE_BLACK_LUMA || avgLuma >= FADE_WHITE_LUMA) {
                    cuts.add(t)
                    t += minDurationMs // skip forward past cut
                }
            }
            t += stepMs
        }
        return cuts
    }

    fun calculateAverageLuma(bm: Bitmap): Float {
        val w = bm.width
        val h = bm.height
        val pixels = IntArray(w * h)
        bm.getPixels(pixels, 0, w, 0, 0, w, h)

        var sum = 0.0
        val count = w * h
        for (p in pixels) {
            val r = (p shr 16) and 0xFF
            val g = (p shr 8) and 0xFF
            val b = p and 0xFF
            sum += (0.299f * r + 0.587f * g + 0.114f * b)
        }
        return (sum / count).toFloat()
    }
}
