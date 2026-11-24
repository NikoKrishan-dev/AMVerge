package com.amverge.android.backend.media.merging

import com.amverge.android.backend.media.ffmpeg.MediaEngine
import com.amverge.android.core.result.OperationResult
import java.io.File

class SceneConcatenator(
    private val mediaEngine: MediaEngine
) {
    suspend fun concatScenes(
        stagedFiles: List<File>,
        finalOutputFile: File
    ): OperationResult<File> {
        return mediaEngine.concatScenes(
            inputFiles = stagedFiles,
            outputFile = finalOutputFile
        )
    }
}
