package com.amverge.android.backend.data.mapper

import com.amverge.android.backend.data.database.entity.SceneEntity
import com.amverge.android.backend.domain.model.Scene
import com.amverge.android.backend.domain.model.ThumbnailStatus

fun SceneEntity.toDomain(): Scene = Scene(
    id = id,
    projectId = projectId,
    index = sceneIndex,
    startMs = startMs,
    endMs = endMs,
    thumbnailPath = thumbnailPath,
    thumbnailStatus = runCatching { ThumbnailStatus.valueOf(thumbnailStatus) }.getOrDefault(ThumbnailStatus.FAILED),
    isFavorite = isFavorite,
    tags = if (tags.isBlank()) emptyList() else tags.split(",").map { it.trim() },
    motionScore = motionScore,
    isDeadframe = isDeadframe
)

fun Scene.toEntity(): SceneEntity = SceneEntity(
    id = id,
    projectId = projectId,
    sceneIndex = index,
    startMs = startMs,
    endMs = endMs,
    thumbnailPath = thumbnailPath,
    thumbnailStatus = thumbnailStatus.name,
    isFavorite = isFavorite,
    tags = tags.joinToString(","),
    motionScore = motionScore,
    isDeadframe = isDeadframe
)
