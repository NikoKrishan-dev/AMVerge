package com.amverge.android.platform.worker

import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.amverge.android.app.AMVergeApplication
import com.amverge.android.backend.domain.model.ProcessingStage
import com.amverge.android.core.logging.Logger
import com.amverge.android.core.result.OperationResult
import com.amverge.android.platform.notification.NotificationHelper

class ImportEpisodeWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val KEY_SOURCE_URI = "source_uri"
        const val KEY_DISPLAY_NAME = "display_name"
        const val KEY_PERSISTED_FLAGS = "persisted_flags"
        const val KEY_RESULT_PROJECT_ID = "result_project_id"
        private const val TAG = "ImportEpisodeWorker"
    }

    override suspend fun doWork(): Result {
        val sourceUri = inputData.getString(KEY_SOURCE_URI) ?: return Result.failure()
        val displayName = inputData.getString(KEY_DISPLAY_NAME) ?: "Episode"
        val flags = inputData.getInt(KEY_PERSISTED_FLAGS, 0)

        val app = applicationContext as AMVergeApplication
        val container = app.container
        val notificationHelper = container.notificationHelper

        val initialNotification = notificationHelper.buildProgressNotification(
            title = "Importing $displayName",
            message = "Probing video stream...",
            progress = 0,
            indeterminate = true
        )

        val foregroundInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                NotificationHelper.NOTIFICATION_ID_IMPORT,
                initialNotification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            ForegroundInfo(NotificationHelper.NOTIFICATION_ID_IMPORT, initialNotification)
        }
        setForeground(foregroundInfo)

        val result = container.importEpisodeUseCase(
            sourceUri = sourceUri,
            displayName = displayName,
            persistedReadFlags = flags,
            onProgress = { stage, progress, message ->
                val progressInt = (progress * 100).toInt()
                val updatedNotification = notificationHelper.buildProgressNotification(
                    title = "Importing $displayName",
                    message = message ?: stage.name,
                    progress = progressInt,
                    indeterminate = false
                )
                try {
                    val updateInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        ForegroundInfo(
                            NotificationHelper.NOTIFICATION_ID_IMPORT,
                            updatedNotification,
                            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                        )
                    } else {
                        ForegroundInfo(NotificationHelper.NOTIFICATION_ID_IMPORT, updatedNotification)
                    }
                    // Updating progress
                    setProgressAsync(
                        workDataOf(
                            "stage" to stage.name,
                            "progress" to progress,
                            "message" to message
                        )
                    )
                } catch (e: Exception) {
                    Logger.w(TAG, "Failed to update worker progress", e)
                }
            }
        )

        return when (result) {
            is OperationResult.Success -> {
                Logger.i(TAG, "Import completed successfully: ${result.data.id}")
                Result.success(workDataOf(KEY_RESULT_PROJECT_ID to result.data.id))
            }
            is OperationResult.Failure -> {
                Logger.e(TAG, "Import failed: ${result.error.message}")
                Result.failure(workDataOf("error" to result.error.message))
            }
        }
    }
}
