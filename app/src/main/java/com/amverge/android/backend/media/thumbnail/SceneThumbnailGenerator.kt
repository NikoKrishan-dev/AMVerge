package com.amverge.android.backend.media.thumbnail

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import com.amverge.android.backend.domain.model.ThumbnailStatus
import com.amverge.android.backend.domain.repository.SceneRepository
import com.amverge.android.backend.media.ffmpeg.MediaEngine
import com.amverge.android.core.logging.Logger
import com.amverge.android.core.result.OperationResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class SceneThumbnailGenerator(
    private val context: Context,
    private val mediaEngine: MediaEngine,
    private val sceneRepository: SceneRepository
) {
    companion object {
        private const val TAG = "ThumbnailGenerator"
        private const val TARGET_POSTER_WIDTH = 480
    }

    private fun getThumbnailDir(projectId: String): File {
        val dir = File(context.filesDir, "thumbnails/$projectId")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    suspend fun generateThumbnailsForProject(
        projectId: String,
        sourceUri: String,
        onProgress: (done: Int, total: Int) -> Unit = { _, _ -> }
    ): OperationResult<Unit> = withContext(Dispatchers.IO) {
        val scenes = sceneRepository.getScenes(projectId)
        if (scenes.isEmpty()) {
            return@withContext OperationResult.Success(Unit)
        }

        val targetDir = getThumbnailDir(projectId)

        // Filter scenes that don't have an existing valid thumbnail on disk
        val pendingScenes = scenes.filter { scene ->
            val path = scene.thumbnailPath
            scene.thumbnailStatus != ThumbnailStatus.READY || path == null || !File(path).exists()
        }

        val total = pendingScenes.size
        if (total == 0) {
            return@withContext OperationResult.Success(Unit)
        }

        Logger.i(TAG, "Starting thumbnail generation for $total scenes in project $projectId")

        var retriever: MediaMetadataRetriever? = null
        var retrieverAvailable = false

        try {
            val mr = MediaMetadataRetriever()
            if (sourceUri.startsWith("content://")) {
                mr.setDataSource(context, Uri.parse(sourceUri))
            } else if (sourceUri.startsWith("file://")) {
                mr.setDataSource(Uri.parse(sourceUri).path)
            } else {
                mr.setDataSource(sourceUri)
            }
            retriever = mr
            retrieverAvailable = true
            Logger.i(TAG, "MediaMetadataRetriever successfully initialized for $sourceUri")
        } catch (e: Exception) {
            Logger.w(TAG, "MediaMetadataRetriever initialization failed, will use FFmpeg fallback", e)
        }

        try {
            for ((index, scene) in pendingScenes.withIndex()) {
                val outputFile = File(targetDir, "thumb_${scene.index}.jpg")
                val tempOutput = File(targetDir, "thumb_${scene.index}_tmp.jpg")
                val seekMs = scene.startMs + minOf(50L, scene.durationMs / 2)

                sceneRepository.updateThumbnailStatus(scene.id, ThumbnailStatus.RUNNING)
                var success = false

                // Primary path: Hardware-accelerated frame decode via MediaMetadataRetriever
                if (retrieverAvailable && retriever != null) {
                    try {
                        val seekUs = seekMs * 1000L
                        val targetWidth = TARGET_POSTER_WIDTH
                        val targetHeight = 270

                        var frame: Bitmap? = retriever.getScaledFrameAtTime(
                            seekUs,
                            MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
                            targetWidth,
                            targetHeight
                        )
                        if (frame == null) {
                            frame = retriever.getFrameAtTime(seekUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                        }
                        if (frame == null) {
                            frame = retriever.getScaledFrameAtTime(
                                seekUs,
                                MediaMetadataRetriever.OPTION_CLOSEST,
                                targetWidth,
                                targetHeight
                            )
                        }

                        if (frame != null) {
                            val scaledBitmap = if (frame.width != targetWidth) {
                                val calcHeight = (targetWidth * (frame.height.toFloat() / frame.width.toFloat())).toInt().coerceAtLeast(1)
                                Bitmap.createScaledBitmap(frame, targetWidth, calcHeight, true)
                            } else frame

                            if (tempOutput.exists()) tempOutput.delete()

                            FileOutputStream(tempOutput).use { outStream ->
                                scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 85, outStream)
                            }

                            if (outputFile.exists()) outputFile.delete()
                            val promoted = tempOutput.renameTo(outputFile)
                            if (promoted && outputFile.exists() && outputFile.length() > 0) {
                                success = true
                            }
                        }
                    } catch (e: Exception) {
                        Logger.w(TAG, "Retriever frame extraction failed for scene ${scene.index}", e)
                    }
                }

                // Fallback path: FFmpegKit extractPoster
                if (!success) {
                    val ffmpegResult = mediaEngine.extractPoster(
                        uriOrPath = sourceUri,
                        atMs = seekMs,
                        outputFile = outputFile,
                        targetWidth = TARGET_POSTER_WIDTH
                    )
                    success = ffmpegResult.isSuccess
                }

                if (success) {
                    sceneRepository.updateThumbnail(
                        sceneId = scene.id,
                        thumbnailPath = outputFile.absolutePath,
                        status = ThumbnailStatus.READY
                    )
                } else {
                    Logger.e(TAG, "Failed both retriever and FFmpeg poster extraction for scene ${scene.index}")
                    sceneRepository.updateThumbnailStatus(scene.id, ThumbnailStatus.FAILED)
                }

                onProgress(index + 1, total)
            }
        } finally {
            try {
                retriever?.release()
            } catch (e: Exception) {
                Logger.w(TAG, "Error releasing MediaMetadataRetriever", e)
            }
        }

        OperationResult.Success(Unit)
    }
}
