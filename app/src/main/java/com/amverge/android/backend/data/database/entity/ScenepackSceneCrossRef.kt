package com.amverge.android.backend.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "scenepack_scenes",
    primaryKeys = ["scenepackId", "sceneId"],
    foreignKeys = [
        ForeignKey(
            entity = ScenepackEntity::class,
            parentColumns = ["id"],
            childColumns = ["scenepackId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = SceneEntity::class,
            parentColumns = ["id"],
            childColumns = ["sceneId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["scenepackId"]),
        Index(value = ["sceneId"])
    ]
)
data class ScenepackSceneCrossRef(
    val scenepackId: String,
    val sceneId: String,
    val orderIndex: Int
)
