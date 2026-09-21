package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playlists")
data class Playlist(
    @PrimaryKey val id: String,
    val name: String,
    val description: String = "",
    val isSmart: Boolean = false,
    val smartRule: String = "", // e.g., "FAVORITES", "MOST_PLAYED", "RECENT_ADDED", "LONG_TRACKS"
    val createdAt: Long = System.currentTimeMillis(),
    val trackIds: List<String> = emptyList()
)

@Entity(tableName = "playlist_tracks", primaryKeys = ["playlistId", "trackId"])
data class PlaylistTrackCrossRef(
    val playlistId: String,
    val trackId: String,
    val sortOrder: Int
)
