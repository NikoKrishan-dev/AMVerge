package com.amverge.android.backend.data.repository

import com.amverge.android.backend.data.database.dao.ProcessingJobDao
import com.amverge.android.backend.data.mapper.toDomain
import com.amverge.android.backend.data.mapper.toEntity
import com.amverge.android.backend.domain.model.JobStatus
import com.amverge.android.backend.domain.model.ProcessingJob
import com.amverge.android.backend.domain.model.ProcessingStage
import com.amverge.android.backend.domain.repository.JobRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class JobRepositoryImpl(
    private val jobDao: ProcessingJobDao
) : JobRepository {

    override fun observeJob(jobId: String): Flow<ProcessingJob?> {
        return jobDao.observeJob(jobId).map { it?.toDomain() }
    }

    override fun observeLatestJobForProject(projectId: String): Flow<ProcessingJob?> {
        return jobDao.observeLatestJobForProject(projectId).map { it?.toDomain() }
    }

    override suspend fun getJob(jobId: String): ProcessingJob? {
        return jobDao.getJob(jobId)?.toDomain()
    }

    override suspend fun saveJob(job: ProcessingJob) {
        jobDao.insert(job.toEntity())
    }

    override suspend fun updateProgress(
        jobId: String,
        stage: ProcessingStage,
        progress: Float,
        message: String?
    ) {
        jobDao.updateProgress(jobId, stage.name, progress, message)
    }

    override suspend fun updateStatus(
        jobId: String,
        status: JobStatus,
        errorCode: String?,
        errorMessage: String?
    ) {
        jobDao.updateStatus(jobId, status.name, errorCode, errorMessage)
    }
}
