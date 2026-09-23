package com.example.data.db

import androidx.room.*
import com.example.data.model.LyricsEntity
import com.example.data.model.PlaybackEvent
import com.example.data.model.Playlist
import com.example.data.model.PlaylistTrackCrossRef
import com.example.data.model.Track
import kotlinx.coroutines.flow.Flow

@Dao
interface MusicDao {
    @Query("SELECT * FROM tracks WHERE isHiddenDuplicate = 0 ORDER BY title ASC")
    fun getAllTracks(): Flow<List<Track>>

    @Query("SELECT * FROM tracks ORDER BY title ASC")
    suspend fun getAllTracksSync(): List<Track>

    @Query("SELECT * FROM tracks WHERE isFavorite = 1 AND isHiddenDuplicate = 0 ORDER BY title ASC")
    fun getFavoriteTracks(): Flow<List<Track>>

    @Query("SELECT * FROM tracks WHERE lastPlayedTimestamp > 0 AND isHiddenDuplicate = 0 ORDER BY lastPlayedTimestamp DESC LIMIT 10")
    fun getRecentlyPlayedTracks(): Flow<List<Track>>

    @Query("SELECT * FROM tracks WHERE lastPlayedTimestamp > 0 ORDER BY lastPlayedTimestamp DESC")
    suspend fun getAllRecentlyPlayedTracksSync(): List<Track>

    @Query("SELECT * FROM tracks WHERE isHiddenDuplicate = 0 ORDER BY playCount DESC LIMIT 30")
    fun getMostPlayedTracks(): Flow<List<Track>>

    @Query("SELECT * FROM tracks WHERE isHiddenDuplicate = 0 ORDER BY dateAdded DESC LIMIT 30")
    fun getRecentlyAddedTracks(): Flow<List<Track>>

    @Query("SELECT * FROM tracks WHERE isHiddenDuplicate = 1")
    fun getHiddenDuplicates(): Flow<List<Track>>

    @Query("SELECT * FROM tracks WHERE isInstrumental = 1 AND isHiddenDuplicate = 0 ORDER BY playCount DESC")
    fun getInstrumentalTracks(): Flow<List<Track>>

    @Query("SELECT * FROM tracks WHERE id = :id LIMIT 1")
    suspend fun getTrackById(id: String): Track?

    @Query("UPDATE tracks SET isHiddenDuplicate = :isHidden, duplicateGroupId = :groupId WHERE id = :trackId")
    suspend fun setTrackDuplicateState(trackId: String, isHidden: Boolean, groupId: String?)

    @Query("UPDATE tracks SET isInstrumental = :isInstrumental WHERE id = :trackId")
    suspend fun setTrackInstrumental(trackId: String, isInstrumental: Boolean)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTracks(tracks: List<Track>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertNewTracks(tracks: List<Track>): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrack(track: Track)

    @Update
    suspend fun updateTrack(track: Track)

    @Query("UPDATE tracks SET isFavorite = :isFavorite WHERE id = :trackId")
    suspend fun setFavorite(trackId: String, isFavorite: Boolean)

    @Query("UPDATE tracks SET playCount = playCount + 1, lastPlayedTimestamp = :timestamp WHERE id = :trackId")
    suspend fun recordPlay(trackId: String, timestamp: Long)

    @Query("UPDATE tracks SET lastPlayedTimestamp = 0 WHERE id = :trackId")
    suspend fun clearTrackRecentTimestamp(trackId: String)

    @Query("UPDATE tracks SET lastPlayedTimestamp = 0 WHERE id IN (SELECT id FROM tracks WHERE lastPlayedTimestamp > 0 ORDER BY lastPlayedTimestamp DESC LIMIT -1 OFFSET :limit)")
    suspend fun pruneRecentlyPlayedBeyondLimit(limit: Int = 10)

    @Query("DELETE FROM tracks WHERE id = :trackId")
    suspend fun deleteTrack(trackId: String)

    // Lyrics
    @Query("SELECT * FROM lyrics WHERE trackId = :trackId LIMIT 1")
    fun getLyrics(trackId: String): Flow<LyricsEntity?>

    @Query("SELECT * FROM lyrics WHERE trackId = :trackId LIMIT 1")
    suspend fun getLyricsSync(trackId: String): LyricsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLyrics(lyrics: LyricsEntity)

    @Query("DELETE FROM lyrics WHERE trackId = :trackId")
    suspend fun deleteLyrics(trackId: String)

    // Playlists
    @Query("SELECT * FROM playlists ORDER BY createdAt DESC")
    fun getAllPlaylists(): Flow<List<Playlist>>

    @Query("SELECT * FROM playlists WHERE id = :id LIMIT 1")
    suspend fun getPlaylistById(id: String): Playlist?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: Playlist)

    @Query("DELETE FROM playlists WHERE id = :id")
    suspend fun deletePlaylist(id: String)

    @Query("SELECT * FROM playlist_tracks WHERE playlistId = :playlistId ORDER BY sortOrder ASC")
    fun getPlaylistTracks(playlistId: String): Flow<List<PlaylistTrackCrossRef>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylistTrack(ref: PlaylistTrackCrossRef)

    @Query("DELETE FROM playlist_tracks WHERE playlistId = :playlistId AND trackId = :trackId")
    suspend fun removeTrackFromPlaylist(playlistId: String, trackId: String)

    // Playback Events for Listening Wrapped Analysis
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaybackEvent(event: PlaybackEvent)

    @Query("SELECT COUNT(*) FROM tracks WHERE isFavorite = 1 AND isHiddenDuplicate = 0")
    suspend fun getFavoriteCount(): Int

    @Query("SELECT COUNT(*) FROM tracks WHERE isFavorite = 1 AND isHiddenDuplicate = 0")
    fun getFavoriteCountFlow(): Flow<Int>

    @Query("SELECT COALESCE(SUM(playCount), 0) FROM tracks")
    suspend fun getTotalPlayCount(): Int

    @Query("SELECT * FROM tracks WHERE playCount > 0 ORDER BY playCount DESC LIMIT 1")
    suspend fun getTopPlayedTrack(): Track?

    @Query("SELECT * FROM tracks WHERE lastPlayedTimestamp > 0 ORDER BY lastPlayedTimestamp DESC LIMIT 1")
    suspend fun getLastPlayedTrack(): Track?

    @Query("SELECT * FROM playback_events ORDER BY timestamp DESC")
    fun getAllPlaybackEventsFlow(): Flow<List<PlaybackEvent>>

    @Query("SELECT * FROM playback_events ORDER BY timestamp DESC")
    suspend fun getAllPlaybackEvents(): List<PlaybackEvent>

    @Query("SELECT * FROM playback_events WHERE timestamp >= :startMs AND timestamp <= :endMs ORDER BY timestamp DESC")
    suspend fun getPlaybackEventsBetween(startMs: Long, endMs: Long): List<PlaybackEvent>

    @Query("SELECT COUNT(*) FROM playback_events")
    suspend fun getPlaybackEventsCount(): Int

    @Query("SELECT MIN(timestamp) FROM playback_events")
    suspend fun getFirstPlaybackTimestamp(): Long?

    @Query("DELETE FROM playback_events")
    suspend fun clearAllPlaybackEvents()

    @Query("DELETE FROM tracks WHERE isDemo = 1 OR id LIKE 'aura_demo_%'")
    suspend fun deleteDemoTracks()
}
