package com.amverge.android.backend.data.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "processing_jobs",
    indices = [
        Index(value = ["projectId"]),
        Index(value = ["projectId", "status"])
    ]
)
data class ProcessingJobEntity(
    @PrimaryKey
    val id: String,
    val projectId: String,
    val type: String,
    val status: String,
    val stage: String,
    val progress: Float,
    val message: String?,
    val errorCode: String?,
    val errorMessage: String?,
    val workName: String?,
    val createdAt: Long,
    val updatedAt: Long
)
