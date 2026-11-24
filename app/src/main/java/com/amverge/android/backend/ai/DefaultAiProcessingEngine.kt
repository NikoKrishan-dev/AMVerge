package com.amverge.android.backend.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.amverge.android.backend.ai.depth.DepthEstimator
import com.amverge.android.backend.ai.interpolation.InterpolationEngine
import com.amverge.android.backend.ai.upscale.UpscaleEngine
import com.amverge.android.backend.domain.model.ai.AiToolRequest
import com.amverge.android.backend.domain.model.ai.AiToolResult
import com.amverge.android.backend.domain.model.ai.AiToolType
import com.amverge.android.backend.media.ffmpeg.MediaEngine
import com.amverge.android.core.logging.Logger
import com.amverge.android.core.result.AppError
import com.amverge.android.core.result.OperationResult
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import java.util.UUID

class DefaultAiProcessingEngine(
    private val context: Context,
    private val mediaEngine: MediaEngine,
    private val depthEstimator: DepthEstimator = DepthEstimator(context),
    private val upscaleEngine: UpscaleEngine = UpscaleEngine(context),
    private val interpolationEngine: InterpolationEngine = InterpolationEngine(context)
) : AiProcessingEngine {

    companion object {
        private const val TAG = "DefaultAiProcessingEngine"
        private const val MAX_MOBILE_FPS = 120.0f
    }

    override suspend fun executeAiTool(
        request: AiToolRequest,
        inputVideoFile: File,
        outputVideoFile: File,
        onProgress: suspend (progress: Float, message: String) -> Unit
    ): OperationResult<AiToolResult> = withContext(Dispatchers.IO) {
        if (!inputVideoFile.exists()) {
            return@withContext OperationResult.Failure(
                AppError.SourceNotFound("Input video file does not exist: ${inputVideoFile.absolutePath}")
            )
        }

        // 1. Probe source video metadata
        val probeResult = mediaEngine.probe(inputVideoFile.absolutePath)
        val mediaInfo = if (probeResult is OperationResult.Success) {
            probeResult.data
        } else {
            return@withContext OperationResult.Failure(
                AppError.NoVideoStream("Failed to probe video characteristics for AI processing")
            )
        }

        val baseFps = mediaInfo.frameRate?.toFloat()?.takeIf { it > 1.0f } ?: 24.0f
        val origWidth = mediaInfo.width ?: 1280
        val origHeight = mediaInfo.height ?: 720

        val stagingDir = File(context.cacheDir, "ai_staging_${UUID.randomUUID()}")
        stagingDir.mkdirs()
        val inFramesDir = File(stagingDir, "in_frames").apply { mkdirs() }
        val outFramesDir = File(stagingDir, "out_frames").apply { mkdirs() }

        try {
            // 2. Decode video into individual frame images
            onProgress(0.05f, "Decoding input video frames...")
            val extractArgs = arrayOf(
                "-hide_banner",
                "-i", inputVideoFile.absolutePath,
                "-q:v", "2",
                "-f", "image2",
                File(inFramesDir, "frame_%06d.jpg").absolutePath
            )
            val extractSession = FFmpegKit.executeWithArguments(extractArgs)
            if (!ReturnCode.isSuccess(extractSession.returnCode)) {
                return@withContext OperationResult.Failure(
                    AppError.MergeFailed("Failed to extract video frames for AI processing")
                )
            }

            val frameFiles = inFramesDir.listFiles { f -> f.extension.equals("jpg", ignoreCase = true) }
                ?.sortedBy { it.name } ?: emptyList()

            if (frameFiles.isEmpty()) {
                return@withContext OperationResult.Failure(
                    AppError.NoVideoStream("No video frames extracted from input clip")
                )
            }

            val totalInFrames = frameFiles.size
            Logger.d(TAG, "Extracted $totalInFrames frames for ${request.toolType}")

            var targetFps = baseFps
            var finalWidth = origWidth
            var finalHeight = origHeight
            var outFrameIndex = 1

            // 3. Process frames with requested AI tool
            when (request.toolType) {
                AiToolType.DEPTH_MAP -> {
                    onProgress(0.15f, "Generating neural depth maps (${request.depthConfig.model.displayName})...")
                    frameFiles.forEachIndexed { i, frameFile ->
                        val bmp = BitmapFactory.decodeFile(frameFile.absolutePath)
                        if (bmp != null) {
                            val depthBmp = depthEstimator.processFrame(bmp, request.depthConfig)
                            val outFile = File(outFramesDir, String.format(Locale.US, "frame_%06d.jpg", outFrameIndex++))
                            saveBitmap(depthBmp, outFile)
                            finalWidth = depthBmp.width
                            finalHeight = depthBmp.height
                            depthBmp.recycle()
                            bmp.recycle()
                        }
                        val progress = 0.15f + ((i + 1).toFloat() / totalInFrames.toFloat()) * 0.70f
                        onProgress(progress, "Depth map frame ${i + 1} of $totalInFrames")
                    }
                }

                AiToolType.UPSCALE -> {
                    val scale = request.upscaleConfig.scale
                    finalWidth = origWidth * scale
                    finalHeight = origHeight * scale
                    onProgress(0.15f, "Upscaling video to ${scale}x (${finalWidth}x${finalHeight})...")

                    frameFiles.forEachIndexed { i, frameFile ->
                        val bmp = BitmapFactory.decodeFile(frameFile.absolutePath)
                        if (bmp != null) {
                            val upscaledBmp = upscaleEngine.processFrame(bmp, request.upscaleConfig)
                            val outFile = File(outFramesDir, String.format(Locale.US, "frame_%06d.jpg", outFrameIndex++))
                            saveBitmap(upscaledBmp, outFile)
                            upscaledBmp.recycle()
                            bmp.recycle()
                        }
                        val progress = 0.15f + ((i + 1).toFloat() / totalInFrames.toFloat()) * 0.70f
                        onProgress(progress, "Upscaling frame ${i + 1} of $totalInFrames (${scale}x)")
                    }
                }

                AiToolType.INTERPOLATION -> {
                    val multiplier = request.interpolationConfig.multiplier
                    val calculatedFps = baseFps * multiplier.toFloat()
                    targetFps = minOf(MAX_MOBILE_FPS, calculatedFps)
                    onProgress(0.15f, "Interpolating frames ${multiplier}x (${baseFps.toInt()}fps -> ${targetFps.toInt()}fps)...")

                    for (i in 0 until totalInFrames) {
                        val currentBmp = BitmapFactory.decodeFile(frameFiles[i].absolutePath)
                        if (currentBmp == null) continue

                        // Save current original frame
                        val outFile = File(outFramesDir, String.format(Locale.US, "frame_%06d.jpg", outFrameIndex++))
                        saveBitmap(currentBmp, outFile)
                        finalWidth = currentBmp.width
                        finalHeight = currentBmp.height

                        // Generate (multiplier - 1) intermediate frames before next frame
                        if (i < totalInFrames - 1) {
                            val nextBmp = BitmapFactory.decodeFile(frameFiles[i + 1].absolutePath)
                            if (nextBmp != null) {
                                val intermediates = interpolationEngine.interpolateFrames(
                                    currentBmp,
                                    nextBmp,
                                    request.interpolationConfig
                                )
                                for (interFrame in intermediates) {
                                    val interOut = File(outFramesDir, String.format(Locale.US, "frame_%06d.jpg", outFrameIndex++))
                                    saveBitmap(interFrame, interOut)
                                    interFrame.recycle()
                                }
                                nextBmp.recycle()
                            }
                        }
                        currentBmp.recycle()

                        val progress = 0.15f + ((i + 1).toFloat() / totalInFrames.toFloat()) * 0.70f
                        onProgress(progress, "Interpolating frame ${i + 1} of $totalInFrames (${multiplier}x)")
                    }
                }
            }

            // 4. Encode processed frames into target MP4 with audio
            onProgress(0.88f, "Encoding output video...")
            outputVideoFile.parentFile?.mkdirs()
            val tempOutput = File(outputVideoFile.parentFile, "${outputVideoFile.nameWithoutExtension}_tmp.mp4")
            if (tempOutput.exists()) tempOutput.delete()

            val fpsStr = String.format(Locale.US, "%.2f", targetFps)
            val encodeArgs = arrayOf(
                "-hide_banner",
                "-framerate", fpsStr,
                "-i", File(outFramesDir, "frame_%06d.jpg").absolutePath,
                "-i", inputVideoFile.absolutePath,
                "-map", "0:v:0",
                "-map", "1:a?",
                "-c:v", "libx264",
                "-preset", "veryfast",
                "-crf", "19",
                "-pix_fmt", "yuv420p",
                "-c:a", "aac",
                "-b:a", "192k",
                "-shortest",
                "-movflags", "+faststart",
                "-y", tempOutput.absolutePath
            )

            Logger.d(TAG, "Encoding AI result: ${encodeArgs.joinToString(" ")}")
            val encodeSession = FFmpegKit.executeWithArguments(encodeArgs)

            if (!ReturnCode.isSuccess(encodeSession.returnCode) || !tempOutput.exists() || tempOutput.length() == 0L) {
                // Secondary software mpeg4 fallback encode
                Logger.w(TAG, "libx264 encode failed, falling back to mpeg4")
                val swArgs = arrayOf(
                    "-hide_banner",
                    "-framerate", fpsStr,
                    "-i", File(outFramesDir, "frame_%06d.jpg").absolutePath,
                    "-i", inputVideoFile.absolutePath,
                    "-map", "0:v:0",
                    "-map", "1:a?",
                    "-c:v", "mpeg4",
                    "-q:v", "3",
                    "-c:a", "aac",
                    "-b:a", "192k",
                    "-shortest",
                    "-movflags", "+faststart",
                    "-y", tempOutput.absolutePath
                )
                FFmpegKit.executeWithArguments(swArgs)
            }

            if (tempOutput.exists() && tempOutput.length() > 0L) {
                if (outputVideoFile.exists()) outputVideoFile.delete()
                val renamed = tempOutput.renameTo(outputVideoFile)
                if (renamed && outputVideoFile.exists()) {
                    onProgress(1.0f, "AI processing complete!")
                    return@withContext OperationResult.Success(
                        AiToolResult(
                            toolType = request.toolType,
                            originalVideo = inputVideoFile,
                            processedVideo = outputVideoFile,
                            durationMs = mediaInfo.durationMs,
                            width = finalWidth,
                            height = finalHeight,
                            fps = targetFps,
                            frameCount = outFrameIndex - 1
                        )
                    )
                }
            }

            return@withContext OperationResult.Failure(
                AppError.MergeFailed("Failed to encode AI processed video frames")
            )
        } catch (e: Exception) {
            Logger.e(TAG, "AI processing failed with exception", e)
            OperationResult.Failure(AppError.Unknown("AI processing failed: ${e.message}", e))
        } finally {
            stagingDir.deleteRecursively()
        }
    }

    private fun saveBitmap(bmp: Bitmap, file: File) {
        FileOutputStream(file).use { out ->
            bmp.compress(Bitmap.CompressFormat.JPEG, 92, out)
        }
    }
}
