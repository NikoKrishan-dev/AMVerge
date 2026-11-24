package com.amverge.android.core.common

import java.util.Locale

object MediaTime {

    const val MIN_SCENE_DURATION_MS: Long = 250L

    fun toSeconds(ms: Long): Double = ms / 1000.0

    fun toMicroseconds(ms: Long): Long = ms * 1000L

    fun fromSeconds(seconds: Double): Long = (seconds * 1000.0).toLong()

    fun fromMicroseconds(us: Long): Long = us / 1000L

    fun formatTime(ms: Long): String {
        val totalSeconds = ms / 1000
        val millis = ms % 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60

        return if (hours > 0) {
            String.format(Locale.US, "%02d:%02d:%02d.%03d", hours, minutes, seconds, millis)
        } else {
            String.format(Locale.US, "%02d:%02d.%03d", minutes, seconds, millis)
        }
    }

    fun formatDuration(ms: Long): String {
        if (ms < 1000) {
            return "${ms}ms"
        }
        val seconds = ms / 1000.0
        return String.format(Locale.US, "%.2fs", seconds)
    }
}
