package com.amverge.android.backend.media.detection

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import com.amverge.android.backend.domain.model.Scene
import com.amverge.android.core.logging.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs

class MotionVarianceEvaluator(
    private val context: Context
) {
    companion object {
        private const val TAG = "MotionVarianceEvaluator"
        private const val SAMPLE_WIDTH = 64
        private const val SAMPLE_HEIGHT = 36
    }

    /**
     * Evaluates motion dynamics across a batch of scenes reusing a single MediaMetadataRetriever.
     * Uses OPTION_CLOSEST_SYNC and downscaled frame extraction for high speed.
     */
    suspend fun evaluateBatch(
        sourceUri: String,
        scenes: List<Scene>,
        deadframeThreshold: Float = 0.15f
    ): List<Scene> = withContext(Dispatchers.IO) {
        if (scenes.isEmpty()) return@withContext scenes

        val retriever = MediaMetadataRetriever()
        var retrieverReady = false
        try {
            val uri = Uri.parse(sourceUri)
            if (uri.scheme == "content") {
                retriever.setDataSource(context, uri)
            } else {
                retriever.setDataSource(sourceUri)
            }
            retrieverReady = true
        } catch (e: Exception) {
            Logger.w(TAG, "Failed to initialize MediaMetadataRetriever for motion evaluation", e)
        }

        if (!retrieverReady) return@withContext scenes

        try {
            scenes.map { scene ->
                evaluateSceneWithRetriever(retriever, scene, deadframeThreshold)
            }
        } finally {
            runCatching { retriever.release() }
        }
    }

    suspend fun evaluateSceneMotion(
        sourceUri: String,
        scene: Scene,
        deadframeThreshold: Float = 0.15f
    ): Scene = withContext(Dispatchers.IO) {
        val durationMs = scene.durationMs
        if (durationMs < 100L) {
            return@withContext scene
        }

        val retriever = MediaMetadataRetriever()
        try {
            val uri = Uri.parse(sourceUri)
            if (uri.scheme == "content") {
                retriever.setDataSource(context, uri)
            } else {
                retriever.setDataSource(sourceUri)
            }
            evaluateSceneWithRetriever(retriever, scene, deadframeThreshold)
        } catch (e: Exception) {
            Logger.w(TAG, "Failed to evaluate motion for scene ${scene.index}", e)
            scene
        } finally {
            runCatching { retriever.release() }
        }
    }

    private fun evaluateSceneWithRetriever(
        retriever: MediaMetadataRetriever,
        scene: Scene,
        deadframeThreshold: Float
    ): Scene {
        val durationMs = scene.durationMs
        if (durationMs < 100L) return scene

        return try {
            val timeUs1 = (scene.startMs + (durationMs * 0.25).toLong()) * 1000L
            val timeUs2 = (scene.startMs + (durationMs * 0.75).toLong()) * 1000L

            val bm1 = retriever.getScaledFrameAtTime(timeUs1, MediaMetadataRetriever.OPTION_CLOSEST_SYNC, SAMPLE_WIDTH, SAMPLE_HEIGHT)
                ?: retriever.getScaledFrameAtTime(timeUs1, MediaMetadataRetriever.OPTION_CLOSEST, SAMPLE_WIDTH, SAMPLE_HEIGHT)
            val bm2 = retriever.getScaledFrameAtTime(timeUs2, MediaMetadataRetriever.OPTION_CLOSEST_SYNC, SAMPLE_WIDTH, SAMPLE_HEIGHT)
                ?: retriever.getScaledFrameAtTime(timeUs2, MediaMetadataRetriever.OPTION_CLOSEST, SAMPLE_WIDTH, SAMPLE_HEIGHT)

            if (bm1 == null || bm2 == null) {
                return scene
            }

            val score = calculateMeanDifference(bm1, bm2)
            val isDead = score < deadframeThreshold
            val updatedTags = scene.tags.toMutableList()

            if (isDead) {
                if (!updatedTags.contains("static")) updatedTags.add("static")
                updatedTags.remove("action")
            } else if (score > 0.35f) {
                if (!updatedTags.contains("action")) updatedTags.add("action")
                updatedTags.remove("static")
            }

            scene.copy(
                motionScore = score,
                isDeadframe = isDead,
                tags = updatedTags
            )
        } catch (e: Exception) {
            scene
        }
    }

    fun calculateMeanDifference(bm1: Bitmap, bm2: Bitmap): Float {
        val w = minOf(bm1.width, bm2.width)
        val h = minOf(bm1.height, bm2.height)
        val pixels1 = IntArray(w * h)
        val pixels2 = IntArray(w * h)

        bm1.getPixels(pixels1, 0, w, 0, 0, w, h)
        bm2.getPixels(pixels2, 0, w, 0, 0, w, h)

        var totalDiff = 0.0
        val count = w * h

        for (i in 0 until count) {
            val p1 = pixels1[i]
            val p2 = pixels2[i]

            // Luminance: Y = 0.299R + 0.587G + 0.114B
            val r1 = (p1 shr 16) and 0xFF
            val g1 = (p1 shr 8) and 0xFF
            val b1 = p1 and 0xFF
            val y1 = 0.299f * r1 + 0.587f * g1 + 0.114f * b1

            val r2 = (p2 shr 16) and 0xFF
            val g2 = (p2 shr 8) and 0xFF
            val b2 = p2 and 0xFF
            val y2 = 0.299f * r2 + 0.587f * g2 + 0.114f * b2

            totalDiff += abs(y1 - y2)
        }

        return ((totalDiff / count) / 255.0).toFloat().coerceIn(0.0f, 1.0f)
    }
}
