package com.amverge.android.backend.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.amverge.android.backend.data.database.entity.EpisodeProjectEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EpisodeProjectDao {

    @Query("SELECT * FROM episode_projects WHERE id = :id LIMIT 1")
    fun observeProject(id: String): Flow<EpisodeProjectEntity?>

    @Query("SELECT * FROM episode_projects ORDER BY createdAt DESC")
    fun observeAllProjects(): Flow<List<EpisodeProjectEntity>>

    @Query("SELECT * FROM episode_projects WHERE id = :id LIMIT 1")
    suspend fun getProject(id: String): EpisodeProjectEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(project: EpisodeProjectEntity)

    @Update
    suspend fun update(project: EpisodeProjectEntity)

    @Query("UPDATE episode_projects SET status = :status, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateStatus(id: String, status: String, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE episode_projects SET seriesId = :seriesId, episodeNumber = :episodeNumber, updatedAt = :updatedAt WHERE id = :id")
    suspend fun assignSeries(id: String, seriesId: String?, episodeNumber: Int?, updatedAt: Long = System.currentTimeMillis())

    @Query("SELECT * FROM episode_projects WHERE seriesId = :seriesId ORDER BY episodeNumber ASC, createdAt ASC")
    fun observeProjectsForSeries(seriesId: String): Flow<List<EpisodeProjectEntity>>

    @Query("SELECT COUNT(*) FROM episode_projects WHERE seriesId = :seriesId")
    suspend fun getEpisodeCountForSeries(seriesId: String): Int

    @Query("DELETE FROM episode_projects WHERE id = :id")
    suspend fun deleteById(id: String)
}
