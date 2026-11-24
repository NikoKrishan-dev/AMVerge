package com.amverge.android.backend.domain.repository

import com.amverge.android.backend.domain.model.Scene
import com.amverge.android.backend.domain.model.ThumbnailStatus
import kotlinx.coroutines.flow.Flow

data class ThumbnailBatchUpdate(
    val sceneId: String,
    val thumbnailPath: String?,
    val status: ThumbnailStatus
)

interface SceneRepository {
    fun observeScenes(projectId: String): Flow<List<Scene>>
    suspend fun getScenes(projectId: String): List<Scene>
    suspend fun getScenesByIds(projectId: String, sceneIds: List<String>): List<Scene>
    suspend fun getScene(sceneId: String): Scene?
    suspend fun replaceScenes(projectId: String, scenes: List<Scene>)
    suspend fun updateThumbnail(sceneId: String, thumbnailPath: String, status: ThumbnailStatus)
    suspend fun updateThumbnailStatus(sceneId: String, status: ThumbnailStatus)
    suspend fun updateThumbnailsBatch(updates: List<ThumbnailBatchUpdate>)
    suspend fun toggleFavorite(sceneId: String, isFavorite: Boolean)
    suspend fun updateTags(sceneId: String, tags: List<String>)
    suspend fun mergeScenes(projectId: String, sceneIds: List<String>): com.amverge.android.core.result.OperationResult<Scene>
    suspend fun splitScene(sceneId: String, splitAtMs: Long): com.amverge.android.core.result.OperationResult<Pair<Scene, Scene>>
    suspend fun deleteScenes(projectId: String, sceneIds: List<String>): com.amverge.android.core.result.OperationResult<Unit>
}
