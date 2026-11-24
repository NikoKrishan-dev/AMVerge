package com.amverge.android.backend.ai.depth

import com.amverge.android.backend.domain.model.ai.DepthColormap

object ColorLut {

    private fun rgb(r: Int, g: Int, b: Int): Int =
        (0xFF shl 24) or ((r.coerceIn(0, 255)) shl 16) or ((g.coerceIn(0, 255)) shl 8) or (b.coerceIn(0, 255))

    private fun red(c: Int): Int = (c shr 16) and 0xFF
    private fun green(c: Int): Int = (c shr 8) and 0xFF
    private fun blue(c: Int): Int = c and 0xFF

    // Reference gradient keyframes from standard perceptual scientific colormaps
    private val INFERNO_STOPS = listOf(
        0.00f to rgb(0, 0, 4),
        0.20f to rgb(40, 11, 84),
        0.40f to rgb(101, 21, 110),
        0.60f to rgb(186, 54, 85),
        0.80f to rgb(249, 142, 9),
        1.00f to rgb(252, 255, 164)
    )

    private val VIRIDIS_STOPS = listOf(
        0.00f to rgb(68, 1, 84),
        0.25f to rgb(59, 82, 139),
        0.50f to rgb(33, 145, 140),
        0.75f to rgb(94, 201, 98),
        1.00f to rgb(253, 231, 37)
    )

    private val TURBO_STOPS = listOf(
        0.00f to rgb(48, 18, 59),
        0.25f to rgb(38, 130, 238),
        0.50f to rgb(61, 222, 107),
        0.75f to rgb(249, 187, 36),
        1.00f to rgb(122, 4, 3)
    )

    private val PLASMA_STOPS = listOf(
        0.00f to rgb(13, 8, 135),
        0.25f to rgb(126, 3, 168),
        0.50f to rgb(204, 71, 120),
        0.75f to rgb(248, 149, 64),
        1.00f to rgb(240, 249, 33)
    )

    private val MAGMA_STOPS = listOf(
        0.00f to rgb(0, 0, 4),
        0.25f to rgb(59, 15, 112),
        0.50f to rgb(140, 41, 129),
        0.75f to rgb(222, 73, 104),
        1.00f to rgb(252, 253, 191)
    )

    // Precomputed 256-color lookup tables for each colormap
    private val tables: Map<DepthColormap, IntArray> by lazy {
        mapOf(
            DepthColormap.GRAYSCALE to IntArray(256) { i -> rgb(i, i, i) },
            DepthColormap.INFERNO to generateLut(INFERNO_STOPS),
            DepthColormap.VIRIDIS to generateLut(VIRIDIS_STOPS),
            DepthColormap.TURBO to generateLut(TURBO_STOPS),
            DepthColormap.PLASMA to generateLut(PLASMA_STOPS),
            DepthColormap.MAGMA to generateLut(MAGMA_STOPS)
        )
    }

    fun getColor(colormap: DepthColormap, normalizedValue: Float): Int {
        val clamped = normalizedValue.coerceIn(0f, 1f)
        val index = (clamped * 255f).toInt().coerceIn(0, 255)
        return tables[colormap]?.get(index) ?: rgb(index, index, index)
    }

    fun applyColormapToBuffer(
        depthMap: FloatArray,
        colormap: DepthColormap,
        outputPixels: IntArray
    ) {
        val lut = tables[colormap] ?: tables[DepthColormap.GRAYSCALE]!!
        for (i in depthMap.indices) {
            val d = depthMap[i].coerceIn(0f, 1f)
            val index = (d * 255f).toInt().coerceIn(0, 255)
            outputPixels[i] = lut[index]
        }
    }

    private fun generateLut(stops: List<Pair<Float, Int>>): IntArray {
        val lut = IntArray(256)
        for (i in 0..255) {
            val t = i / 255f
            var lowerStop = stops.first()
            var upperStop = stops.last()

            for (j in 0 until stops.size - 1) {
                if (t >= stops[j].first && t <= stops[j + 1].first) {
                    lowerStop = stops[j]
                    upperStop = stops[j + 1]
                    break
                }
            }

            val range = upperStop.first - lowerStop.first
            val factor = if (range > 0f) (t - lowerStop.first) / range else 0f

            val r = (red(lowerStop.second) * (1f - factor) + red(upperStop.second) * factor).toInt()
            val g = (green(lowerStop.second) * (1f - factor) + green(upperStop.second) * factor).toInt()
            val b = (blue(lowerStop.second) * (1f - factor) + blue(upperStop.second) * factor).toInt()

            lut[i] = rgb(r, g, b)
        }
        return lut
    }
}
