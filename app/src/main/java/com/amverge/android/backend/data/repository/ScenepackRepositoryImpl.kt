package com.amverge.android.backend.data.repository

import com.amverge.android.backend.data.database.dao.ScenepackDao
import com.amverge.android.backend.data.database.entity.ScenepackEntity
import com.amverge.android.backend.data.database.entity.ScenepackSceneCrossRef
import com.amverge.android.backend.data.mapper.toDomain
import com.amverge.android.backend.domain.model.Scene
import com.amverge.android.backend.domain.model.Scenepack
import com.amverge.android.backend.domain.repository.ScenepackRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class ScenepackRepositoryImpl(
    private val scenepackDao: ScenepackDao
) : ScenepackRepository {

    override fun observeAllScenepacks(): Flow<List<Scenepack>> {
        return scenepackDao.observeAllScenepacks().map { list ->
            list.map { entity ->
                val count = scenepackDao.getSceneCountForScenepack(entity.id)
                entity.toDomain(sceneCount = count)
            }
        }
    }

    override suspend fun getScenepack(id: String): Scenepack? {
        val entity = scenepackDao.getScenepackById(id) ?: return null
        val count = scenepackDao.getSceneCountForScenepack(id)
        return entity.toDomain(sceneCount = count)
    }

    override suspend fun createScenepack(
        name: String,
        description: String?,
        sceneIds: List<String>
    ): Scenepack {
        val id = UUID.randomUUID().toString()
        val entity = ScenepackEntity(
            id = id,
            name = name,
            description = description,
            createdAt = System.currentTimeMillis()
        )
        scenepackDao.insertScenepack(entity)
        if (sceneIds.isNotEmpty()) {
            val refs = sceneIds.mapIndexed { idx, sId ->
                ScenepackSceneCrossRef(
                    scenepackId = id,
                    sceneId = sId,
                    orderIndex = idx
                )
            }
            scenepackDao.insertCrossRefs(refs)
        }
        return entity.toDomain(sceneCount = sceneIds.size)
    }

    override suspend fun addScenesToScenepack(scenepackId: String, sceneIds: List<String>) {
        if (sceneIds.isEmpty()) return
        val existingCount = scenepackDao.getSceneCountForScenepack(scenepackId)
        val refs = sceneIds.mapIndexed { idx, sId ->
            ScenepackSceneCrossRef(
                scenepackId = scenepackId,
                sceneId = sId,
                orderIndex = existingCount + idx
            )
        }
        scenepackDao.insertCrossRefs(refs)
    }

    override suspend fun getScenesForScenepack(scenepackId: String): List<Scene> {
        return scenepackDao.getScenesForScenepack(scenepackId).map { it.toDomain() }
    }

    override suspend fun deleteScenepack(id: String) {
        scenepackDao.deleteScenepack(id)
    }
}
