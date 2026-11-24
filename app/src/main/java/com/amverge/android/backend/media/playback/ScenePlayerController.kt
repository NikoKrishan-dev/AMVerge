package com.amverge.android.backend.media.playback

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.amverge.android.core.logging.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ScenePlayerController(
    context: Context
) {
    companion object {
        private const val TAG = "ScenePlayerController"
    }

    val player: ExoPlayer = ExoPlayer.Builder(context.applicationContext).build()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()

    private val _playbackError = MutableStateFlow<String?>(null)
    val playbackError: StateFlow<String?> = _playbackError.asStateFlow()

    init {
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                Logger.e(TAG, "Playback error: ${error.message}", error)
                _playbackError.value = error.message ?: "Playback failed"
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    _isPlaying.value = false
                }
            }
        })
    }

    fun playScene(sourceUri: String, startMs: Long, endMs: Long) {
        _playbackError.value = null
        val uri = Uri.parse(sourceUri)

        val clippingConfig = MediaItem.ClippingConfiguration.Builder()
            .setStartPositionMs(startMs)
            .setEndPositionMs(endMs)
            .build()

        val mediaItem = MediaItem.Builder()
            .setUri(uri)
            .setClippingConfiguration(clippingConfig)
            .build()

        player.setMediaItem(mediaItem)
        player.prepare()
        player.playWhenReady = true
        Logger.d(TAG, "Playing scene range [$startMs, $endMs] from $sourceUri")
    }

    fun play() {
        player.play()
    }

    fun pause() {
        player.pause()
    }

    fun seekTo(positionMs: Long) {
        player.seekTo(positionMs)
    }

    fun stepFrameForward(fps: Double = 24.0) {
        pause()
        val frameDurationMs = (1000.0 / fps).toLong().coerceAtLeast(1L)
        val target = player.currentPosition + frameDurationMs
        player.seekTo(target)
    }

    fun stepFrameBackward(fps: Double = 24.0) {
        pause()
        val frameDurationMs = (1000.0 / fps).toLong().coerceAtLeast(1L)
        val target = maxOf(0L, player.currentPosition - frameDurationMs)
        player.seekTo(target)
    }

    fun setPlaybackSpeed(speed: Float) {
        player.setPlaybackSpeed(speed)
    }

    fun setLooping(isLooping: Boolean) {
        player.repeatMode = if (isLooping) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
    }

    fun release() {
        player.release()
        Logger.d(TAG, "ExoPlayer released")
    }
}
