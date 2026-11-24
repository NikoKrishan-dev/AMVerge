package com.amverge.android.backend.data.repository

import com.amverge.android.backend.data.database.dao.EpisodeProjectDao
import com.amverge.android.backend.data.database.dao.SeriesDao
import com.amverge.android.backend.data.database.entity.SeriesEntity
import com.amverge.android.backend.data.mapper.toDomain
import com.amverge.android.backend.domain.model.Series
import com.amverge.android.backend.domain.repository.SeriesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class SeriesRepositoryImpl(
    private val seriesDao: SeriesDao,
    private val episodeProjectDao: EpisodeProjectDao
) : SeriesRepository {

    override fun observeAllSeries(): Flow<List<Series>> {
        return seriesDao.observeAllSeries().map { list ->
            list.map { entity ->
                val count = episodeProjectDao.getEpisodeCountForSeries(entity.id)
                entity.toDomain(episodeCount = count)
            }
        }
    }

    override suspend fun getSeries(id: String): Series? {
        val entity = seriesDao.getSeriesById(id) ?: return null
        val count = episodeProjectDao.getEpisodeCountForSeries(id)
        return entity.toDomain(episodeCount = count)
    }

    override suspend fun createSeries(title: String, description: String?, coverUri: String?): Series {
        val id = UUID.randomUUID().toString()
        val entity = SeriesEntity(
            id = id,
            title = title,
            description = description,
            coverUri = coverUri,
            createdAt = System.currentTimeMillis()
        )
        seriesDao.insert(entity)
        return entity.toDomain(episodeCount = 0)
    }

    override suspend fun deleteSeries(id: String) {
        seriesDao.deleteById(id)
    }
}
