package com.amverge.android.backend.domain.model

enum class ThumbnailStatus {
    PENDING,
    RUNNING,
    READY,
    FAILED
}

data class Scene(
    val id: String,
    val projectId: String,
    val index: Int,
    val startMs: Long,
    val endMs: Long,
    val thumbnailPath: String? = null,
    val thumbnailStatus: ThumbnailStatus = ThumbnailStatus.PENDING,
    val isFavorite: Boolean = false,
    val tags: List<String> = emptyList(),
    val motionScore: Float = 0.5f,
    val isDeadframe: Boolean = false
) {
    val durationMs: Long get() = endMs - startMs

    init {
        require(startMs >= 0) { "startMs must be non-negative" }
        require(endMs > startMs) { "endMs must be strictly greater than startMs" }
    }
}
