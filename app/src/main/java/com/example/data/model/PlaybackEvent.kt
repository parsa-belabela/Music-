package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playback_events")
data class PlaybackEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val trackId: String,
    val trackTitle: String,
    val artist: String,
    val album: String,
    val genre: String = "Electronic",
    val timestamp: Long = System.currentTimeMillis(),
    val durationListenedMs: Long = 0L,
    val fullTrackDurationMs: Long = 0L,
    val wasSkipped: Boolean = false
)
