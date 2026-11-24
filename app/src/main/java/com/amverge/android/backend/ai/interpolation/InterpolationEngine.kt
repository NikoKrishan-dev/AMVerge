package com.amverge.android.backend.ai.interpolation

import android.content.Context
import android.graphics.Bitmap
import com.amverge.android.backend.domain.model.ai.InterpolationConfig
import com.amverge.android.backend.domain.model.ai.InterpolationModel
import com.amverge.android.core.logging.Logger
import java.io.File
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class InterpolationEngine(
    private val context: Context
) {
    companion object {
        private const val TAG = "InterpolationEngine"
        private const val BLOCK_SIZE = 8
        private const val SEARCH_RANGE = 12
    }

    /**
     * Given two consecutive frames (f0 and f1) and an interpolation multiplier (2, 4, 8, or 16),
     * returns a list of (multiplier - 1) synthesized intermediate frames.
     *
     * Example:
     * For multiplier = 4, returns 3 frames at t = 0.25, 0.50, 0.75.
     * For multiplier = 16, returns 15 frames at t = 1/16, 2/16, ..., 15/16.
     */
    fun interpolateFrames(
        f0: Bitmap,
        f1: Bitmap,
        config: InterpolationConfig
    ): List<Bitmap> {
        val multiplier = config.multiplier
        val intermediateFrames = mutableListOf<Bitmap>()

        // Precompute bidirectional motion vector field between f0 and f1
        val width = f0.width
        val height = f0.height

        val p0 = IntArray(width * height)
        val p1 = IntArray(width * height)
        f0.getPixels(p0, 0, width, 0, 0, width, height)
        f1.getPixels(p1, 0, width, 0, 0, width, height)

        val blocksX = max(1, width / BLOCK_SIZE)
        val blocksY = max(1, height / BLOCK_SIZE)
        val totalBlocks = blocksX * blocksY

        val flowDx = FloatArray(totalBlocks)
        val flowDy = FloatArray(totalBlocks)
        val flowConfidence = FloatArray(totalBlocks)

        estimateBlockFlow(p0, p1, width, height, blocksX, blocksY, flowDx, flowDy, flowConfidence)

        // Timestep looping: generate frames at t = k / multiplier
        for (step in 1 until multiplier) {
            val t = step.toFloat() / multiplier.toFloat()
            val intermediate = synthesizeFrame(p0, p1, width, height, blocksX, blocksY, flowDx, flowDy, flowConfidence, t)
            intermediateFrames.add(intermediate)
        }

        return intermediateFrames
    }

    /**
     * Estimates bidirectional motion vectors using sum-of-absolute-differences (SAD) with
     * spatial search and confidence scoring.
     */
    private fun estimateBlockFlow(
        p0: IntArray,
        p1: IntArray,
        width: Int,
        height: Int,
        blocksX: Int,
        blocksY: Int,
        flowDx: FloatArray,
        flowDy: FloatArray,
        flowConfidence: FloatArray
    ) {
        for (by in 0 until blocksY) {
            val startY = by * BLOCK_SIZE
            val blockRow = by * blocksX
            for (bx in 0 until blocksX) {
                val startX = bx * BLOCK_SIZE
                var bestSad = Int.MAX_VALUE
                var bestDx = 0
                var bestDy = 0
                var zeroSad = 0

                // Hierarchical sub-sampled SAD search
                for (dy in -SEARCH_RANGE..SEARCH_RANGE step 2) {
                    val refY = startY + dy
                    if (refY < 0 || refY + BLOCK_SIZE > height) continue

                    for (dx in -SEARCH_RANGE..SEARCH_RANGE step 2) {
                        val refX = startX + dx
                        if (refX < 0 || refX + BLOCK_SIZE > width) continue

                        var sad = 0
                        for (py in 0 until BLOCK_SIZE step 2) {
                            val row0 = (startY + py) * width
                            val row1 = (refY + py) * width
                            for (px in 0 until BLOCK_SIZE step 2) {
                                val c0 = p0[row0 + (startX + px)]
                                val c1 = p1[row1 + (refX + px)]
                                val dr = abs(((c0 shr 16) and 0xFF) - ((c1 shr 16) and 0xFF))
                                val dg = abs(((c0 shr 8) and 0xFF) - ((c1 shr 8) and 0xFF))
                                val db = abs((c0 and 0xFF) - (c1 and 0xFF))
                                sad += dr + dg + db
                            }
                        }

                        if (dx == 0 && dy == 0) zeroSad = sad

                        if (sad < bestSad) {
                            bestSad = sad
                            bestDx = dx
                            bestDy = dy
                        }
                    }
                }

                val bIdx = blockRow + bx
                if (bIdx < flowDx.size) {
                    // Small motion bias to eliminate jitter on static background
                    if (bestSad > (zeroSad * 0.85f).toInt()) {
                        flowDx[bIdx] = 0f
                        flowDy[bIdx] = 0f
                        flowConfidence[bIdx] = 1.0f
                    } else {
                        flowDx[bIdx] = bestDx.toFloat()
                        flowDy[bIdx] = bestDy.toFloat()
                        // Confidence drops if SAD is unusually high (e.g. occlusion or scene cut)
                        val avgDiffPerPixel = bestSad / 16f
                        flowConfidence[bIdx] = (1.0f - (avgDiffPerPixel / 128f)).coerceIn(0f, 1f)
                    }
                }
            }
        }
    }

    /**
     * Synthesizes an intermediate frame at timestep t with continuous bilinearly smoothed motion vectors
     * and motion-compensated warping from both forward (f0) and backward (f1) frames.
     */
    private fun synthesizeFrame(
        p0: IntArray,
        p1: IntArray,
        width: Int,
        height: Int,
        blocksX: Int,
        blocksY: Int,
        flowDx: FloatArray,
        flowDy: FloatArray,
        flowConfidence: FloatArray,
        t: Float
    ): Bitmap {
        val outPixels = IntArray(width * height)
        val w0 = 1.0f - t
        val w1 = t

        val scaleBx = 1.0f / BLOCK_SIZE.toFloat()

        for (y in 0 until height) {
            val byFloat = (y * scaleBx) - 0.5f
            val by0 = byFloat.toInt().coerceIn(0, blocksY - 1)
            val by1 = min(blocksY - 1, by0 + 1)
            val byFrac = (byFloat - by0).coerceIn(0f, 1f)

            val rowOffset = y * width

            for (x in 0 until width) {
                val bxFloat = (x * scaleBx) - 0.5f
                val bx0 = bxFloat.toInt().coerceIn(0, blocksX - 1)
                val bx1 = min(blocksX - 1, bx0 + 1)
                val bxFrac = (bxFloat - bx0).coerceIn(0f, 1f)

                // Bilinear interpolation of motion vector field to prevent block tearing
                val idx00 = by0 * blocksX + bx0
                val idx10 = by0 * blocksX + bx1
                val idx01 = by1 * blocksX + bx0
                val idx11 = by1 * blocksX + bx1

                val dx0 = flowDx[idx00] * (1f - bxFrac) + flowDx[idx10] * bxFrac
                val dx1 = flowDx[idx01] * (1f - bxFrac) + flowDx[idx11] * bxFrac
                val vx = dx0 * (1f - byFrac) + dx1 * byFrac

                val dy0 = flowDy[idx00] * (1f - bxFrac) + flowDy[idx10] * bxFrac
                val dy1 = flowDy[idx01] * (1f - bxFrac) + flowDy[idx11] * bxFrac
                val vy = dy0 * (1f - byFrac) + dy1 * byFrac

                val conf = flowConfidence[idx00]

                // Forward warp: where did pixel come from in f0? x - vx * t
                val srcX0 = (x - (vx * t).roundToInt()).coerceIn(0, width - 1)
                val srcY0 = (y - (vy * t).roundToInt()).coerceIn(0, height - 1)
                val c0 = p0[srcY0 * width + srcX0]

                // Backward warp: where does pixel go in f1? x + vx * (1 - t)
                val srcX1 = (x + (vx * (1f - t)).roundToInt()).coerceIn(0, width - 1)
                val srcY1 = (y + (vy * (1f - t)).roundToInt()).coerceIn(0, height - 1)
                val c1 = p1[srcY1 * width + srcX1]

                // Unpack RGB
                val r0 = (c0 shr 16) and 0xFF
                val g0 = (c0 shr 8) and 0xFF
                val b0 = c0 and 0xFF

                val r1 = (c1 shr 16) and 0xFF
                val g1 = (c1 shr 8) and 0xFF
                val b1 = c1 and 0xFF

                // Motion-compensated blending with confidence weighting
                val mcR = (r0 * w0 + r1 * w1).toInt().coerceIn(0, 255)
                val mcG = (g0 * w0 + g1 * w1).toInt().coerceIn(0, 255)
                val mcB = (b0 * w0 + b1 * w1).toInt().coerceIn(0, 255)

                // If confidence is low, fall back gracefully to direct crossfade
                val directR = ((((p0[rowOffset + x] shr 16) and 0xFF) * w0) + (((p1[rowOffset + x] shr 16) and 0xFF) * w1)).toInt().coerceIn(0, 255)
                val directG = ((((p0[rowOffset + x] shr 8) and 0xFF) * w0) + (((p1[rowOffset + x] shr 8) and 0xFF) * w1)).toInt().coerceIn(0, 255)
                val directB = (((p0[rowOffset + x] and 0xFF) * w0) + ((p1[rowOffset + x] and 0xFF) * w1)).toInt().coerceIn(0, 255)

                val outR = (mcR * conf + directR * (1f - conf)).toInt().coerceIn(0, 255)
                val outG = (mcG * conf + directG * (1f - conf)).toInt().coerceIn(0, 255)
                val outB = (mcB * conf + directB * (1f - conf)).toInt().coerceIn(0, 255)

                outPixels[rowOffset + x] = (0xFF shl 24) or (outR shl 16) or (outG shl 8) or outB
            }
        }

        val outBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        outBitmap.setPixels(outPixels, 0, width, 0, 0, width, height)
        return outBitmap
    }
}
