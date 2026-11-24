package com.amverge.android.backend.domain.repository

import com.amverge.android.backend.domain.model.JobStatus
import com.amverge.android.backend.domain.model.ProcessingJob
import com.amverge.android.backend.domain.model.ProcessingStage
import kotlinx.coroutines.flow.Flow

interface JobRepository {
    fun observeJob(jobId: String): Flow<ProcessingJob?>
    fun observeLatestJobForProject(projectId: String): Flow<ProcessingJob?>
    suspend fun getJob(jobId: String): ProcessingJob?
    suspend fun saveJob(job: ProcessingJob)
    suspend fun updateProgress(jobId: String, stage: ProcessingStage, progress: Float, message: String? = null)
    suspend fun updateStatus(jobId: String, status: JobStatus, errorCode: String? = null, errorMessage: String? = null)
}
