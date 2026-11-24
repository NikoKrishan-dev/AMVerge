package com.amverge.android.backend.domain.repository

import com.amverge.android.backend.domain.model.Series
import kotlinx.coroutines.flow.Flow

interface SeriesRepository {
    fun observeAllSeries(): Flow<List<Series>>
    suspend fun getSeries(id: String): Series?
    suspend fun createSeries(title: String, description: String? = null, coverUri: String? = null): Series
    suspend fun deleteSeries(id: String)
}
