package com.amverge.android.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.amverge.android.app.AMVergeApplication
import com.amverge.android.backend.media.playback.ScenePlayerController
import com.amverge.android.frontend.screens.ai.AiStudioScreen
import com.amverge.android.frontend.screens.import.ImportScreen
import com.amverge.android.frontend.screens.preview.ScenePreviewScreen
import com.amverge.android.frontend.screens.scenes.SceneGridScreen
import com.amverge.android.frontend.viewmodel.AiToolsViewModel
import com.amverge.android.frontend.viewmodel.ExportViewModel
import com.amverge.android.frontend.viewmodel.ImportViewModel
import com.amverge.android.frontend.viewmodel.PreviewViewModel
import com.amverge.android.frontend.viewmodel.SceneGridViewModel
import com.amverge.android.frontend.viewmodel.SeriesLibraryViewModel

@Composable
fun AMVergeNavHost(navController: NavHostController) {
    val context = LocalContext.current
    val app = context.applicationContext as AMVergeApplication
    val container = app.container

    val sharedAiToolsViewModel = remember {
        AiToolsViewModel(
            context = context,
            mediaEngine = container.mediaEngine,
            sceneExtractor = container.sceneExtractor,
            runAiToolUseCase = container.runAiToolUseCase,
            safUriAccess = container.safUriAccess
        )
    }

    NavHost(
        navController = navController,
        startDestination = Screen.Import.route
    ) {
        composable(Screen.Import.route) {
            val importViewModel = remember {
                ImportViewModel(
                    importEpisodeUseCase = container.importEpisodeUseCase,
                    projectRepository = container.projectRepository,
                    safUriAccess = container.safUriAccess,
                    detectionPreferencesRepository = container.detectionPreferencesRepository
                )
            }
            val seriesLibraryViewModel = remember {
                SeriesLibraryViewModel(
                    seriesRepository = container.seriesRepository,
                    scenepackRepository = container.scenepackRepository,
                    projectRepository = container.projectRepository,
                    exportScenepackUseCase = container.exportScenepackUseCase
                )
            }
            ImportScreen(
                viewModel = importViewModel,
                aiToolsViewModel = sharedAiToolsViewModel,
                seriesLibraryViewModel = seriesLibraryViewModel,
                onNavigateToProject = { projectId ->
                    navController.navigate(Screen.SceneGrid.createRoute(projectId))
                }
            )
        }

        composable(Screen.AiStudio.route) {
            AiStudioScreen(
                viewModel = sharedAiToolsViewModel,
                onNavigateBack = { navController.popBackStack() },
                showTopBar = true
            )
        }

        composable(
            route = Screen.SceneGrid.route,
            arguments = listOf(navArgument("projectId") { type = NavType.StringType })
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getString("projectId") ?: return@composable
            val gridViewModel = remember(projectId) {
                SceneGridViewModel(
                    projectId = projectId,
                    projectRepository = container.projectRepository,
                    sceneRepository = container.sceneRepository,
                    thumbnailGenerator = container.thumbnailGenerator,
                    scenepackRepository = container.scenepackRepository,
                    detectionPreferencesRepository = container.detectionPreferencesRepository
                )
            }
            val exportViewModel = remember(projectId) {
                ExportViewModel(container.exportScenesUseCase)
            }

            SceneGridScreen(
                viewModel = gridViewModel,
                exportViewModel = exportViewModel,
                aiToolsViewModel = sharedAiToolsViewModel,
                onNavigateBack = { navController.popBackStack() },
                onPreviewScene = { pId, sId ->
                    navController.navigate(Screen.ScenePreview.createRoute(pId, sId))
                }
            )
        }

        composable(
            route = Screen.ScenePreview.route,
            arguments = listOf(
                navArgument("projectId") { type = NavType.StringType },
                navArgument("sceneId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getString("projectId") ?: return@composable
            val sceneId = backStackEntry.arguments?.getString("sceneId") ?: return@composable

            val previewViewModel = remember(projectId, sceneId) {
                PreviewViewModel(
                    projectId = projectId,
                    sceneId = sceneId,
                    projectRepository = container.projectRepository,
                    sceneRepository = container.sceneRepository,
                    playerController = ScenePlayerController(context)
                )
            }

            ScenePreviewScreen(
                viewModel = previewViewModel,
                onNavigateBack = { navController.popBackStack() },
                onSendToAiStudio = { scene ->
                    navController.navigate(Screen.AiStudio.route)
                }
            )
        }
    }
}
