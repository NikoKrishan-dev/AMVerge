package com.amverge.android.backend.media.detection

import com.amverge.android.backend.domain.model.VideoPacketInfo

object OpenGopBoundaryFilter {

    /**
     * Identifies keyframes that are safe cut points.
     * In an open GOP, an I-frame (keyframe) may be followed in demux/decode order by leading
     * B-frames whose presentation timestamp (PTS) is earlier than the keyframe's PTS.
     * Cutting at an open-GOP keyframe with leading pictures causes decode errors because
     * those leading frames depend on reference pictures in the preceding GOP.
     *
     * @param packets Stream packets in demux/decode order.
     * @param maxLookahead Maximum number of subsequent packets to inspect for leading pictures.
     * @return List of safe keyframe packets (in demux order).
     */
    fun filterSafeKeyframes(
        packets: List<VideoPacketInfo>,
        maxLookahead: Int = 32
    ): List<VideoPacketInfo> {
        if (packets.isEmpty()) return emptyList()

        val safeKeyframes = mutableListOf<VideoPacketInfo>()
        val totalPackets = packets.size

        for (i in 0 until totalPackets) {
            val packet = packets[i]
            if (!packet.isKeyframe) continue

            val keyPts = packet.ptsUs ?: continue

            // First packet in stream is always a safe starting keyframe
            if (i == 0) {
                safeKeyframes.add(packet)
                continue
            }

            // Inspect following packets up to next keyframe or maxLookahead
            var hasLeadingPicture = false
            val lookaheadEnd = minOf(totalPackets, i + 1 + maxLookahead)

            for (j in (i + 1) until lookaheadEnd) {
                val nextPacket = packets[j]
                if (nextPacket.isKeyframe) break // Reached next GOP

                val nextPts = nextPacket.ptsUs
                if (nextPts != null && nextPts < keyPts) {
                    hasLeadingPicture = true
                    break
                }
            }

            if (!hasLeadingPicture) {
                safeKeyframes.add(packet)
            }
        }

        return safeKeyframes
    }
}
