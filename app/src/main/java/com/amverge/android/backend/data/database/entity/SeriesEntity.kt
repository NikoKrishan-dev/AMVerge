package com.amverge.android.backend.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "series")
data class SeriesEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val description: String?,
    val coverUri: String?,
    val createdAt: Long
)
