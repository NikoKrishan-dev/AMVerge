package com.amverge.android.backend.data.repository

import com.amverge.android.backend.data.database.dao.EpisodeProjectDao
import com.amverge.android.backend.data.mapper.toDomain
import com.amverge.android.backend.data.mapper.toEntity
import com.amverge.android.backend.domain.model.EpisodeProject
import com.amverge.android.backend.domain.model.ProjectStatus
import com.amverge.android.backend.domain.repository.ProjectRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ProjectRepositoryImpl(
    private val projectDao: EpisodeProjectDao
) : ProjectRepository {

    override fun observeProject(projectId: String): Flow<EpisodeProject?> {
        return projectDao.observeProject(projectId).map { it?.toDomain() }
    }

    override fun observeAllProjects(): Flow<List<EpisodeProject>> {
        return projectDao.observeAllProjects().map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getProject(projectId: String): EpisodeProject? {
        return projectDao.getProject(projectId)?.toDomain()
    }

    override suspend fun saveProject(project: EpisodeProject) {
        projectDao.insert(project.toEntity())
    }

    override suspend fun updateStatus(projectId: String, status: ProjectStatus) {
        projectDao.updateStatus(projectId, status.name)
    }

    override suspend fun assignSeries(projectId: String, seriesId: String?, episodeNumber: Int?) {
        projectDao.assignSeries(projectId, seriesId, episodeNumber)
    }

    override fun observeProjectsForSeries(seriesId: String): Flow<List<EpisodeProject>> {
        return projectDao.observeProjectsForSeries(seriesId).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun deleteProject(projectId: String) {
        projectDao.deleteById(projectId)
    }
}
