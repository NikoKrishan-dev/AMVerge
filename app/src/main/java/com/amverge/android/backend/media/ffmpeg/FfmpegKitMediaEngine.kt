package com.amverge.android.backend.media.ffmpeg

import android.content.Context
import android.net.Uri
import com.amverge.android.backend.domain.model.MediaInfo
import com.amverge.android.backend.domain.model.TimeRange
import com.amverge.android.backend.domain.model.VideoPacketInfo
import com.amverge.android.core.common.MediaTime
import com.amverge.android.core.logging.Logger
import com.amverge.android.core.result.AppError
import com.amverge.android.core.result.OperationResult
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFmpegKitConfig
import com.arthenica.ffmpegkit.FFprobeKit
import com.arthenica.ffmpegkit.ReturnCode
import com.arthenica.ffmpegkit.Session
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

class FfmpegKitMediaEngine(
    private val context: Context
) : MediaEngine {

    companion object {
        private const val TAG = "FfmpegKitMediaEngine"
    }

    private fun resolveInputPath(uriOrPath: String): String {
        return if (uriOrPath.startsWith("content://")) {
            val uri = Uri.parse(uriOrPath)
            FFmpegKitConfig.getSafParameterForRead(context, uri)
        } else if (uriOrPath.startsWith("file://")) {
            Uri.parse(uriOrPath).path ?: uriOrPath
        } else {
            uriOrPath
        }
    }

    private fun extractCleanErrorMessage(session: Session, defaultMessage: String): String {
        val raw = session.failStackTrace?.takeIf { it.isNotBlank() }
            ?: session.output?.takeIf { it.isNotBlank() }
            ?: session.allLogsAsString?.takeIf { it.isNotBlank() }
            ?: return defaultMessage

        val lines = raw.lineSequence()
            .map { it.trim() }
            .filter { line ->
                line.isNotEmpty() &&
                !line.startsWith("ffmpeg version", ignoreCase = true) &&
                !line.startsWith("built with", ignoreCase = true) &&
                !line.startsWith("configuration:", ignoreCase = true) &&
                !line.startsWith("--") &&
                !line.startsWith("libav", ignoreCase = true) &&
                !line.startsWith("libsw", ignoreCase = true)
            }
            .toList()

        return if (lines.isNotEmpty()) {
            lines.takeLast(4).joinToString("\n")
        } else {
            defaultMessage
        }
    }

    override suspend fun probe(uriOrPath: String): OperationResult<MediaInfo> = withContext(Dispatchers.IO) {
        val resolvedInput = resolveInputPath(uriOrPath)
        Logger.d(TAG, "Probing media: $resolvedInput")

        val mediaInfoSession = FFprobeKit.getMediaInformation(resolvedInput)
        if (!ReturnCode.isSuccess(mediaInfoSession.returnCode)) {
            val errorMsg = extractCleanErrorMessage(mediaInfoSession, "Failed to probe media file")
            Logger.e(TAG, "Media probe failed: $errorMsg")
            return@withContext OperationResult.Failure(
                AppError.NoVideoStream("Failed to probe media file: $errorMsg")
            )
        }

        val info = mediaInfoSession.mediaInformation
            ?: return@withContext OperationResult.Failure(
                AppError.NoVideoStream("No media information returned by FFprobe")
            )

        val durationSec = info.duration?.toDoubleOrNull() ?: 0.0
        val durationMs = (durationSec * 1000.0).toLong()
        if (durationMs <= 0) {
            return@withContext OperationResult.Failure(
                AppError.InvalidMediaDuration("Invalid media duration reported by FFprobe: $durationMs ms")
            )
        }

        val streams = info.streams ?: emptyList()
        val videoStream = streams.firstOrNull { it.type == "video" }
            ?: return@withContext OperationResult.Failure(
                AppError.NoVideoStream("No video stream found in probed media")
            )

        val audioStream = streams.firstOrNull { it.type == "audio" }

        val width = videoStream.width?.toInt()
        val height = videoStream.height?.toInt()
        val videoCodec = videoStream.codec
        val audioCodec = audioStream?.codec
        val container = info.format
        val frameRate = videoStream.realFrameRate?.let { parseFrameRate(it) }
        val bitRate = info.bitrate?.toLongOrNull()

        OperationResult.Success(
            MediaInfo(
                durationMs = durationMs,
                width = width,
                height = height,
                videoCodec = videoCodec,
                audioCodec = audioCodec,
                container = container,
                frameRate = frameRate,
                bitRate = bitRate
            )
        )
    }

    override suspend fun readVideoPackets(uriOrPath: String): OperationResult<List<VideoPacketInfo>> = withContext(Dispatchers.IO) {
        // Fast path: Android native MediaExtractor for sub-second keyframe demuxing
        val nativeKeyframes = com.amverge.android.backend.media.detection.NativeKeyframeExtractor.extractSyncKeyframes(context, uriOrPath)
        if (!nativeKeyframes.isNullOrEmpty()) {
            Logger.d(TAG, "Using native MediaExtractor keyframes: ${nativeKeyframes.size} sync points")
            return@withContext OperationResult.Success(nativeKeyframes)
        }

        // Fallback path: FFprobe with -skip_frame nokey for accelerated scan
        val resolvedInput = resolveInputPath(uriOrPath)
        Logger.d(TAG, "Falling back to FFprobe packet reading for: $resolvedInput")

        val args = arrayOf(
            "-v", "error",
            "-skip_frame", "nokey",
            "-select_streams", "v:0",
            "-show_packets",
            "-show_entries", "packet=pts_time,dts_time,flags",
            "-of", "csv=p=0",
            resolvedInput
        )
        val session = FFprobeKit.executeWithArguments(args)

        if (!ReturnCode.isSuccess(session.returnCode)) {
            val errorMsg = extractCleanErrorMessage(session, "Failed to read packets with FFprobe")
            Logger.e(TAG, "Failed to read packets: $errorMsg")
            return@withContext OperationResult.Failure(
                AppError.DetectionFailed("Failed to read video packets with FFprobe: $errorMsg")
            )
        }

        val packets = FfprobePacketReader.parsePacketsCsv(session.output ?: "")
        if (packets.isEmpty()) {
            return@withContext OperationResult.Failure(
                AppError.NoVideoStream("No packets extracted for stream v:0")
            )
        }

        Logger.d(TAG, "Extracted ${packets.size} packets for scene detection via FFprobe")
        OperationResult.Success(packets)
    }

    override suspend fun extractPoster(
        uriOrPath: String,
        atMs: Long,
        outputFile: File,
        targetWidth: Int
    ): OperationResult<File> = withContext(Dispatchers.IO) {
        val resolvedInput = resolveInputPath(uriOrPath)
        val seekSec = MediaTime.toSeconds(atMs)

        outputFile.parentFile?.mkdirs()
        // Ensure temp file keeps the .jpg extension so FFmpeg recognizes image2 muxer
        val tempOutput = File(outputFile.parentFile, "${outputFile.nameWithoutExtension}_tmp.jpg")
        if (tempOutput.exists()) tempOutput.delete()

        val seekStr = String.format(Locale.US, "%.3f", seekSec)
        val scaleArg = "scale=$targetWidth:-2"

        val args = arrayOf(
            "-hide_banner",
            "-ss", seekStr,
            "-i", resolvedInput,
            "-vframes", "1",
            "-vf", scaleArg,
            "-f", "image2",
            "-q:v", "3",
            "-y", tempOutput.absolutePath
        )

        Logger.d(TAG, "Extracting poster with arguments: ${args.joinToString(" ")}")
        val session = FFmpegKit.executeWithArguments(args)

        if (ReturnCode.isSuccess(session.returnCode) && tempOutput.exists() && tempOutput.length() > 0) {
            if (outputFile.exists()) outputFile.delete()
            val renamed = tempOutput.renameTo(outputFile)
            if (renamed && outputFile.exists()) {
                return@withContext OperationResult.Success(outputFile)
            }
        }

        tempOutput.delete()
        val errorMsg = extractCleanErrorMessage(session, "Failed to generate thumbnail at ${atMs}ms")
        Logger.e(TAG, "Poster extraction failed: $errorMsg")
        OperationResult.Failure(AppError.ThumbnailFailed(errorMsg))
    }

    override suspend fun extractScene(
        uriOrPath: String,
        timeRange: TimeRange,
        outputFile: File,
        cropFilter: String?,
        muteAudio: Boolean
    ): OperationResult<File> = withContext(Dispatchers.IO) {
        val resolvedInput = resolveInputPath(uriOrPath)
        val startSec = MediaTime.toSeconds(timeRange.startMs)
        val durationSec = MediaTime.toSeconds(timeRange.durationMs)

        outputFile.parentFile?.mkdirs()
        val ext = outputFile.extension.ifEmpty { "mp4" }
        // Keep correct extension on temp output so FFmpeg can determine output muxer
        val tempOutput = File(outputFile.parentFile, "${outputFile.nameWithoutExtension}_tmp.$ext")
        if (tempOutput.exists()) tempOutput.delete()

        val startStr = String.format(Locale.US, "%.3f", startSec)
        val durationStr = String.format(Locale.US, "%.3f", durationSec)

        val vf = if (!cropFilter.isNullOrBlank()) {
            "setpts=PTS-STARTPTS,$cropFilter,format=yuv420p"
        } else {
            "setpts=PTS-STARTPTS,format=yuv420p"
        }

        val audioMapping = if (muteAudio) {
            listOf("-an")
        } else {
            listOf("-map", "0:a?", "-c:a", "aac", "-b:a", "192k")
        }

        // Attempt 1: Frame-accurate H.264 encode using libx264
        val x264List = mutableListOf(
            "-hide_banner",
            "-ss", startStr,
            "-i", resolvedInput,
            "-t", durationStr,
            "-map", "0:v:0"
        ).apply {
            addAll(audioMapping)
            add("-vf")
            add(vf)
            add("-c:v")
            add("libx264")
            add("-preset")
            add("ultrafast")
            add("-crf")
            add("18")
            add("-avoid_negative_ts")
            add("make_zero")
            add("-movflags")
            add("+faststart")
            add("-y")
            add(tempOutput.absolutePath)
        }
        val x264Args = x264List.toTypedArray()
        Logger.d(TAG, "Extracting scene (libx264 transcode): ${x264Args.joinToString(" ")}")
        var session = FFmpegKit.executeWithArguments(x264Args)

        if (ReturnCode.isSuccess(session.returnCode) && tempOutput.exists() && tempOutput.length() > 0) {
            if (outputFile.exists()) outputFile.delete()
            val renamed = tempOutput.renameTo(outputFile)
            if (renamed && outputFile.exists()) {
                return@withContext OperationResult.Success(outputFile)
            }
        }

        // Attempt 2: Universal software MPEG-4 transcode (frame-accurate software fallback)
        Logger.w(TAG, "libx264 transcode failed, trying universal software mpeg4 transcode")
        if (tempOutput.exists()) tempOutput.delete()
        val swList = mutableListOf(
            "-hide_banner",
            "-ss", startStr,
            "-i", resolvedInput,
            "-t", durationStr,
            "-map", "0:v:0"
        ).apply {
            addAll(audioMapping)
            add("-vf")
            add(vf)
            add("-c:v")
            add("mpeg4")
            add("-q:v")
            add("3")
            add("-avoid_negative_ts")
            add("make_zero")
            add("-movflags")
            add("+faststart")
            add("-y")
            add(tempOutput.absolutePath)
        }
        val swArgs = swList.toTypedArray()
        session = FFmpegKit.executeWithArguments(swArgs)

        if (ReturnCode.isSuccess(session.returnCode) && tempOutput.exists() && tempOutput.length() > 0) {
            if (outputFile.exists()) outputFile.delete()
            val renamed = tempOutput.renameTo(outputFile)
            if (renamed && outputFile.exists()) {
                return@withContext OperationResult.Success(outputFile)
            }
        }

        // Attempt 3: Direct stream copy fallback (only if no cropping is requested)
        if (cropFilter.isNullOrBlank()) {
            Logger.w(TAG, "Software transcode failed, attempting stream-copy fallback")
            if (tempOutput.exists()) tempOutput.delete()
            val copyList = mutableListOf(
                "-hide_banner",
                "-ss", startStr,
                "-i", resolvedInput,
                "-t", durationStr,
                "-map", "0:v:0"
            ).apply {
                if (muteAudio) {
                    add("-an")
                } else {
                    add("-map")
                    add("0:a?")
                    add("-c:a")
                    add("copy")
                }
                add("-c:v")
                add("copy")
                add("-avoid_negative_ts")
                add("make_zero")
                add("-movflags")
                add("+faststart")
                add("-y")
                add(tempOutput.absolutePath)
            }
            val copyArgs = copyList.toTypedArray()
            session = FFmpegKit.executeWithArguments(copyArgs)

            if (ReturnCode.isSuccess(session.returnCode) && tempOutput.exists() && tempOutput.length() > 0) {
                if (outputFile.exists()) outputFile.delete()
                val renamed = tempOutput.renameTo(outputFile)
                if (renamed && outputFile.exists()) {
                    return@withContext OperationResult.Success(outputFile)
                }
            }
        }

        tempOutput.delete()
        val errorMsg = extractCleanErrorMessage(session, "Failed to extract scene [${timeRange.startMs}, ${timeRange.endMs}]")
        Logger.e(TAG, "Scene extraction failed: $errorMsg")
        OperationResult.Failure(AppError.MergeFailed(errorMsg))
    }

    override suspend fun concatScenes(
        inputFiles: List<File>,
        outputFile: File
    ): OperationResult<File> = withContext(Dispatchers.IO) {
        if (inputFiles.isEmpty()) {
            return@withContext OperationResult.Failure(AppError.MergeFailed("No input files provided for concat"))
        }

        outputFile.parentFile?.mkdirs()

        // Fast path for 1 clip: directly copy to destination without running concat demuxer
        if (inputFiles.size == 1) {
            val singleInput = inputFiles[0]
            if (outputFile.exists()) outputFile.delete()
            singleInput.copyTo(outputFile, overwrite = true)
            return@withContext if (outputFile.exists() && outputFile.length() > 0) {
                OperationResult.Success(outputFile)
            } else {
                OperationResult.Failure(AppError.MergeFailed("Failed to copy single clip to output"))
            }
        }

        val ext = outputFile.extension.ifEmpty { "mp4" }
        val tempOutput = File(outputFile.parentFile, "${outputFile.nameWithoutExtension}_tmp.$ext")
        if (tempOutput.exists()) tempOutput.delete()

        val concatListFile = File(outputFile.parentFile, "concat_manifest_${System.currentTimeMillis()}.txt")
        val manifestContent = buildString {
            for (file in inputFiles) {
                val escapedPath = file.absolutePath.replace("'", "'\\''")
                appendLine("file '$escapedPath'")
            }
        }
        concatListFile.writeText(manifestContent)

        val args = arrayOf(
            "-hide_banner",
            "-f", "concat",
            "-safe", "0",
            "-i", concatListFile.absolutePath,
            "-c", "copy",
            "-movflags", "+faststart",
            "-y", tempOutput.absolutePath
        )
        Logger.d(TAG, "Concatenating scenes: ${args.joinToString(" ")}")

        val session = FFmpegKit.executeWithArguments(args)
        concatListFile.delete()

        if (ReturnCode.isSuccess(session.returnCode) && tempOutput.exists() && tempOutput.length() > 0) {
            if (outputFile.exists()) outputFile.delete()
            val renamed = tempOutput.renameTo(outputFile)
            if (renamed && outputFile.exists()) {
                return@withContext OperationResult.Success(outputFile)
            }
        }

        tempOutput.delete()
        val errorMsg = extractCleanErrorMessage(session, "Failed to concatenate clips")
        Logger.e(TAG, "Concat scenes failed: $errorMsg")
        OperationResult.Failure(AppError.MergeFailed(errorMsg))
    }

    override suspend fun verifyOutput(file: File): OperationResult<MediaInfo> = withContext(Dispatchers.IO) {
        if (!file.exists()) {
            return@withContext OperationResult.Failure(
                AppError.OutputVerificationFailed("Output file does not exist: ${file.name}")
            )
        }

        if (file.length() <= 0) {
            return@withContext OperationResult.Failure(
                AppError.OutputVerificationFailed("Output file is empty: ${file.name}")
            )
        }

        probe(file.absolutePath)
    }

    override fun cancelSession(sessionId: Long) {
        FFmpegKit.cancel(sessionId)
    }

    private fun parseFrameRate(rateStr: String): Double? {
        val parts = rateStr.split('/')
        return if (parts.size == 2) {
            val num = parts[0].toDoubleOrNull() ?: return null
            val den = parts[1].toDoubleOrNull() ?: return null
            if (den > 0) num / den else null
        } else {
            rateStr.toDoubleOrNull()
        }
    }
}
