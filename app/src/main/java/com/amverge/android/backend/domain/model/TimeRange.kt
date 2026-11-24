package com.amverge.android.backend.domain.model

data class TimeRange(
    val startMs: Long,
    val endMs: Long
) {
    init {
        require(startMs >= 0) { "startMs must be non-negative" }
        require(endMs > startMs) { "endMs must be strictly greater than startMs" }
    }

    val durationMs: Long get() = endMs - startMs
}
