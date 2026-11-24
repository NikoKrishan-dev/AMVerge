package com.amverge.android.backend.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.amverge.android.backend.data.database.entity.ProcessingJobEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProcessingJobDao {

    @Query("SELECT * FROM processing_jobs WHERE id = :jobId LIMIT 1")
    fun observeJob(jobId: String): Flow<ProcessingJobEntity?>

    @Query("SELECT * FROM processing_jobs WHERE projectId = :projectId ORDER BY createdAt DESC LIMIT 1")
    fun observeLatestJobForProject(projectId: String): Flow<ProcessingJobEntity?>

    @Query("SELECT * FROM processing_jobs WHERE id = :jobId LIMIT 1")
    suspend fun getJob(jobId: String): ProcessingJobEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(job: ProcessingJobEntity)

    @Update
    suspend fun update(job: ProcessingJobEntity)

    @Query("UPDATE processing_jobs SET stage = :stage, progress = :progress, message = :message, updatedAt = :updatedAt WHERE id = :jobId")
    suspend fun updateProgress(
        jobId: String,
        stage: String,
        progress: Float,
        message: String?,
        updatedAt: Long = System.currentTimeMillis()
    )

    @Query("UPDATE processing_jobs SET status = :status, errorCode = :errorCode, errorMessage = :errorMessage, updatedAt = :updatedAt WHERE id = :jobId")
    suspend fun updateStatus(
        jobId: String,
        status: String,
        errorCode: String?,
        errorMessage: String?,
        updatedAt: Long = System.currentTimeMillis()
    )
}
