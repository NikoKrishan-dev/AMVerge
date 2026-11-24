package com.amverge.android.backend.media.cutting

import com.amverge.android.backend.domain.model.Scene
import com.amverge.android.backend.domain.model.TimeRange
import com.amverge.android.backend.media.ffmpeg.MediaEngine
import com.amverge.android.core.result.OperationResult
import java.io.File
import java.util.Locale

class SceneExtractor(
    private val mediaEngine: MediaEngine
) {
    suspend fun extractSceneToStaging(
        sourceUri: String,
        scene: Scene,
        stagingDir: File,
        extension: String = "mp4",
        cropFilter: String? = null,
        muteAudio: Boolean = false
    ): OperationResult<File> {
        val outputFile = File(stagingDir, "scene_${String.format(Locale.US, "%04d", scene.index)}.$extension")
        val timeRange = TimeRange(startMs = scene.startMs, endMs = scene.endMs)
        return mediaEngine.extractScene(
            uriOrPath = sourceUri,
            timeRange = timeRange,
            outputFile = outputFile,
            cropFilter = cropFilter,
            muteAudio = muteAudio
        )
    }
}
