package com.amverge.android.backend.domain.usecase

import android.content.Context
import android.net.Uri
import com.amverge.android.backend.domain.repository.ProjectRepository
import com.amverge.android.backend.domain.repository.ScenepackRepository
import com.amverge.android.backend.media.cutting.SceneExtractor
import com.amverge.android.core.logging.Logger
import com.amverge.android.core.result.AppError
import com.amverge.android.core.result.OperationResult
import com.amverge.android.platform.storage.SafUriAccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ExportScenepackUseCase(
    private val context: Context,
    private val scenepackRepository: ScenepackRepository,
    private val projectRepository: ProjectRepository,
    private val sceneExtractor: SceneExtractor,
    private val safUriAccess: SafUriAccess
) {
    companion object {
        private const val TAG = "ExportScenepackUseCase"
    }

    suspend fun exportScenepackToZip(
        scenepackId: String,
        destinationTreeUri: String? = null,
        onProgress: (Float, String) -> Unit = { _, _ -> }
    ): OperationResult<String> = withContext(Dispatchers.IO) {
        val scenepack = scenepackRepository.getScenepack(scenepackId)
            ?: return@withContext OperationResult.Failure(AppError.ScenepackNotFound("Scenepack not found"))
        val scenes = scenepackRepository.getScenesForScenepack(scenepackId)
        if (scenes.isEmpty()) {
            return@withContext OperationResult.Failure(AppError.NoScenesSelected("Scenepack has no scenes"))
        }

        val stagingDir = File(context.cacheDir, "scenepack_staging_${scenepackId}_${System.currentTimeMillis()}")
        stagingDir.mkdirs()

        try {
            val totalScenes = scenes.size
            val stagedFiles = mutableListOf<File>()
            val edlEvents = mutableListOf<String>()
            var recordStartMs = 0L

            for (i in scenes.indices) {
                val scene = scenes[i]
                val project = projectRepository.getProject(scene.projectId)
                    ?: continue

                val progress = 0.05f + (0.75f * (i.toFloat() / totalScenes))
                val msg = "Exporting clip ${i + 1} of $totalScenes"
                onProgress(progress, msg)

                val cleanPackName = scenepack.name.replace("[^a-zA-Z0-9_-]".toRegex(), "_")
                val sceneName = "${cleanPackName}_clip_${String.format("%03d", i + 1)}.mp4"
                val stagingFile = File(stagingDir, sceneName)

                val extractResult = sceneExtractor.extractSceneToStaging(
                    sourceUri = project.sourceUri,
                    scene = scene,
                    stagingDir = stagingDir
                )

                if (extractResult is OperationResult.Success) {
                    val extractedFile = extractResult.data
                    val finalFile = File(stagingDir, sceneName)
                    if (extractedFile.exists() && extractedFile != finalFile) {
                        extractedFile.renameTo(finalFile)
                    }
                    val targetFile = if (finalFile.exists()) finalFile else extractedFile
                    stagedFiles.add(targetFile)

                    val sceneDurationMs = scene.durationMs
                    val recordEndMs = recordStartMs + sceneDurationMs
                    val eventNum = String.format("%03d", i + 1)
                    val srcIn = formatEdlTimecode(scene.startMs)
                    val srcOut = formatEdlTimecode(scene.endMs)
                    val recIn = formatEdlTimecode(recordStartMs)
                    val recOut = formatEdlTimecode(recordEndMs)
                    edlEvents.add("$eventNum  AX       V     C        $srcIn $srcOut $recIn $recOut\n* FROM CLIP NAME: $sceneName\n")
                    recordStartMs = recordEndMs
                }
            }

            if (stagedFiles.isEmpty()) {
                return@withContext OperationResult.Failure(AppError.ExportExecutionFailed("Failed to extract any scenes for scenepack"))
            }

            onProgress(0.82f, "Generating timeline manifest")
            val cleanPackName = scenepack.name.replace("[^a-zA-Z0-9_-]".toRegex(), "_")
            val edlFile = File(stagingDir, "${cleanPackName}_timeline.edl")
            val edlContent = buildString {
                append("TITLE: ${scenepack.name}\n")
                append("FCM: NON-DROP FRAME\n\n")
                edlEvents.forEach { append(it).append("\n") }
            }
            edlFile.writeText(edlContent)

            val metaFile = File(stagingDir, "scenepack_info.json")
            val metaJson = buildString {
                append("{\n")
                append("  \"name\": \"${scenepack.name}\",\n")
                append("  \"sceneCount\": ${stagedFiles.size},\n")
                append("  \"createdAt\": ${scenepack.createdAt}\n")
                append("}\n")
            }
            metaFile.writeText(metaJson)

            onProgress(0.90f, "Packaging ZIP archive")
            val zipFileName = "Scenepack_${cleanPackName}_${System.currentTimeMillis() % 100000}.zip"
            val zipFile = File(stagingDir, zipFileName)

            ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
                val filesToZip = stagedFiles + listOf(edlFile, metaFile)
                for (file in filesToZip) {
                    zos.putNextEntry(ZipEntry(file.name))
                    FileInputStream(file).use { fis ->
                        fis.copyTo(zos)
                    }
                    zos.closeEntry()
                }
            }

            onProgress(0.97f, "Saving ZIP to storage")
            val savedLocation: String = if (destinationTreeUri != null) {
                val treeUri = Uri.parse(destinationTreeUri)
                val docUri = safUriAccess.createDocumentInTree(treeUri, "application/zip", zipFileName)
                if (docUri != null && safUriAccess.copyFileToDocumentUri(zipFile, docUri)) {
                    docUri.toString()
                } else {
                    val sharedUri = safUriAccess.saveFileToSharedStorage(zipFile, zipFileName)
                    sharedUri?.toString() ?: zipFile.absolutePath
                }
            } else {
                val sharedUri = safUriAccess.saveFileToSharedStorage(zipFile, zipFileName)
                if (sharedUri != null) {
                    sharedUri.toString()
                } else {
                    val permanentFile = File(context.filesDir, "scenepacks/$zipFileName")
                    permanentFile.parentFile?.mkdirs()
                    zipFile.copyTo(permanentFile, overwrite = true)
                    permanentFile.absolutePath
                }
            }

            onProgress(1.0f, "Export complete")
            OperationResult.Success(savedLocation)
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to export scenepack", e)
            OperationResult.Failure(AppError.Unknown(e.message ?: "Failed to export scenepack", e))
        } finally {
            stagingDir.deleteRecursively()
        }
    }

    private fun formatEdlTimecode(ms: Long, fps: Double = 23.976): String {
        val totalSeconds = ms / 1000.0
        val totalFrames = (totalSeconds * fps).toLong()
        val framesPerSec = fps.toInt().coerceAtLeast(1)
        val f = (totalFrames % framesPerSec).toInt()
        val totalSecs = (ms / 1000).toInt()
        val s = totalSecs % 60
        val m = (totalSecs / 60) % 60
        val h = totalSecs / 3600
        return String.format("%02d:%02d:%02d:%02d", h, m, s, f)
    }
}
