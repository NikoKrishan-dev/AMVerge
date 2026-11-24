package com.amverge.android.backend.domain.model

enum class ProjectStatus {
    CREATED,
    PROBING,
    DETECTING,
    GENERATING_THUMBNAILS,
    READY,
    FAILED,
    SOURCE_UNAVAILABLE
}

data class EpisodeProject(
    val id: String,
    val displayName: String,
    val sourceUri: String,
    val persistedReadFlags: Int,
    val durationMs: Long,
    val width: Int?,
    val height: Int?,
    val videoCodec: String?,
    val audioCodec: String?,
    val container: String?,
    val status: ProjectStatus,
    val seriesId: String? = null,
    val episodeNumber: Int? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
