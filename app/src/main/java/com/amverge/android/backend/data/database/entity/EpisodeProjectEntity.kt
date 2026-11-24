package com.amverge.android.backend.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "episode_projects")
data class EpisodeProjectEntity(
    @PrimaryKey
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
    val status: String,
    val seriesId: String? = null,
    val episodeNumber: Int? = null,
    val createdAt: Long,
    val updatedAt: Long
)
