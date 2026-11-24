package com.amverge.android.backend.domain.repository

import com.amverge.android.backend.domain.model.Scene
import com.amverge.android.backend.domain.model.Scenepack
import kotlinx.coroutines.flow.Flow

interface ScenepackRepository {
    fun observeAllScenepacks(): Flow<List<Scenepack>>
    suspend fun getScenepack(id: String): Scenepack?
    suspend fun createScenepack(name: String, description: String? = null, sceneIds: List<String>): Scenepack
    suspend fun addScenesToScenepack(scenepackId: String, sceneIds: List<String>)
    suspend fun getScenesForScenepack(scenepackId: String): List<Scene>
    suspend fun deleteScenepack(id: String)
}
