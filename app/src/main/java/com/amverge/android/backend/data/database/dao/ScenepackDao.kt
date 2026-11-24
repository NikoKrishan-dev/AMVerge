package com.amverge.android.backend.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.amverge.android.backend.data.database.entity.SceneEntity
import com.amverge.android.backend.data.database.entity.ScenepackEntity
import com.amverge.android.backend.data.database.entity.ScenepackSceneCrossRef
import kotlinx.coroutines.flow.Flow

@Dao
interface ScenepackDao {

    @Query("SELECT * FROM scenepacks ORDER BY createdAt DESC")
    fun observeAllScenepacks(): Flow<List<ScenepackEntity>>

    @Query("SELECT * FROM scenepacks WHERE id = :id LIMIT 1")
    suspend fun getScenepackById(id: String): ScenepackEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScenepack(scenepack: ScenepackEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCrossRefs(refs: List<ScenepackSceneCrossRef>)

    @Query("""
        SELECT s.* FROM scenes s
        INNER JOIN scenepack_scenes ss ON s.id = ss.sceneId
        WHERE ss.scenepackId = :scenepackId
        ORDER BY ss.orderIndex ASC
    """)
    suspend fun getScenesForScenepack(scenepackId: String): List<SceneEntity>

    @Query("SELECT COUNT(*) FROM scenepack_scenes WHERE scenepackId = :scenepackId")
    suspend fun getSceneCountForScenepack(scenepackId: String): Int

    @Query("DELETE FROM scenepacks WHERE id = :id")
    suspend fun deleteScenepack(id: String)
}
