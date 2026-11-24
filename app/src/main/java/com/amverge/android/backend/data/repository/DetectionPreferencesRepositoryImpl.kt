package com.amverge.android.backend.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.amverge.android.backend.domain.model.DetectionPreferences
import com.amverge.android.backend.domain.repository.DetectionPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class DetectionPreferencesRepositoryImpl(
    context: Context
) : DetectionPreferencesRepository {

    private val prefs: SharedPreferences = context.getSharedPreferences("amverge_detection_prefs", Context.MODE_PRIVATE)

    private val _preferencesFlow = MutableStateFlow(readFromPrefs())

    override fun observePreferences(): Flow<DetectionPreferences> = _preferencesFlow.asStateFlow()

    override suspend fun getPreferences(): DetectionPreferences = _preferencesFlow.value

    override suspend fun updatePreferences(preferences: DetectionPreferences) {
        prefs.edit()
            .putLong(KEY_MIN_DURATION, preferences.minSceneDurationMs)
            .putBoolean(KEY_FILTER_DEADFRAMES, preferences.filterDeadframes)
            .putFloat(KEY_DEADFRAME_THRESHOLD, preferences.deadframeThreshold)
            .putBoolean(KEY_DETECT_TRANSITIONS, preferences.detectTransitions)
            .putFloat(KEY_SENSITIVITY, preferences.sensitivity)
            .apply()

        _preferencesFlow.update { preferences }
    }

    private fun readFromPrefs(): DetectionPreferences {
        return DetectionPreferences(
            minSceneDurationMs = prefs.getLong(KEY_MIN_DURATION, 250L),
            filterDeadframes = prefs.getBoolean(KEY_FILTER_DEADFRAMES, false),
            deadframeThreshold = prefs.getFloat(KEY_DEADFRAME_THRESHOLD, 0.15f),
            detectTransitions = prefs.getBoolean(KEY_DETECT_TRANSITIONS, true),
            sensitivity = prefs.getFloat(KEY_SENSITIVITY, 0.5f)
        )
    }

    companion object {
        private const val KEY_MIN_DURATION = "min_scene_duration_ms"
        private const val KEY_FILTER_DEADFRAMES = "filter_deadframes"
        private const val KEY_DEADFRAME_THRESHOLD = "deadframe_threshold"
        private const val KEY_DETECT_TRANSITIONS = "detect_transitions"
        private const val KEY_SENSITIVITY = "detection_sensitivity"
    }
}
