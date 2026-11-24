package com.amverge.android.backend.ai

import com.amverge.android.backend.ai.depth.ColorLut
import com.amverge.android.backend.domain.model.ai.AiToolType
import com.amverge.android.backend.domain.model.ai.DepthColormap
import com.amverge.android.backend.domain.model.ai.DepthConfig
import com.amverge.android.backend.domain.model.ai.InterpolationConfig
import com.amverge.android.backend.domain.model.ai.UpscaleConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AiSuiteTest {

    @Test
    fun `InterpolationConfig validates multiplier bounds correctly`() {
        listOf(2, 4, 8, 16).forEach { mult ->
            val config = InterpolationConfig(multiplier = mult)
            assertEquals(mult, config.multiplier)
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun `InterpolationConfig throws on invalid multiplier`() {
        InterpolationConfig(multiplier = 5)
    }

    @Test
    fun `UpscaleConfig validates scale bounds correctly`() {
        val cfg2 = UpscaleConfig(scale = 2)
        assertEquals(2, cfg2.scale)

        val cfg4 = UpscaleConfig(scale = 4)
        assertEquals(4, cfg4.scale)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `UpscaleConfig throws on invalid scale`() {
        UpscaleConfig(scale = 3)
    }

    @Test
    fun `ColorLut applies colormaps across entire range without crash`() {
        val testValues = floatArrayOf(0.0f, 0.25f, 0.5f, 0.75f, 1.0f)
        val outPixels = IntArray(testValues.size)

        DepthColormap.entries.forEach { colormap ->
            ColorLut.applyColormapToBuffer(testValues, colormap, outPixels)
            outPixels.forEach { pixel ->
                assertTrue(pixel != 0)
            }
        }
    }

    @Test
    fun `ColorLut clamps out-of-bound normalized values safely`() {
        val outOfBounds = floatArrayOf(-0.5f, 1.5f)
        val outPixels = IntArray(outOfBounds.size)

        ColorLut.applyColormapToBuffer(outOfBounds, DepthColormap.INFERNO, outPixels)
        assertEquals(2, outPixels.size)
    }
}
