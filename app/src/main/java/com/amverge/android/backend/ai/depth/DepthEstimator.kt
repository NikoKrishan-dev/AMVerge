package com.amverge.android.backend.ai.depth

import android.content.Context
import android.graphics.Bitmap
import com.amverge.android.backend.domain.model.ai.DepthColormap
import com.amverge.android.backend.domain.model.ai.DepthConfig
import com.amverge.android.backend.domain.model.ai.DepthModel
import com.amverge.android.core.logging.Logger
import org.tensorflow.lite.Interpreter
import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

class DepthEstimator(
    private val context: Context
) {
    companion object {
        private const val TAG = "DepthEstimator"
        private const val MODEL_INPUT_SIZE = 256

        // ImageNet normalization constants for MiDaS
        private val MEAN = floatArrayOf(0.485f, 0.456f, 0.406f)
        private val STD = floatArrayOf(0.229f, 0.224f, 0.225f)
    }

    private var interpreter: Interpreter? = null
    private var isModelLoaded: Boolean = false

    init {
        initInterpreter()
    }

    private fun initInterpreter() {
        try {
            val modelBuffer = loadModelBuffer()
            if (modelBuffer != null) {
                val options = Interpreter.Options().apply {
                    setNumThreads(4)
                    setUseNNAPI(false)
                }
                interpreter = Interpreter(modelBuffer, options)
                isModelLoaded = true
                Logger.i(TAG, "Neural MiDaS v2.1 Small model loaded and initialized successfully")
            } else {
                Logger.w(TAG, "MiDaS model buffer not found in assets or storage; will use high-contrast anime saliency depth")
            }
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to initialize MiDaS Interpreter", e)
        }
    }

    private fun loadModelBuffer(): MappedByteBuffer? {
        // 1. Try bundled assets first
        try {
            val assetFd = context.assets.openFd("ai_models/midas_small.tflite")
            val inputStream = FileInputStream(assetFd.fileDescriptor)
            val fileChannel = inputStream.channel
            return fileChannel.map(FileChannel.MapMode.READ_ONLY, assetFd.startOffset, assetFd.declaredLength)
        } catch (e: Exception) {
            Logger.d(TAG, "Asset ai_models/midas_small.tflite not found, checking external filesDir")
        }

        // 2. Try downloaded filesDir/ai_models
        try {
            val extFile = File(context.filesDir, "ai_models/midas_small.tflite")
            if (extFile.exists()) {
                val inputStream = FileInputStream(extFile)
                val fileChannel = inputStream.channel
                return fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, extFile.length())
            }
        } catch (e: Exception) {
            Logger.d(TAG, "External model file check failed: ${e.message}")
        }

        return null
    }

    /**
     * Estimates depth for a single video frame bitmap and returns the colormapped depth bitmap.
     */
    fun processFrame(
        inputFrame: Bitmap,
        config: DepthConfig
    ): Bitmap {
        val width = inputFrame.width
        val height = inputFrame.height

        val depthMap = if (isModelLoaded && interpreter != null) {
            runNeuralDepth(inputFrame, width, height)
        } else {
            runSaliencyDepth(inputFrame, width, height)
        }

        // Apply selected colormap or grayscale
        val outputPixels = IntArray(width * height)
        val colormap = if (config.grayscale) DepthColormap.GRAYSCALE else config.colormap
        ColorLut.applyColormapToBuffer(depthMap, colormap, outputPixels)

        val outputBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        outputBitmap.setPixels(outputPixels, 0, width, 0, 0, width, height)
        return outputBitmap
    }

    /**
     * Executes neural inference using the official MiDaS v2.1 small model.
     * Resizes frame to 256x256, normalizes RGB channels, executes TFLite inference,
     * normalizes predicted relative inverse depth to [0.0, 1.0], and upsamples bilinearly to (width, height).
     */
    private fun runNeuralDepth(
        inputFrame: Bitmap,
        targetWidth: Int,
        targetHeight: Int
    ): FloatArray {
        val scaledInput = if (inputFrame.width == MODEL_INPUT_SIZE && inputFrame.height == MODEL_INPUT_SIZE) {
            inputFrame
        } else {
            Bitmap.createScaledBitmap(inputFrame, MODEL_INPUT_SIZE, MODEL_INPUT_SIZE, true)
        }

        val inputPixels = IntArray(MODEL_INPUT_SIZE * MODEL_INPUT_SIZE)
        scaledInput.getPixels(inputPixels, 0, MODEL_INPUT_SIZE, 0, 0, MODEL_INPUT_SIZE, MODEL_INPUT_SIZE)
        if (scaledInput !== inputFrame) {
            scaledInput.recycle()
        }

        // Prepare input float buffer (1, 256, 256, 3)
        val inputBuffer = ByteBuffer.allocateDirect(1 * MODEL_INPUT_SIZE * MODEL_INPUT_SIZE * 3 * 4)
            .order(ByteOrder.nativeOrder())
        inputBuffer.rewind()

        for (i in 0 until MODEL_INPUT_SIZE * MODEL_INPUT_SIZE) {
            val p = inputPixels[i]
            val r = ((p shr 16) and 0xFF) / 255.0f
            val g = ((p shr 8) and 0xFF) / 255.0f
            val b = (p and 0xFF) / 255.0f

            val rNorm = (r - MEAN[0]) / STD[0]
            val gNorm = (g - MEAN[1]) / STD[1]
            val bNorm = (b - MEAN[2]) / STD[2]

            inputBuffer.putFloat(rNorm)
            inputBuffer.putFloat(gNorm)
            inputBuffer.putFloat(bNorm)
        }

        // Prepare output buffer (1, 256, 256)
        val outputBuffer = ByteBuffer.allocateDirect(1 * MODEL_INPUT_SIZE * MODEL_INPUT_SIZE * 4)
            .order(ByteOrder.nativeOrder())
        outputBuffer.rewind()

        // Run inference
        interpreter?.run(inputBuffer, outputBuffer)

        outputBuffer.rewind()
        val rawDepth256 = FloatArray(MODEL_INPUT_SIZE * MODEL_INPUT_SIZE)
        var minVal = Float.MAX_VALUE
        var maxVal = Float.MIN_VALUE

        for (i in 0 until MODEL_INPUT_SIZE * MODEL_INPUT_SIZE) {
            val d = outputBuffer.float
            rawDepth256[i] = d
            if (d < minVal) minVal = d
            if (d > maxVal) maxVal = d
        }

        // Normalize 256x256 depth map to [0.0, 1.0] (higher = closer)
        val range = if (maxVal > minVal) maxVal - minVal else 1.0f
        for (i in rawDepth256.indices) {
            rawDepth256[i] = ((rawDepth256[i] - minVal) / range).coerceIn(0f, 1f)
        }

        // Bilinear upsampling to target (targetWidth, targetHeight)
        return upsampleBilinear(rawDepth256, MODEL_INPUT_SIZE, MODEL_INPUT_SIZE, targetWidth, targetHeight)
    }

    /**
     * Bilinear interpolation to scale depth map from (srcW, srcH) to (dstW, dstH).
     */
    private fun upsampleBilinear(
        src: FloatArray,
        srcW: Int,
        srcH: Int,
        dstW: Int,
        dstH: Int
    ): FloatArray {
        if (srcW == dstW && srcH == dstH) return src

        val dst = FloatArray(dstW * dstH)
        val scaleX = (srcW - 1).toFloat() / max(1, dstW - 1)
        val scaleY = (srcH - 1).toFloat() / max(1, dstH - 1)

        for (y in 0 until dstH) {
            val srcYFloat = y * scaleY
            val yLow = srcYFloat.toInt()
            val yHigh = min(srcH - 1, yLow + 1)
            val yFrac = srcYFloat - yLow

            val rowOffset = y * dstW
            val srcRowLow = yLow * srcW
            val srcRowHigh = yHigh * srcW

            for (x in 0 until dstW) {
                val srcXFloat = x * scaleX
                val xLow = srcXFloat.toInt()
                val xHigh = min(srcW - 1, xLow + 1)
                val xFrac = srcXFloat - xLow

                val d00 = src[srcRowLow + xLow]
                val d10 = src[srcRowLow + xHigh]
                val d01 = src[srcRowHigh + xLow]
                val d11 = src[srcRowHigh + xHigh]

                val top = d00 * (1f - xFrac) + d10 * xFrac
                val bottom = d01 * (1f - xFrac) + d11 * xFrac
                dst[rowOffset + x] = top * (1f - yFrac) + bottom * yFrac
            }
        }
        return dst
    }

    /**
     * Fallback high-contrast anime saliency depth estimation:
     * Combines edge contours (Sobel), center-surround saliency, color saturation,
     * and subtle vertical ground perspective for true foreground character isolation.
     */
    private fun runSaliencyDepth(
        inputFrame: Bitmap,
        width: Int,
        height: Int
    ): FloatArray {
        val pixels = IntArray(width * height)
        inputFrame.getPixels(pixels, 0, width, 0, 0, width, height)

        val depthMap = FloatArray(width * height)
        val lum = FloatArray(width * height)
        val sat = FloatArray(width * height)

        for (i in 0 until width * height) {
            val p = pixels[i]
            val r = ((p shr 16) and 0xFF) / 255.0f
            val g = ((p shr 8) and 0xFF) / 255.0f
            val b = (p and 0xFF) / 255.0f

            lum[i] = 0.299f * r + 0.587f * g + 0.114f * b
            val cMax = max(r, max(g, b))
            val cMin = min(r, min(g, b))
            sat[i] = if (cMax > 0f) (cMax - cMin) / cMax else 0f
        }

        var minVal = Float.MAX_VALUE
        var maxVal = Float.MIN_VALUE

        val centerX = width / 2f
        val centerY = height / 2f
        val maxDist = sqrt(centerX * centerX + centerY * centerY)

        for (y in 1 until height - 1) {
            val row = y * width
            val rowUp = (y - 1) * width
            val rowDown = (y + 1) * width

            for (x in 1 until width - 1) {
                // Sobel edge detection for crisp character linework
                val gx = (lum[rowDown + x + 1] + 2 * lum[row + x + 1] + lum[rowUp + x + 1]) -
                        (lum[rowDown + x - 1] + 2 * lum[row + x - 1] + lum[rowUp + x - 1])
                val gy = (lum[rowDown + x - 1] + 2 * lum[rowDown + x] + lum[rowDown + x + 1]) -
                        (lum[rowUp + x - 1] + 2 * lum[rowUp + x] + lum[rowUp + x + 1])
                val edge = sqrt(gx * gx + gy * gy).coerceIn(0f, 1f)

                // Center-weighted subject bias (characters are typically centered)
                val dx = x - centerX
                val dy = y - centerY
                val dist = sqrt(dx * dx + dy * dy) / maxDist
                val centerWeight = 1.0f - dist.coerceIn(0f, 1f)

                // High saturation and edge density indicate foreground anime characters
                val score = (sat[row + x] * 0.40f) + (edge * 0.35f) + (centerWeight * 0.25f)
                depthMap[row + x] = score

                if (score < minVal) minVal = score
                if (score > maxVal) maxVal = score
            }
        }

        // Normalize
        val range = if (maxVal > minVal) maxVal - minVal else 1.0f
        for (i in depthMap.indices) {
            depthMap[i] = ((depthMap[i] - minVal) / range).coerceIn(0f, 1f)
        }

        return depthMap
    }
}
