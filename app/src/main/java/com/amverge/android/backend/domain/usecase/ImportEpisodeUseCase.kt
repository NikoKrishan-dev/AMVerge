package com.amverge.android.backend.domain.usecase

import com.amverge.android.backend.domain.model.DetectionPreferences
import com.amverge.android.backend.domain.model.EpisodeProject
import com.amverge.android.backend.domain.model.JobStatus
import com.amverge.android.backend.domain.model.JobType
import com.amverge.android.backend.domain.model.ProcessingJob
import com.amverge.android.backend.domain.model.ProcessingStage
import com.amverge.android.backend.domain.model.ProjectStatus
import com.amverge.android.backend.domain.repository.DetectionPreferencesRepository
import com.amverge.android.backend.domain.repository.JobRepository
import com.amverge.android.backend.domain.repository.ProjectRepository
import com.amverge.android.backend.domain.repository.SceneRepository
import com.amverge.android.backend.media.detection.KeyframeSceneDetector
import com.amverge.android.backend.media.detection.MotionVarianceEvaluator
import com.amverge.android.backend.media.detection.TransitionDetector
import com.amverge.android.backend.media.ffmpeg.MediaEngine
import com.amverge.android.backend.media.thumbnail.SceneThumbnailGenerator
import com.amverge.android.core.logging.Logger
import com.amverge.android.core.result.AppError
import com.amverge.android.core.result.OperationResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class ImportEpisodeUseCase(
    private val projectRepository: ProjectRepository,
    private val sceneRepository: SceneRepository,
    private val jobRepository: JobRepository,
    private val mediaEngine: MediaEngine,
    private val sceneDetector: KeyframeSceneDetector,
    private val thumbnailGenerator: SceneThumbnailGenerator,
    private val motionVarianceEvaluator: MotionVarianceEvaluator? = null,
    private val transitionDetector: TransitionDetector? = null,
    private val detectionPreferencesRepository: DetectionPreferencesRepository? = null
) {
    companion object {
        private const val TAG = "ImportEpisodeUseCase"
    }

    suspend operator fun invoke(
        sourceUri: String,
        displayName: String,
        persistedReadFlags: Int,
        onProgress: (stage: ProcessingStage, progress: Float, message: String?) -> Unit = { _, _, _ -> }
    ): OperationResult<EpisodeProject> = withContext(Dispatchers.IO) {
        val projectId = UUID.randomUUID().toString()
        val jobId = UUID.randomUUID().toString()

        val job = ProcessingJob(
            id = jobId,
            projectId = projectId,
            type = JobType.IMPORT,
            status = JobStatus.RUNNING,
            stage = ProcessingStage.PROBING,
            progress = 0.05f,
            message = "Probing video stream"
        )
        jobRepository.saveJob(job)
        onProgress(ProcessingStage.PROBING, 0.05f, "Probing video stream")

        // 1. Probe media
        val probeResult = mediaEngine.probe(sourceUri)
        if (probeResult is OperationResult.Failure) {
            jobRepository.updateStatus(jobId, JobStatus.FAILED, probeResult.error.code, probeResult.error.message)
            return@withContext OperationResult.Failure(probeResult.error)
        }

        val mediaInfo = (probeResult as OperationResult.Success).data
        val initialProject = EpisodeProject(
            id = projectId,
            displayName = displayName,
            sourceUri = sourceUri,
            persistedReadFlags = persistedReadFlags,
            durationMs = mediaInfo.durationMs,
            width = mediaInfo.width,
            height = mediaInfo.height,
            videoCodec = mediaInfo.videoCodec,
            audioCodec = mediaInfo.audioCodec,
            container = mediaInfo.container,
            status = ProjectStatus.DETECTING
        )
        projectRepository.saveProject(initialProject)

        // 2. Read packets
        jobRepository.updateProgress(jobId, ProcessingStage.DETECTING, 0.20f, "Scanning keyframes")
        onProgress(ProcessingStage.DETECTING, 0.20f, "Scanning keyframes")

        val packetResult = mediaEngine.readVideoPackets(sourceUri)
        if (packetResult is OperationResult.Failure) {
            projectRepository.updateStatus(projectId, ProjectStatus.FAILED)
            jobRepository.updateStatus(jobId, JobStatus.FAILED, packetResult.error.code, packetResult.error.message)
            return@withContext OperationResult.Failure(packetResult.error)
        }

        val packets = (packetResult as OperationResult.Success).data

        val prefs = detectionPreferencesRepository?.getPreferences() ?: DetectionPreferences()

        // 3. Detect scenes
        val detectionResult = sceneDetector.detectScenes(
            projectId = projectId,
            packets = packets,
            durationMs = mediaInfo.durationMs,
            minDurationMs = prefs.minSceneDurationMs
        )

        if (detectionResult is OperationResult.Failure) {
            projectRepository.updateStatus(projectId, ProjectStatus.FAILED)
            jobRepository.updateStatus(jobId, JobStatus.FAILED, detectionResult.error.code, detectionResult.error.message)
            return@withContext OperationResult.Failure(detectionResult.error)
        }

        var scenes = (detectionResult as OperationResult.Success).data
        Logger.i(TAG, "Detected ${scenes.size} initial keyframe scenes for project $projectId")

        // 3.5 Evaluate motion dynamics & deadframe detection (only when requested)
        if (prefs.filterDeadframes && motionVarianceEvaluator != null) {
            jobRepository.updateProgress(jobId, ProcessingStage.DETECTING, 0.40f, "Evaluating scene motion dynamics")
            onProgress(ProcessingStage.DETECTING, 0.40f, "Evaluating scene motion dynamics")

            val evaluatedScenes = motionVarianceEvaluator.evaluateBatch(
                sourceUri = sourceUri,
                scenes = scenes,
                deadframeThreshold = prefs.deadframeThreshold
            )

            val nonDead = evaluatedScenes.filter { !it.isDeadframe }
            scenes = if (nonDead.isNotEmpty()) nonDead else evaluatedScenes
            Logger.i(TAG, "Completed motion evaluation: ${scenes.size} scenes retained")
        }

        // 4. Save scenes in DB and immediately promote project to READY
        jobRepository.updateProgress(jobId, ProcessingStage.SAVING_SCENES, 0.90f, "Saving scenes")
        onProgress(ProcessingStage.SAVING_SCENES, 0.90f, "Saving scenes")
        sceneRepository.replaceScenes(projectId, scenes)
        projectRepository.updateStatus(projectId, ProjectStatus.READY)
        jobRepository.updateProgress(jobId, ProcessingStage.THUMBNAILS, 1.0f, "Import complete")
        jobRepository.updateStatus(jobId, JobStatus.SUCCEEDED)
        onProgress(ProcessingStage.THUMBNAILS, 1.0f, "Import complete")

        // 5. Trigger asynchronous thumbnail generation in background without blocking UI navigation
        val applicationScope = kotlinx.coroutines.CoroutineScope(Dispatchers.IO)
        applicationScope.launch {
            try {
                thumbnailGenerator.generateThumbnailsForProject(
                    projectId = projectId,
                    sourceUri = sourceUri
                )
            } catch (e: Exception) {
                Logger.w(TAG, "Background thumbnail generation encountered an issue", e)
            }
        }

        val completedProject = projectRepository.getProject(projectId)
            ?: return@withContext OperationResult.Failure(
                AppError.Unknown("Project not found after completion")
            )

        OperationResult.Success(completedProject)
    }
}
