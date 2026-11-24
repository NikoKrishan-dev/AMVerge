package com.amverge.android.backend.domain.usecase

import android.content.Context
import android.net.Uri
import com.amverge.android.backend.domain.model.AspectRatioCrop
import com.amverge.android.backend.domain.model.AudioExportOption
import com.amverge.android.backend.domain.model.ExportMode
import com.amverge.android.backend.domain.model.ExportRequest
import com.amverge.android.backend.domain.model.JobStatus
import com.amverge.android.backend.domain.model.JobType
import com.amverge.android.backend.domain.model.ProcessingJob
import com.amverge.android.backend.domain.model.ProcessingStage
import com.amverge.android.backend.domain.model.Scene
import com.amverge.android.backend.domain.repository.JobRepository
import com.amverge.android.backend.domain.repository.ProjectRepository
import com.amverge.android.backend.domain.repository.SceneRepository
import com.amverge.android.backend.media.cutting.SceneExtractor
import com.amverge.android.backend.media.merging.SceneConcatenator
import com.amverge.android.core.logging.Logger
import com.amverge.android.core.result.AppError
import com.amverge.android.core.result.OperationResult
import com.amverge.android.platform.storage.SafUriAccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale
import java.util.UUID

class ExportScenesUseCase(
    private val context: Context,
    private val projectRepository: ProjectRepository,
    private val sceneRepository: SceneRepository,
    private val jobRepository: JobRepository,
    private val sceneExtractor: SceneExtractor,
    private val sceneConcatenator: SceneConcatenator,
    private val safUriAccess: SafUriAccess
) {
    companion object {
        private const val TAG = "ExportScenesUseCase"
    }

    private fun sanitizeFileName(name: String): String {
        return name.replace(Regex("[^a-zA-Z0-9._-]"), "_").take(60)
    }

    suspend operator fun invoke(
        request: ExportRequest,
        onProgress: (stage: ProcessingStage, progress: Float, message: String?) -> Unit = { _, _, _ -> }
    ): OperationResult<List<String>> = withContext(Dispatchers.IO) {
        val project = projectRepository.getProject(request.projectId)
            ?: return@withContext OperationResult.Failure(
                AppError.SourceNotFound("Project not found: ${request.projectId}")
            )

        if (request.sceneIds.isEmpty()) {
            return@withContext OperationResult.Failure(
                AppError.Unknown("No scenes selected for export")
            )
        }

        // Strictly preserve chronological source order
        val scenes = sceneRepository.getScenesByIds(request.projectId, request.sceneIds)
            .sortedBy { it.index }

        if (scenes.isEmpty()) {
            return@withContext OperationResult.Failure(
                AppError.Unknown("Selected scenes not found in database")
            )
        }

        val jobId = UUID.randomUUID().toString()
        val job = ProcessingJob(
            id = jobId,
            projectId = request.projectId,
            type = JobType.EXPORT,
            status = JobStatus.RUNNING,
            stage = ProcessingStage.EXPORTING,
            progress = 0.05f,
            message = "Starting export"
        )
        jobRepository.saveJob(job)

        val stagingDir = File(context.cacheDir, "export_staging_${UUID.randomUUID()}")
        stagingDir.mkdirs()

        val producedOutputs = mutableListOf<String>()
        val baseName = sanitizeFileName(project.displayName.substringBeforeLast('.'))

        val cropFilter = when (request.aspectRatio) {
            AspectRatioCrop.ORIGINAL -> null
            AspectRatioCrop.VERTICAL_9_16 -> "crop=ih*(9/16):ih,scale=1080:1920"
            AspectRatioCrop.SQUARE_1_1 -> "crop=min(iw\\,ih):min(iw\\,ih),scale=1080:1080"
            AspectRatioCrop.STANDARD_4_3 -> "crop=ih*(4/3):ih,scale=1440:1080"
        }
        val muteAudio = (request.audioOption == AudioExportOption.MUTE_AUDIO)

        try {
            // When only 1 scene is selected, always treat as single-scene export unless EDL is requested
            val effectiveMode = if (scenes.size == 1 && request.mode != ExportMode.EDL_TIMELINE) {
                ExportMode.INDIVIDUAL
            } else {
                request.mode
            }

            when (effectiveMode) {
                ExportMode.EDL_TIMELINE -> {
                    onProgress(ProcessingStage.EXPORTING, 0.20f, "Generating CMX 3600 EDL timeline...")

                    val edlContent = generateEdl(
                        projectName = baseName,
                        sourceFileName = project.displayName,
                        fps = 24.0f,
                        scenes = scenes
                    )

                    val edlFileName = "${baseName}_timeline.edl"
                    val edlTempFile = File(stagingDir, edlFileName)
                    edlTempFile.writeText(edlContent, Charsets.UTF_8)

                    val docUriStr = request.destinationDocumentUri
                    val treeUriStr = request.destinationTreeUri

                    if (docUriStr != null) {
                        val docUri = Uri.parse(docUriStr)
                        val copied = safUriAccess.copyFileToDocumentUri(edlTempFile, docUri)
                        if (copied) {
                            producedOutputs.add(docUriStr)
                        }
                    } else if (treeUriStr != null) {
                        val treeUri = Uri.parse(treeUriStr)
                        val createdDoc = safUriAccess.createDocumentInTree(treeUri, "text/plain", edlFileName)
                        if (createdDoc != null) {
                            val copied = safUriAccess.copyFileToDocumentUri(edlTempFile, createdDoc)
                            if (copied) producedOutputs.add(createdDoc.toString())
                        }
                    } else {
                        val persistentDir = File(context.filesDir, "edl_exports").apply { mkdirs() }
                        val targetFile = File(persistentDir, edlFileName)
                        edlTempFile.copyTo(targetFile, overwrite = true)
                        producedOutputs.add(targetFile.absolutePath)
                    }

                    if (producedOutputs.isEmpty()) {
                        jobRepository.updateStatus(jobId, JobStatus.FAILED, "EDL_WRITE_FAILED", "Failed to write EDL timeline file")
                        return@withContext OperationResult.Failure(AppError.MergeFailed("Failed to write EDL timeline to storage"))
                    }

                    onProgress(ProcessingStage.VERIFYING, 1.0f, "EDL timeline exported successfully")
                }

                ExportMode.INDIVIDUAL -> {
                    val treeUriStr = request.destinationTreeUri
                    val treeUri = if (treeUriStr != null) Uri.parse(treeUriStr) else null
                    val docUriStr = request.destinationDocumentUri
                    val totalScenes = scenes.size

                    scenes.forEachIndexed { i, scene ->
                        val cutResult = sceneExtractor.extractSceneToStaging(
                            sourceUri = project.sourceUri,
                            scene = scene,
                            stagingDir = stagingDir,
                            cropFilter = cropFilter,
                            muteAudio = muteAudio
                        )

                        if (cutResult is OperationResult.Failure) {
                            val cleanError = cutResult.error.message.take(120)
                            jobRepository.updateStatus(jobId, JobStatus.FAILED, cutResult.error.code, cleanError)
                            return@withContext OperationResult.Failure(cutResult.error)
                        }

                        val stagedFile = (cutResult as OperationResult.Success).data
                        val cropSuffix = if (cropFilter != null) "_${request.aspectRatio.name.lowercase()}" else ""
                        val muteSuffix = if (muteAudio) "_muted" else ""
                        val outName = "${baseName}_scene_${String.format(Locale.US, "%04d", scene.index + 1)}$cropSuffix$muteSuffix.mp4"

                        if (docUriStr != null && totalScenes == 1) {
                            val docUri = Uri.parse(docUriStr)
                            val copied = safUriAccess.copyFileToDocumentUri(stagedFile, docUri)
                            if (copied) {
                                producedOutputs.add(docUriStr)
                            } else {
                                val savedUri = safUriAccess.saveVideoToSharedStorage(stagedFile, outName)
                                producedOutputs.add(savedUri?.toString() ?: stagedFile.absolutePath)
                            }
                        } else if (treeUri != null) {
                            val targetDocUri = safUriAccess.createDocumentInTree(treeUri, "video/mp4", outName)
                            if (targetDocUri != null) {
                                safUriAccess.copyFileToDocumentUri(stagedFile, targetDocUri)
                                producedOutputs.add(targetDocUri.toString())
                            } else {
                                val savedUri = safUriAccess.saveVideoToSharedStorage(stagedFile, outName)
                                producedOutputs.add(savedUri?.toString() ?: stagedFile.absolutePath)
                            }
                        } else {
                            // Default: save to Movies/AMVerge in device storage
                            val savedUri = safUriAccess.saveVideoToSharedStorage(stagedFile, outName)
                            producedOutputs.add(savedUri?.toString() ?: stagedFile.absolutePath)
                        }

                        val progress = ((i + 1).toFloat() / totalScenes.toFloat()) * 0.95f
                        val msg = if (totalScenes == 1) "Exported scene ${scene.index + 1}" else "Exported clip ${i + 1} of $totalScenes"
                        jobRepository.updateProgress(jobId, ProcessingStage.EXPORTING, progress, msg)
                        onProgress(ProcessingStage.EXPORTING, progress, msg)
                    }
                }

                ExportMode.MERGED -> {
                    val stagedClips = mutableListOf<File>()
                    val totalScenes = scenes.size

                    scenes.forEachIndexed { i, scene ->
                        val cutResult = sceneExtractor.extractSceneToStaging(
                            sourceUri = project.sourceUri,
                            scene = scene,
                            stagingDir = stagingDir,
                            cropFilter = cropFilter,
                            muteAudio = muteAudio
                        )

                        if (cutResult is OperationResult.Failure) {
                            val cleanError = cutResult.error.message.take(120)
                            jobRepository.updateStatus(jobId, JobStatus.FAILED, cutResult.error.code, cleanError)
                            return@withContext OperationResult.Failure(cutResult.error)
                        }

                        stagedClips.add((cutResult as OperationResult.Success).data)

                        val progress = ((i + 1).toFloat() / totalScenes.toFloat()) * 0.60f
                        val msg = "Cutting clip ${i + 1} of $totalScenes"
                        jobRepository.updateProgress(jobId, ProcessingStage.EXPORTING, progress, msg)
                        onProgress(ProcessingStage.EXPORTING, progress, msg)
                    }

                    // Concat staged clips
                    jobRepository.updateProgress(jobId, ProcessingStage.EXPORTING, 0.70f, "Merging clips into single video")
                    onProgress(ProcessingStage.EXPORTING, 0.70f, "Merging clips into single video")

                    val cropSuffix = if (cropFilter != null) "_${request.aspectRatio.name.lowercase()}" else ""
                    val muteSuffix = if (muteAudio) "_muted" else ""
                    val mergedStagingFile = File(stagingDir, "${baseName}_merged_${System.currentTimeMillis()}$cropSuffix$muteSuffix.mp4")
                    val concatResult = sceneConcatenator.concatScenes(stagedClips, mergedStagingFile)
                    if (concatResult is OperationResult.Failure) {
                        val cleanError = concatResult.error.message.take(120)
                        jobRepository.updateStatus(jobId, JobStatus.FAILED, concatResult.error.code, cleanError)
                        return@withContext OperationResult.Failure(concatResult.error)
                    }

                    val mergedFile = (concatResult as OperationResult.Success).data
                    val mergedOutName = "${baseName}_merged_${System.currentTimeMillis() % 100000}$cropSuffix$muteSuffix.mp4"

                    // Deliver to custom document URI if chosen by user
                    val docUriStr = request.destinationDocumentUri
                    if (docUriStr != null) {
                        val docUri = Uri.parse(docUriStr)
                        val copied = safUriAccess.copyFileToDocumentUri(mergedFile, docUri)
                        if (!copied) {
                            return@withContext OperationResult.Failure(
                                AppError.ExportDestinationUnavailable("Failed to write merged output to document URI")
                            )
                        }
                        producedOutputs.add(docUriStr)
                    } else {
                        // Default: Save directly to shared Movies/AMVerge so it appears in device Gallery and Files
                        val savedUri = safUriAccess.saveVideoToSharedStorage(mergedFile, mergedOutName)
                        if (savedUri != null) {
                            producedOutputs.add(savedUri.toString())
                        } else {
                            val permanentOutput = File(context.filesDir, "exports/$mergedOutName")
                            permanentOutput.parentFile?.mkdirs()
                            mergedFile.copyTo(permanentOutput, overwrite = true)
                            producedOutputs.add(permanentOutput.absolutePath)
                        }
                    }

                    jobRepository.updateProgress(jobId, ProcessingStage.VERIFYING, 1.0f, "Merge complete")
                    onProgress(ProcessingStage.VERIFYING, 1.0f, "Merge complete")
                }
            }

            jobRepository.updateStatus(jobId, JobStatus.SUCCEEDED)
            OperationResult.Success(producedOutputs)
        } catch (e: Exception) {
            Logger.e(TAG, "Export failed with exception", e)
            val cleanMsg = e.message?.take(120) ?: "Export failed"
            jobRepository.updateStatus(jobId, JobStatus.FAILED, "EXPORT_ERROR", cleanMsg)
            OperationResult.Failure(AppError.Unknown(cleanMsg, e))
        } finally {
            stagingDir.deleteRecursively()
        }
    }

    private fun formatTimecode(ms: Long, fps: Float): String {
        val effectiveFps = if (fps > 1.0f) fps else 24.0f
        val totalFrames = (ms * effectiveFps / 1000.0).toLong()
        val frames = (totalFrames % effectiveFps.toLong()).coerceIn(0, 99)
        val totalSeconds = totalFrames / effectiveFps.toLong()
        val seconds = totalSeconds % 60
        val totalMinutes = totalSeconds / 60
        val minutes = totalMinutes % 60
        val hours = totalMinutes / 60
        return String.format(Locale.US, "%02d:%02d:%02d:%02d", hours, minutes, seconds, frames)
    }

    private fun generateEdl(
        projectName: String,
        sourceFileName: String,
        fps: Float,
        scenes: List<Scene>
    ): String {
        val sb = StringBuilder()
        sb.appendLine("TITLE: $projectName")
        sb.appendLine("FCM: NON-DROP FRAME")
        sb.appendLine()

        var recordMs = 0L

        scenes.forEachIndexed { i, scene ->
            val eventNum = String.format(Locale.US, "%03d", i + 1)
            val srcIn = formatTimecode(scene.startMs, fps)
            val srcOut = formatTimecode(scene.endMs, fps)
            val recIn = formatTimecode(recordMs, fps)
            val recOut = formatTimecode(recordMs + scene.durationMs, fps)

            sb.appendLine("$eventNum  AX       V     C        $srcIn $srcOut $recIn $recOut")
            sb.appendLine("* FROM CLIP NAME: $sourceFileName")
            sb.appendLine()

            recordMs += scene.durationMs
        }

        return sb.toString()
    }
}
