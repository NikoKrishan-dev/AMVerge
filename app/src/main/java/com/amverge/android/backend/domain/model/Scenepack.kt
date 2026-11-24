package com.amverge.android.backend.domain.model

data class Scenepack(
    val id: String,
    val name: String,
    val description: String? = null,
    val sceneCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
