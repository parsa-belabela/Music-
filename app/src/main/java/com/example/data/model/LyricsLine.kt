package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

data class WordTimestamp(
    val word: String,
    val startMs: Long,
    val endMs: Long
)

data class LyricsLine(
    val timestampMs: Long,
    val text: String,
    val words: List<WordTimestamp> = emptyList()
) {
    val formattedTimestamp: String
        get() {
            val totalSec = timestampMs / 1000
            val min = totalSec / 60
            val sec = totalSec % 60
            val ms = (timestampMs % 1000) / 10
            return "%02d:%02d.%02d".format(min, sec, ms)
        }
}

@Entity(tableName = "lyrics")
data class LyricsEntity(
    @PrimaryKey val trackId: String,
    val rawLrc: String,
    val offsetMs: Long = 0L,
    val isUserEdited: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)

enum class LyricsDisplayMode(val displayName: String) {
    CLASSIC("Classic"),
    MINIMAL("Minimal"),
    CENTER("Center"),
    CINEMATIC("Cinematic"),
    KARAOKE("Karaoke"),
    FOCUS("Focus"),
    FLOATING("Floating")
}
