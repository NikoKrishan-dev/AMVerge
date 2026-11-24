package com.amverge.android.backend.media.ffmpeg

import com.amverge.android.backend.domain.model.MediaInfo
import com.amverge.android.backend.domain.model.TimeRange
import com.amverge.android.backend.domain.model.VideoPacketInfo
import com.amverge.android.core.result.OperationResult
import java.io.File

interface MediaEngine {
    suspend fun probe(uriOrPath: String): OperationResult<MediaInfo>
    suspend fun readVideoPackets(uriOrPath: String): OperationResult<List<VideoPacketInfo>>
    suspend fun extractPoster(
        uriOrPath: String,
        atMs: Long,
        outputFile: File,
        targetWidth: Int = 480
    ): OperationResult<File>
    suspend fun extractScene(
        uriOrPath: String,
        timeRange: TimeRange,
        outputFile: File,
        cropFilter: String? = null,
        muteAudio: Boolean = false
    ): OperationResult<File>
    suspend fun concatScenes(
        inputFiles: List<File>,
        outputFile: File
    ): OperationResult<File>
    suspend fun verifyOutput(file: File): OperationResult<MediaInfo>
    fun cancelSession(sessionId: Long)
}
