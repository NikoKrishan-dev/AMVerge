package com.amverge.android.backend.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "scenes",
    foreignKeys = [
        ForeignKey(
            entity = EpisodeProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["projectId"]),
        Index(value = ["projectId", "sceneIndex"], unique = true)
    ]
)
data class SceneEntity(
    @PrimaryKey
    val id: String,
    val projectId: String,
    val sceneIndex: Int,
    val startMs: Long,
    val endMs: Long,
    val thumbnailPath: String?,
    val thumbnailStatus: String,
    val isFavorite: Boolean = false,
    val tags: String = "",
    val motionScore: Float = 0.5f,
    val isDeadframe: Boolean = false
)
