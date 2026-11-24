package com.amverge.android.backend.domain.model

data class Series(
    val id: String,
    val title: String,
    val description: String? = null,
    val coverUri: String? = null,
    val episodeCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
