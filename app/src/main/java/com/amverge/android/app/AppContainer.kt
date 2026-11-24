package com.amverge.android.app

import android.content.Context
import com.amverge.android.backend.ai.AiProcessingEngine
import com.amverge.android.backend.ai.DefaultAiProcessingEngine
import com.amverge.android.backend.data.database.AMVergeDatabase
import com.amverge.android.backend.data.repository.DetectionPreferencesRepositoryImpl
import com.amverge.android.backend.data.repository.JobRepositoryImpl
import com.amverge.android.backend.data.repository.ProjectRepositoryImpl
import com.amverge.android.backend.data.repository.SceneRepositoryImpl
import com.amverge.android.backend.data.repository.ScenepackRepositoryImpl
import com.amverge.android.backend.data.repository.SeriesRepositoryImpl
import com.amverge.android.backend.domain.repository.DetectionPreferencesRepository
import com.amverge.android.backend.domain.repository.JobRepository
import com.amverge.android.backend.domain.repository.ProjectRepository
import com.amverge.android.backend.domain.repository.SceneRepository
import com.amverge.android.backend.domain.repository.ScenepackRepository
import com.amverge.android.backend.domain.repository.SeriesRepository
import com.amverge.android.backend.domain.usecase.ExportScenepackUseCase
import com.amverge.android.backend.domain.usecase.ExportScenesUseCase
import com.amverge.android.backend.domain.usecase.ImportEpisodeUseCase
import com.amverge.android.backend.domain.usecase.RunAiToolUseCase
import com.amverge.android.backend.media.cutting.SceneExtractor
import com.amverge.android.backend.media.detection.KeyframeSceneDetector
import com.amverge.android.backend.media.detection.MotionVarianceEvaluator
import com.amverge.android.backend.media.detection.TransitionDetector
import com.amverge.android.backend.media.ffmpeg.FfmpegKitMediaEngine
import com.amverge.android.backend.media.ffmpeg.MediaEngine
import com.amverge.android.backend.media.merging.SceneConcatenator
import com.amverge.android.backend.media.thumbnail.SceneThumbnailGenerator
import com.amverge.android.platform.notification.NotificationHelper
import com.amverge.android.platform.storage.SafUriAccess

class AppContainer(private val context: Context) {

    val database: AMVergeDatabase by lazy {
        AMVergeDatabase.getInstance(context)
    }

    val projectRepository: ProjectRepository by lazy {
        ProjectRepositoryImpl(database.episodeProjectDao())
    }

    val sceneRepository: SceneRepository by lazy {
        SceneRepositoryImpl(database.sceneDao())
    }

    val jobRepository: JobRepository by lazy {
        JobRepositoryImpl(database.processingJobDao())
    }

    val seriesRepository: SeriesRepository by lazy {
        SeriesRepositoryImpl(database.seriesDao(), database.episodeProjectDao())
    }

    val scenepackRepository: ScenepackRepository by lazy {
        ScenepackRepositoryImpl(database.scenepackDao())
    }

    val mediaEngine: MediaEngine by lazy {
        FfmpegKitMediaEngine(context)
    }

    val sceneDetector: KeyframeSceneDetector by lazy {
        KeyframeSceneDetector()
    }

    val thumbnailGenerator: SceneThumbnailGenerator by lazy {
        SceneThumbnailGenerator(context, mediaEngine, sceneRepository)
    }

    val sceneExtractor: SceneExtractor by lazy {
        SceneExtractor(mediaEngine)
    }

    val sceneConcatenator: SceneConcatenator by lazy {
        SceneConcatenator(mediaEngine)
    }

    val safUriAccess: SafUriAccess by lazy {
        SafUriAccess(context)
    }

    val notificationHelper: NotificationHelper by lazy {
        NotificationHelper(context)
    }

    val detectionPreferencesRepository: DetectionPreferencesRepository by lazy {
        DetectionPreferencesRepositoryImpl(context)
    }

    val motionVarianceEvaluator: MotionVarianceEvaluator by lazy {
        MotionVarianceEvaluator(context)
    }

    val transitionDetector: TransitionDetector by lazy {
        TransitionDetector(context)
    }

    val importEpisodeUseCase: ImportEpisodeUseCase by lazy {
        ImportEpisodeUseCase(
            projectRepository = projectRepository,
            sceneRepository = sceneRepository,
            jobRepository = jobRepository,
            mediaEngine = mediaEngine,
            sceneDetector = sceneDetector,
            thumbnailGenerator = thumbnailGenerator,
            motionVarianceEvaluator = motionVarianceEvaluator,
            transitionDetector = transitionDetector,
            detectionPreferencesRepository = detectionPreferencesRepository
        )
    }

    val exportScenesUseCase: ExportScenesUseCase by lazy {
        ExportScenesUseCase(
            context = context,
            projectRepository = projectRepository,
            sceneRepository = sceneRepository,
            jobRepository = jobRepository,
            sceneExtractor = sceneExtractor,
            sceneConcatenator = sceneConcatenator,
            safUriAccess = safUriAccess
        )
    }

    val exportScenepackUseCase: ExportScenepackUseCase by lazy {
        ExportScenepackUseCase(
            context = context,
            scenepackRepository = scenepackRepository,
            projectRepository = projectRepository,
            sceneExtractor = sceneExtractor,
            safUriAccess = safUriAccess
        )
    }

    val aiProcessingEngine: AiProcessingEngine by lazy {
        DefaultAiProcessingEngine(context, mediaEngine)
    }

    val runAiToolUseCase: RunAiToolUseCase by lazy {
        RunAiToolUseCase(
            context = context,
            jobRepository = jobRepository,
            aiProcessingEngine = aiProcessingEngine
        )
    }
}
