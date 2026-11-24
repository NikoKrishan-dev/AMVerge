package com.amverge.android.backend.domain.repository

import com.amverge.android.backend.domain.model.DetectionPreferences
import kotlinx.coroutines.flow.Flow

interface DetectionPreferencesRepository {
    fun observePreferences(): Flow<DetectionPreferences>
    suspend fun getPreferences(): DetectionPreferences
    suspend fun updatePreferences(preferences: DetectionPreferences)
}
