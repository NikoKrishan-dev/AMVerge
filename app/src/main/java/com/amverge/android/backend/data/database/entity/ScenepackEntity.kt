package com.amverge.android.backend.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scenepacks")
data class ScenepackEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val description: String?,
    val createdAt: Long
)
