package com.amverge.android.platform.worker

import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.amverge.android.app.AMVergeApplication
import com.amverge.android.backend.domain.model.ExportMode
import com.amverge.android.backend.domain.model.ExportRequest
import com.amverge.android.backend.domain.model.ProcessingStage
import com.amverge.android.core.logging.Logger
import com.amverge.android.core.result.OperationResult
import com.amverge.android.platform.notification.NotificationHelper

class ExportScenesWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val KEY_PROJECT_ID = "project_id"
        const val KEY_SCENE_IDS = "scene_ids"
        const val KEY_MODE = "mode"
        const val KEY_DESTINATION_TREE_URI = "destination_tree_uri"
        const val KEY_DESTINATION_DOC_URI = "destination_doc_uri"
        const val KEY_OUTPUT_FILE_NAME = "output_file_name"
        private const val TAG = "ExportScenesWorker"
    }

    override suspend fun doWork(): Result {
        val projectId = inputData.getString(KEY_PROJECT_ID) ?: return Result.failure()
        val sceneIds = inputData.getStringArray(KEY_SCENE_IDS)?.toList() ?: return Result.failure()
        val modeStr = inputData.getString(KEY_MODE) ?: ExportMode.INDIVIDUAL.name
        val mode = runCatching { ExportMode.valueOf(modeStr) }.getOrDefault(ExportMode.INDIVIDUAL)
        val treeUri = inputData.getString(KEY_DESTINATION_TREE_URI)
        val docUri = inputData.getString(KEY_DESTINATION_DOC_URI)
        val fileName = inputData.getString(KEY_OUTPUT_FILE_NAME)

        val app = applicationContext as AMVergeApplication
        val container = app.container
        val notificationHelper = container.notificationHelper

        val initialNotification = notificationHelper.buildProgressNotification(
            title = "Exporting Scenes",
            message = "Preparing export...",
            progress = 0,
            indeterminate = true
        )

        val foregroundInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                NotificationHelper.NOTIFICATION_ID_EXPORT,
                initialNotification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            ForegroundInfo(NotificationHelper.NOTIFICATION_ID_EXPORT, initialNotification)
        }
        setForeground(foregroundInfo)

        val request = ExportRequest(
            projectId = projectId,
            sceneIds = sceneIds,
            mode = mode,
            destinationTreeUri = treeUri,
            destinationDocumentUri = docUri,
            outputFileName = fileName
        )

        val result = container.exportScenesUseCase(
            request = request,
            onProgress = { stage, progress, message ->
                val progressInt = (progress * 100).toInt()
                val updatedNotification = notificationHelper.buildProgressNotification(
                    title = "Exporting Scenes",
                    message = message ?: stage.name,
                    progress = progressInt,
                    indeterminate = false
                )
                try {
                    val updateInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        ForegroundInfo(
                            NotificationHelper.NOTIFICATION_ID_EXPORT,
                            updatedNotification,
                            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                        )
                    } else {
                        ForegroundInfo(NotificationHelper.NOTIFICATION_ID_EXPORT, updatedNotification)
                    }
                    setProgressAsync(
                        workDataOf(
                            "stage" to stage.name,
                            "progress" to progress,
                            "message" to message
                        )
                    )
                } catch (e: Exception) {
                    Logger.w(TAG, "Failed to update export worker progress", e)
                }
            }
        )

        return when (result) {
            is OperationResult.Success -> {
                Logger.i(TAG, "Export completed with ${result.data.size} outputs")
                Result.success(workDataOf("output_count" to result.data.size))
            }
            is OperationResult.Failure -> {
                Logger.e(TAG, "Export failed: ${result.error.message}")
                Result.failure(workDataOf("error" to result.error.message))
            }
        }
    }
}
