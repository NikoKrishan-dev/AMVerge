package com.amverge.android.backend.domain.model

data class DetectionPreferences(
    val minSceneDurationMs: Long = 250L,
    val filterDeadframes: Boolean = false,
    val deadframeThreshold: Float = 0.15f,
    val detectTransitions: Boolean = true,
    val sensitivity: Float = 0.5f
)
