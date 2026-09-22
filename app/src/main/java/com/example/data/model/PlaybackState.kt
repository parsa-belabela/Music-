package com.example.data.model

enum class PlayerStatus {
    IDLE,
    PLAYING,
    PAUSED,
    BUFFERING,
    ERROR
}

enum class RepeatMode {
    OFF,
    ALL,
    ONE
}

enum class ShuffleMode {
    OFF,
    SHUFFLE,
    INTELLIGENT
}

data class PlaybackState(
    val currentTrack: Track? = null,
    val status: PlayerStatus = PlayerStatus.IDLE,
    val isPlayWhenReady: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val volume: Float = 1.0f,
    val isMuted: Boolean = false,
    val shuffleMode: ShuffleMode = ShuffleMode.OFF,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val queue: List<Track> = emptyList(),
    val queueIndex: Int = 0,
    val errorMessage: String? = null
) {
    val isShuffle: Boolean
        get() = shuffleMode != ShuffleMode.OFF

    val isPlaying: Boolean
        get() = status == PlayerStatus.PLAYING || (status == PlayerStatus.BUFFERING && isPlayWhenReady)

    val progress: Float
        get() = if (durationMs > 0) (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f

    val positionFormatted: String
        get() {
            val totalSec = currentPositionMs / 1000
            val min = totalSec / 60
            val sec = totalSec % 60
            return "%d:%02d".format(min, sec)
        }

    val remainingFormatted: String
        get() {
            val remMs = (durationMs - currentPositionMs).coerceAtLeast(0L)
            val totalSec = remMs / 1000
            val min = totalSec / 60
            val sec = totalSec % 60
            return "-%d:%02d".format(min, sec)
        }
}
