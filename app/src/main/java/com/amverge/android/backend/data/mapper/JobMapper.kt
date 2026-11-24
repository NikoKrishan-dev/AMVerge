package com.amverge.android.backend.data.mapper

import com.amverge.android.backend.data.database.entity.ProcessingJobEntity
import com.amverge.android.backend.domain.model.JobStatus
import com.amverge.android.backend.domain.model.JobType
import com.amverge.android.backend.domain.model.ProcessingJob
import com.amverge.android.backend.domain.model.ProcessingStage

fun ProcessingJobEntity.toDomain(): ProcessingJob = ProcessingJob(
    id = id,
    projectId = projectId,
    type = runCatching { JobType.valueOf(type) }.getOrDefault(JobType.IMPORT),
    status = runCatching { JobStatus.valueOf(status) }.getOrDefault(JobStatus.FAILED),
    stage = runCatching { ProcessingStage.valueOf(stage) }.getOrDefault(ProcessingStage.PROBING),
    progress = progress,
    message = message,
    errorCode = errorCode,
    errorMessage = errorMessage,
    workName = workName,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun ProcessingJob.toEntity(): ProcessingJobEntity = ProcessingJobEntity(
    id = id,
    projectId = projectId,
    type = type.name,
    status = status.name,
    stage = stage.name,
    progress = progress,
    message = message,
    errorCode = errorCode,
    errorMessage = errorMessage,
    workName = workName,
    createdAt = createdAt,
    updatedAt = updatedAt
)
