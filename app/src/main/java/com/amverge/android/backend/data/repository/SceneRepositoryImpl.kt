package com.amverge.android.backend.data.repository

import com.amverge.android.backend.data.database.dao.SceneDao
import com.amverge.android.backend.data.mapper.toDomain
import com.amverge.android.backend.data.mapper.toEntity
import com.amverge.android.backend.domain.model.Scene
import com.amverge.android.backend.domain.model.ThumbnailStatus
import com.amverge.android.backend.domain.repository.SceneRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SceneRepositoryImpl(
    private val sceneDao: SceneDao
) : SceneRepository {

    override fun observeScenes(projectId: String): Flow<List<Scene>> {
        return sceneDao.observeScenesForProject(projectId).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getScenes(projectId: String): List<Scene> {
        return sceneDao.getScenesForProject(projectId).map { it.toDomain() }
    }

    override suspend fun getScenesByIds(projectId: String, sceneIds: List<String>): List<Scene> {
        return sceneDao.getScenesByIds(projectId, sceneIds).map { it.toDomain() }
    }

    override suspend fun getScene(sceneId: String): Scene? {
        return sceneDao.getSceneById(sceneId)?.toDomain()
    }

    override suspend fun replaceScenes(projectId: String, scenes: List<Scene>) {
        sceneDao.replaceScenesForProject(projectId, scenes.map { it.toEntity() })
    }

    override suspend fun updateThumbnail(sceneId: String, thumbnailPath: String, status: ThumbnailStatus) {
        sceneDao.updateThumbnail(sceneId, thumbnailPath, status.name)
    }

    override suspend fun updateThumbnailStatus(sceneId: String, status: ThumbnailStatus) {
        sceneDao.updateThumbnailStatus(sceneId, status.name)
    }

    override suspend fun toggleFavorite(sceneId: String, isFavorite: Boolean) {
        sceneDao.updateFavorite(sceneId, isFavorite)
    }

    override suspend fun updateTags(sceneId: String, tags: List<String>) {
        sceneDao.updateTags(sceneId, tags.joinToString(","))
    }

    override suspend fun mergeScenes(
        projectId: String,
        sceneIds: List<String>
    ): com.amverge.android.core.result.OperationResult<Scene> {
        val allScenes = getScenes(projectId).sortedBy { it.index }
        val toMerge = allScenes.filter { it.id in sceneIds }.sortedBy { it.startMs }

        if (toMerge.size < 2) {
            return com.amverge.android.core.result.OperationResult.Failure(
                com.amverge.android.core.result.AppError.InvalidSceneRange("Select at least 2 scenes to merge")
            )
        }

        // Verify contiguous boundaries
        for (i in 0 until toMerge.size - 1) {
            if (toMerge[i].endMs != toMerge[i + 1].startMs) {
                return com.amverge.android.core.result.OperationResult.Failure(
                    com.amverge.android.core.result.AppError.InvalidSceneRange("Selected scenes must be contiguous to merge")
                )
            }
        }

        val first = toMerge.first()
        val last = toMerge.last()
        val mergedTags = toMerge.flatMap { it.tags }.distinct()
        val mergedScene = Scene(
            id = java.util.UUID.randomUUID().toString(),
            projectId = projectId,
            index = first.index,
            startMs = first.startMs,
            endMs = last.endMs,
            thumbnailPath = first.thumbnailPath,
            thumbnailStatus = first.thumbnailStatus,
            isFavorite = toMerge.any { it.isFavorite },
            tags = mergedTags,
            motionScore = toMerge.map { it.motionScore }.average().toFloat(),
            isDeadframe = toMerge.all { it.isDeadframe }
        )

        val mergedIds = toMerge.map { it.id }.toSet()
        val newScenes = mutableListOf<Scene>()
        var insertedMerged = false

        for (scene in allScenes) {
            if (scene.id in mergedIds) {
                if (!insertedMerged) {
                    newScenes.add(mergedScene)
                    insertedMerged = true
                }
            } else {
                newScenes.add(scene)
            }
        }

        val reindexed = newScenes.mapIndexed { idx, s -> s.copy(index = idx) }
        replaceScenes(projectId, reindexed)
        return com.amverge.android.core.result.OperationResult.Success(mergedScene.copy(index = reindexed.indexOfFirst { it.id == mergedScene.id }))
    }

    override suspend fun splitScene(
        sceneId: String,
        splitAtMs: Long
    ): com.amverge.android.core.result.OperationResult<Pair<Scene, Scene>> {
        val target = getScene(sceneId)
            ?: return com.amverge.android.core.result.OperationResult.Failure(
                com.amverge.android.core.result.AppError.SceneNotFound("Scene $sceneId not found")
            )

        if (splitAtMs <= target.startMs + 50L || splitAtMs >= target.endMs - 50L) {
            return com.amverge.android.core.result.OperationResult.Failure(
                com.amverge.android.core.result.AppError.InvalidSceneRange("Split point must be strictly within scene boundaries")
            )
        }

        val allScenes = getScenes(target.projectId).sortedBy { it.index }
        val part1 = target.copy(
            id = java.util.UUID.randomUUID().toString(),
            endMs = splitAtMs
        )
        val part2 = target.copy(
            id = java.util.UUID.randomUUID().toString(),
            startMs = splitAtMs,
            thumbnailPath = null,
            thumbnailStatus = ThumbnailStatus.PENDING
        )

        val newScenes = mutableListOf<Scene>()
        for (scene in allScenes) {
            if (scene.id == sceneId) {
                newScenes.add(part1)
                newScenes.add(part2)
            } else {
                newScenes.add(scene)
            }
        }

        val reindexed = newScenes.mapIndexed { idx, s -> s.copy(index = idx) }
        replaceScenes(target.projectId, reindexed)
        return com.amverge.android.core.result.OperationResult.Success(Pair(part1, part2))
    }

    override suspend fun deleteScenes(
        projectId: String,
        sceneIds: List<String>
    ): com.amverge.android.core.result.OperationResult<Unit> {
        val allScenes = getScenes(projectId).sortedBy { it.index }
        val toDelete = sceneIds.toSet()
        val remaining = allScenes.filter { it.id !in toDelete }

        if (remaining.isEmpty()) {
            return com.amverge.android.core.result.OperationResult.Failure(
                com.amverge.android.core.result.AppError.InvalidSceneRange("Cannot delete all scenes from project")
            )
        }

        val reindexed = remaining.mapIndexed { idx, s -> s.copy(index = idx) }
        replaceScenes(projectId, reindexed)
        return com.amverge.android.core.result.OperationResult.Success(Unit)
    }
}
