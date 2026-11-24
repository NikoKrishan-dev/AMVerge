package com.amverge.android.backend.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.amverge.android.backend.data.database.entity.SceneEntity
import kotlinx.coroutines.flow.Flow

data class ThumbnailBatchItem(
    val sceneId: String,
    val thumbnailPath: String?,
    val status: String
)

@Dao
interface SceneDao {

    @Query("SELECT * FROM scenes WHERE projectId = :projectId ORDER BY sceneIndex ASC")
    fun observeScenesForProject(projectId: String): Flow<List<SceneEntity>>

    @Query("SELECT * FROM scenes WHERE projectId = :projectId ORDER BY sceneIndex ASC")
    suspend fun getScenesForProject(projectId: String): List<SceneEntity>

    @Query("SELECT * FROM scenes WHERE projectId = :projectId AND id IN (:sceneIds) ORDER BY sceneIndex ASC")
    suspend fun getScenesByIds(projectId: String, sceneIds: List<String>): List<SceneEntity>

    @Query("SELECT * FROM scenes WHERE id = :sceneId LIMIT 1")
    suspend fun getSceneById(sceneId: String): SceneEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScenes(scenes: List<SceneEntity>)

    @Query("DELETE FROM scenes WHERE projectId = :projectId")
    suspend fun deleteScenesForProject(projectId: String)

    @Transaction
    suspend fun replaceScenesForProject(projectId: String, scenes: List<SceneEntity>) {
        deleteScenesForProject(projectId)
        insertScenes(scenes)
    }

    @Query("UPDATE scenes SET thumbnailPath = :thumbnailPath, thumbnailStatus = :status WHERE id = :sceneId")
    suspend fun updateThumbnail(sceneId: String, thumbnailPath: String, status: String)

    @Query("UPDATE scenes SET thumbnailStatus = :status WHERE id = :sceneId")
    suspend fun updateThumbnailStatus(sceneId: String, status: String)

    @Transaction
    suspend fun updateThumbnailsBatch(updates: List<ThumbnailBatchItem>) {
        for (item in updates) {
            if (item.thumbnailPath != null) {
                updateThumbnail(item.sceneId, item.thumbnailPath, item.status)
            } else {
                updateThumbnailStatus(item.sceneId, item.status)
            }
        }
    }

    @Query("UPDATE scenes SET isFavorite = :isFavorite WHERE id = :sceneId")
    suspend fun updateFavorite(sceneId: String, isFavorite: Boolean)

    @Query("UPDATE scenes SET tags = :tags WHERE id = :sceneId")
    suspend fun updateTags(sceneId: String, tags: String)
}
