package com.amverge.android.backend.media.ffmpeg

import com.amverge.android.backend.domain.model.VideoPacketInfo

object FfprobePacketReader {

    /**
     * Parses ffprobe csv packet output line by line into VideoPacketInfo.
     * Expected format:
     * pts_time,dts_time,flags
     * Example:
     * 0.000000,0.000000,K__
     * 0.041708,0.041708,___
     */
    fun parsePacketsCsv(output: String): List<VideoPacketInfo> {
        if (output.isBlank()) return emptyList()

        val packets = mutableListOf<VideoPacketInfo>()
        var demuxIndex = 0L

        output.lineSequence().forEach { line ->
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("#")) return@forEach

            val parts = trimmed.split(',')
            if (parts.size >= 3) {
                val ptsTimeSec = parts[0].trim().toDoubleOrNull()
                val dtsTimeSec = parts[1].trim().toDoubleOrNull()
                val flags = parts[2].trim()

                val ptsUs = ptsTimeSec?.let { (it * 1_000_000.0).toLong() }
                val dtsUs = dtsTimeSec?.let { (it * 1_000_000.0).toLong() }
                val isKeyframe = flags.contains("K", ignoreCase = false)

                packets.add(
                    VideoPacketInfo(
                        ptsUs = ptsUs,
                        dtsUs = dtsUs,
                        isKeyframe = isKeyframe,
                        demuxIndex = demuxIndex++
                    )
                )
            }
        }

        return packets
    }
}
