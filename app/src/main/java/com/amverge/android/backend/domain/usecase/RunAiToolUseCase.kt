package com.amverge.android.backend.domain.usecase

import android.content.Context
import com.amverge.android.backend.ai.AiProcessingEngine
import com.amverge.android.backend.domain.model.JobStatus
import com.amverge.android.backend.domain.model.JobType
import com.amverge.android.backend.domain.model.ProcessingJob
import com.amverge.android.backend.domain.model.ProcessingStage
import com.amverge.android.backend.domain.model.ai.AiToolRequest
import com.amverge.android.backend.domain.model.ai.AiToolResult
import com.amverge.android.backend.domain.model.ai.AiToolType
import com.amverge.android.backend.domain.repository.JobRepository
import com.amverge.android.core.logging.Logger
import com.amverge.android.core.result.AppError
import com.amverge.android.core.result.OperationResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

class RunAiToolUseCase(
    private val context: Context,
    private val jobRepository: JobRepository,
    private val aiProcessingEngine: AiProcessingEngine
) {
    companion object {
        private const val TAG = "RunAiToolUseCase"
    }

    suspend operator fun invoke(
        request: AiToolRequest,
        onProgress: suspend (stage: ProcessingStage, progress: Float, message: String) -> Unit = { _, _, _ -> }
    ): OperationResult<AiToolResult> = withContext(Dispatchers.IO) {
        val jobId = UUID.randomUUID().toString()
        val job = ProcessingJob(
            id = jobId,
            projectId = "ai_tool_${request.clipName}",
            type = JobType.AI_TOOL,
            status = JobStatus.RUNNING,
            stage = ProcessingStage.AI_PREPARING,
            progress = 0.05f,
            message = "Preparing AI tool pass..."
        )
        jobRepository.saveJob(job)

        val inputVideoFile = File(request.sourcePathOrUri)
        if (!inputVideoFile.exists()) {
            val errorMsg = "Input clip does not exist on disk: ${inputVideoFile.name}"
            jobRepository.updateStatus(jobId, JobStatus.FAILED, "SOURCE_NOT_FOUND", errorMsg)
            return@withContext OperationResult.Failure(AppError.SourceNotFound(errorMsg))
        }

        val suffix = when (request.toolType) {
            AiToolType.INTERPOLATION -> "_${request.interpolationConfig.multiplier}x_interpolated"
            AiToolType.UPSCALE -> "_${request.upscaleConfig.scale}x_upscaled"
            AiToolType.DEPTH_MAP -> "_depth_${request.depthConfig.colormap.name.lowercase()}"
        }

        val outDir = request.targetDirectory ?: File(context.cacheDir, "ai_results").apply { mkdirs() }
        val outFileName = "${inputVideoFile.nameWithoutExtension}$suffix.mp4"
        val outputVideoFile = File(outDir, outFileName)

        try {
            val result = aiProcessingEngine.executeAiTool(
                request = request,
                inputVideoFile = inputVideoFile,
                outputVideoFile = outputVideoFile,
                onProgress = { progress, message ->
                    val stage = if (progress < 0.15f) {
                        ProcessingStage.AI_PREPARING
                    } else if (progress < 0.88f) {
                        ProcessingStage.AI_INFERENCE
                    } else {
                        ProcessingStage.AI_ENCODING
                    }
                    jobRepository.updateProgress(jobId, stage, progress, message)
                    onProgress(stage, progress, message)
                }
            )

            when (result) {
                is OperationResult.Success -> {
                    jobRepository.updateStatus(jobId, JobStatus.SUCCEEDED)
                    result
                }
                is OperationResult.Failure -> {
                    val cleanError = result.error.message.take(120)
                    jobRepository.updateStatus(jobId, JobStatus.FAILED, result.error.code, cleanError)
                    result
                }
            }
        } catch (e: Exception) {
            Logger.e(TAG, "RunAiToolUseCase unexpected error", e)
            val cleanError = e.message?.take(120) ?: "AI tool processing failed"
            jobRepository.updateStatus(jobId, JobStatus.FAILED, "AI_ERROR", cleanError)
            OperationResult.Failure(AppError.Unknown(cleanError, e))
        }
    }
}
