package com.amverge.android.backend.data.mapper

import com.amverge.android.backend.data.database.entity.SeriesEntity
import com.amverge.android.backend.domain.model.Series

fun SeriesEntity.toDomain(episodeCount: Int = 0): Series = Series(
    id = id,
    title = title,
    description = description,
    coverUri = coverUri,
    episodeCount = episodeCount,
    createdAt = createdAt
)

fun Series.toEntity(): SeriesEntity = SeriesEntity(
    id = id,
    title = title,
    description = description,
    coverUri = coverUri,
    createdAt = createdAt
)
