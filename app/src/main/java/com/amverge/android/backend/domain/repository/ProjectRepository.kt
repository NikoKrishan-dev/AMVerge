package com.amverge.android.backend.domain.repository

import com.amverge.android.backend.domain.model.EpisodeProject
import com.amverge.android.backend.domain.model.ProjectStatus
import kotlinx.coroutines.flow.Flow

interface ProjectRepository {
    fun observeProject(projectId: String): Flow<EpisodeProject?>
    fun observeAllProjects(): Flow<List<EpisodeProject>>
    suspend fun getProject(projectId: String): EpisodeProject?
    suspend fun saveProject(project: EpisodeProject)
    suspend fun updateStatus(projectId: String, status: ProjectStatus)
    suspend fun assignSeries(projectId: String, seriesId: String?, episodeNumber: Int?)
    fun observeProjectsForSeries(seriesId: String): Flow<List<EpisodeProject>>
    suspend fun deleteProject(projectId: String)
}
