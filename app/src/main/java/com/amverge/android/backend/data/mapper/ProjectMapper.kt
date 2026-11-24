package com.amverge.android.backend.data.mapper

import com.amverge.android.backend.data.database.entity.EpisodeProjectEntity
import com.amverge.android.backend.domain.model.EpisodeProject
import com.amverge.android.backend.domain.model.ProjectStatus

fun EpisodeProjectEntity.toDomain(): EpisodeProject = EpisodeProject(
    id = id,
    displayName = displayName,
    sourceUri = sourceUri,
    persistedReadFlags = persistedReadFlags,
    durationMs = durationMs,
    width = width,
    height = height,
    videoCodec = videoCodec,
    audioCodec = audioCodec,
    container = container,
    status = runCatching { ProjectStatus.valueOf(status) }.getOrDefault(ProjectStatus.FAILED),
    seriesId = seriesId,
    episodeNumber = episodeNumber,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun EpisodeProject.toEntity(): EpisodeProjectEntity = EpisodeProjectEntity(
    id = id,
    displayName = displayName,
    sourceUri = sourceUri,
    persistedReadFlags = persistedReadFlags,
    durationMs = durationMs,
    width = width,
    height = height,
    videoCodec = videoCodec,
    audioCodec = audioCodec,
    container = container,
    status = status.name,
    seriesId = seriesId,
    episodeNumber = episodeNumber,
    createdAt = createdAt,
    updatedAt = updatedAt
)
