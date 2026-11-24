package com.amverge.android.backend.domain.model

data class VideoPacketInfo(
    val ptsUs: Long?,
    val dtsUs: Long?,
    val isKeyframe: Boolean,
    val demuxIndex: Long
)

data class MediaInfo(
    val durationMs: Long,
    val width: Int?,
    val height: Int?,
    val videoCodec: String?,
    val audioCodec: String?,
    val container: String?,
    val frameRate: Double? = null,
    val bitRate: Long? = null
)
