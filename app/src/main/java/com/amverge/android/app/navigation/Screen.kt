package com.amverge.android.app.navigation

sealed class Screen(val route: String) {
    data object Import : Screen("import")
    data object AiStudio : Screen("ai_studio")

    data object SceneGrid : Screen("scenes/{projectId}") {
        fun createRoute(projectId: String) = "scenes/$projectId"
    }

    data object ScenePreview : Screen("preview/{projectId}/{sceneId}") {
        fun createRoute(projectId: String, sceneId: String) = "preview/$projectId/$sceneId"
    }
}
