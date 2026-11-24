package com.amverge.android.backend.data.mapper

import com.amverge.android.backend.data.database.entity.ScenepackEntity
import com.amverge.android.backend.domain.model.Scenepack

fun ScenepackEntity.toDomain(sceneCount: Int = 0): Scenepack = Scenepack(
    id = id,
    name = name,
    description = description,
    sceneCount = sceneCount,
    createdAt = createdAt
)

fun Scenepack.toEntity(): ScenepackEntity = ScenepackEntity(
    id = id,
    name = name,
    description = description,
    createdAt = createdAt
)
