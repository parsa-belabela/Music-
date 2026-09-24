package com.example.data.repository

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import com.example.data.db.MusicDao
import com.example.data.model.*
import com.example.lyrics.LrcParser
import com.example.util.WrappedEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class MusicRepository(
    private val context: Context,
    private val musicDao: MusicDao,
    private val trackAudioProfileDao: com.example.data.db.TrackAudioProfileDao? = null
) {
    private val tag = "MusicRepository"

    val allTracks: Flow<List<Track>> = musicDao.getAllTracks()
    val favoriteTracks: Flow<List<Track>> = musicDao.getFavoriteTracks()
    val recentlyPlayed: Flow<List<Track>> = musicDao.getRecentlyPlayedTracks()
    val mostPlayed: Flow<List<Track>> = musicDao.getMostPlayedTracks()
    val recentlyAdded: Flow<List<Track>> = musicDao.getRecentlyAddedTracks()
    val playlists: Flow<List<Playlist>> = musicDao.getAllPlaylists()
    val hiddenDuplicates: Flow<List<Track>> = musicDao.getHiddenDuplicates()
    val instrumentalTracks: Flow<List<Track>> = musicDao.getInstrumentalTracks()

    suspend fun getAudioProfile(trackId: String): TrackAudioProfile? = withContext(Dispatchers.IO) {
        val dao = trackAudioProfileDao ?: com.example.data.db.AppDatabase.getInstance(context).trackAudioProfileDao()
        dao.getProfile(trackId)
    }

    suspend fun getAudioProfiles(trackIds: List<String>): List<TrackAudioProfile> = withContext(Dispatchers.IO) {
        val dao = trackAudioProfileDao ?: com.example.data.db.AppDatabase.getInstance(context).trackAudioProfileDao()
        dao.getProfiles(trackIds)
    }

    suspend fun saveAudioProfile(profile: TrackAudioProfile) = withContext(Dispatchers.IO) {
        val dao = trackAudioProfileDao ?: com.example.data.db.AppDatabase.getInstance(context).trackAudioProfileDao()
        dao.upsert(profile)
    }

    suspend fun ensureAudioProfilesAnalyzed(
        analyzer: com.example.audio.TrackAnalyzer,
        ctx: Context,
        isPlaying: Boolean = false,
        batterySaver: Boolean = false
    ) = withContext(Dispatchers.IO) {
        if (batterySaver) return@withContext
        try {
            val dao = trackAudioProfileDao ?: com.example.data.db.AppDatabase.getInstance(ctx).trackAudioProfileDao()
            val allTracksList = musicDao.getAllTracksSync()
            val analyzedIds = dao.getAllAnalyzedTrackIds().toSet()
            val unanalyzed = allTracksList.filter { it.id !in analyzedIds }

            for (track in unanalyzed) {
                if (batterySaver) break
                val profile = analyzer.analyze(ctx, track)
                dao.upsert(profile)
                val delayMs = if (isPlaying) 400L else 120L
                kotlinx.coroutines.delay(delayMs)
            }
        } catch (e: Exception) {
            Log.w(tag, "Audio profile background analysis error: ${e.message}")
        }
    }

    suspend fun detectDuplicates(): List<com.example.ui.components.DuplicateGroup> = withContext(Dispatchers.IO) {
        val all = musicDao.getAllTracksSync()
        if (all.size < 2) return@withContext emptyList()

        fun normalizeStr(s: String): String {
            return s.lowercase()
                .replace(Regex("[^a-z0-9\u0600-\u06FF]"), "")
                .trim()
        }

        // Group by normalized title + artist + rounded duration (to nearest 3 seconds)
        val candidateGroups = all.groupBy { track ->
            val normTitle = normalizeStr(track.title)
            val normArtist = normalizeStr(track.artist)
            val durBucket = (track.durationMs / 3000L)
            "$normTitle|$normArtist|$durBucket"
        }.filter { it.value.size > 1 }

        val duplicateGroups = mutableListOf<com.example.ui.components.DuplicateGroup>()
        val retriever = android.media.MediaMetadataRetriever()

        for ((key, tracks) in candidateGroups) {
            val enrichedTracks = tracks.map { trk ->
                var actualBitrate = trk.bitrate
                try {
                    if (trk.uri.isNotEmpty() && !trk.uri.startsWith("android.resource://")) {
                        retriever.setDataSource(context, Uri.parse(trk.uri))
                        val brStr = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_BITRATE)
                        if (!brStr.isNullOrEmpty()) {
                            actualBitrate = brStr.toInt() / 1000
                        }
                    }
                } catch (_: Exception) {}
                trk.copy(bitrate = actualBitrate)
            }

            // Highest bitrate wins
            val sorted = enrichedTracks.sortedByDescending { it.bitrate }
            val winner = sorted.first()
            val losers = sorted.drop(1)
            val groupId = java.util.UUID.randomUUID().toString()

            duplicateGroups.add(
                com.example.ui.components.DuplicateGroup(
                    groupId = groupId,
                    winner = winner,
                    losers = losers
                )
            )
        }

        try {
            retriever.release()
        } catch (_: Exception) {}

        duplicateGroups
    }

    suspend fun applyDuplicateGroupWinners(groups: List<com.example.ui.components.DuplicateGroup>) = withContext(Dispatchers.IO) {
        for (group in groups) {
            musicDao.setTrackDuplicateState(group.winner.id, false, group.groupId)
            for (loser in group.losers) {
                musicDao.setTrackDuplicateState(loser.id, true, group.groupId)
            }
        }
    }

    suspend fun unhideTrack(trackId: String) = withContext(Dispatchers.IO) {
        musicDao.setTrackDuplicateState(trackId, false, null)
    }

    suspend fun setTrackInstrumental(trackId: String, isInstrumental: Boolean) = withContext(Dispatchers.IO) {
        musicDao.setTrackInstrumental(trackId, isInstrumental)
    }

    suspend fun getTracksForCurrentTimeSlot(limit: Int = 15): List<Track> = withContext(Dispatchers.IO) {
        val currentHour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        val allEvents = musicDao.getAllPlaybackEvents()
        val allTracksMap = musicDao.getAllTracksSync().associateBy { it.id }

        // Filter events belonging to same hour bucket (+/- 3 hours)
        val relevantEvents = allEvents.filter { ev ->
            val cal = java.util.Calendar.getInstance().apply { timeInMillis = ev.timestamp }
            val h = cal.get(java.util.Calendar.HOUR_OF_DAY)
            kotlin.math.abs(h - currentHour) <= 3 || kotlin.math.abs(h - currentHour) >= 21
        }

        val topIds = relevantEvents.groupingBy { it.trackId }.eachCount()
            .entries.sortedByDescending { it.value }.map { it.key }

        val tracks = topIds.mapNotNull { allTracksMap[it] }.take(limit)
        if (tracks.isNotEmpty()) {
            tracks
        } else {
            allTracksMap.values.shuffled().take(limit)
        }
    }

    suspend fun getAllPlaybackEvents(): List<PlaybackEvent> = withContext(Dispatchers.IO) {
        musicDao.getAllPlaybackEvents()
    }

    suspend fun checkMilestones(): List<String> = withContext(Dispatchers.IO) {
        val events = musicDao.getAllPlaybackEvents()
        val totalMs: Long = events.sumOf { it.durationListenedMs }
        val unlocked = mutableListOf("default")

        // 10 hours threshold for vinyl style unlock
        if (totalMs >= 10 * 3600 * 1000L || events.size >= 50) {
            unlocked.add("vinyl_turntable")
        }

        unlocked
    }

    suspend fun initDefaultDataIfNeeded() = withContext(Dispatchers.IO) {
        // Explicitly purge any demo/sample tracks to keep the library 100% clean with zero preloaded songs
        musicDao.deleteDemoTracks()
    }

    suspend fun scanDeviceMusic(): Int = withContext(Dispatchers.IO) {
        val tracksFound = mutableListOf<Track>()
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.YEAR,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.DATE_MODIFIED,
            MediaStore.Audio.Media.DATA
        )

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"

        try {
            context.contentResolver.query(
                collection,
                projection,
                selection,
                null,
                "${MediaStore.Audio.Media.DATE_ADDED} DESC"
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val durCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val yearCol = cursor.getColumnIndex(MediaStore.Audio.Media.YEAR)
                val albumIdCol = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID)
                val dateAddedCol = cursor.getColumnIndex(MediaStore.Audio.Media.DATE_ADDED)
                val dateModCol = cursor.getColumnIndex(MediaStore.Audio.Media.DATE_MODIFIED)
                val dataCol = cursor.getColumnIndex(MediaStore.Audio.Media.DATA)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val title = cursor.getString(titleCol) ?: "Unknown Track"
                    val artist = cursor.getString(artistCol) ?: "Unknown Artist"
                    val album = cursor.getString(albumCol) ?: "Unknown Album"
                    val duration = cursor.getLong(durCol)
                    val year = if (yearCol != -1) cursor.getInt(yearCol) else 2024
                    val albumId = if (albumIdCol != -1) cursor.getLong(albumIdCol) else -1L

                    val dateAddedSec = if (dateAddedCol != -1) cursor.getLong(dateAddedCol) else 0L
                    val dateModSec = if (dateModCol != -1) cursor.getLong(dateModCol) else 0L
                    val filePath = if (dataCol != -1) cursor.getString(dataCol) else null

                    val dateAddedMs = when {
                        dateAddedSec > 0L -> dateAddedSec * 1000L
                        dateModSec > 0L -> dateModSec * 1000L
                        !filePath.isNullOrBlank() -> {
                            val f = java.io.File(filePath)
                            if (f.exists() && f.lastModified() > 0L) f.lastModified() else System.currentTimeMillis()
                        }
                        else -> System.currentTimeMillis()
                    }

                    val contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id).toString()
                    val artworkUri = if (albumId > 0) {
                        ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), albumId).toString()
                    } else null

                    if (duration > 5000) { // Ignore tiny notification sounds
                        tracksFound.add(
                            Track(
                                id = "device_$id",
                                title = title,
                                artist = artist,
                                album = album,
                                durationMs = duration,
                                uri = contentUri,
                                artworkUri = artworkUri,
                                year = if (year > 0) year else 2024,
                                dateAdded = dateAddedMs,
                                isDemo = false
                            )
                        )
                    }
                }
            }

            if (tracksFound.isNotEmpty()) {
                musicDao.insertNewTracks(tracksFound)
                // Guarantee existing tracks also receive accurate dateAdded timestamps
                for (trk in tracksFound) {
                    musicDao.updateTrackDateAdded(trk.id, trk.dateAdded)
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to scan MediaStore: ${e.message}")
        }

        tracksFound.size
    }

    suspend fun toggleFavorite(trackId: String, currentFavorite: Boolean) = withContext(Dispatchers.IO) {
        musicDao.setFavorite(trackId, !currentFavorite)
    }

    suspend fun recordPlay(trackId: String) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        musicDao.recordPlay(trackId, now)
        // Keep at most 10 items in Recently Played, removing the oldest from the end
        try {
            val allRecent = musicDao.getAllRecentlyPlayedTracksSync()
            if (allRecent.size > 10) {
                for (i in 10 until allRecent.size) {
                    musicDao.clearTrackRecentTimestamp(allRecent[i].id)
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to prune recently played: ${e.message}")
        }
    }

    suspend fun recordPlaybackSession(
        track: Track,
        durationListenedMs: Long,
        wasSkipped: Boolean
    ) = withContext(Dispatchers.IO) {
        if (durationListenedMs >= 3000L) { // Only log meaningful listens (> 3 seconds)
            val event = PlaybackEvent(
                trackId = track.id,
                trackTitle = track.title,
                artist = track.artist,
                album = track.album,
                genre = track.genre,
                timestamp = System.currentTimeMillis(),
                durationListenedMs = durationListenedMs,
                fullTrackDurationMs = track.durationMs,
                wasSkipped = wasSkipped
            )
            musicDao.insertPlaybackEvent(event)
        }
    }

    suspend fun getAvailableWrappedPeriods(): List<WrappedPeriod> = withContext(Dispatchers.IO) {
        val firstTimestamp = musicDao.getFirstPlaybackTimestamp() ?: System.currentTimeMillis()
        WrappedEngine.getAvailablePeriods(firstTimestamp)
    }

    suspend fun getWrappedStats(period: WrappedPeriod): WrappedStats = withContext(Dispatchers.IO) {
        val events = musicDao.getAllPlaybackEvents()
        val allTracksList = musicDao.getAllTracks().firstOrNull() ?: emptyList()
        val firstTimestamp = musicDao.getFirstPlaybackTimestamp() ?: 0L
        WrappedEngine.computeStats(period, events, allTracksList, firstTimestamp)
    }

    suspend fun clearPlaybackHistory() = withContext(Dispatchers.IO) {
        musicDao.clearAllPlaybackEvents()
    }

    suspend fun updateTrackMetadata(track: Track) = withContext(Dispatchers.IO) {
        musicDao.updateTrack(track)
    }

    suspend fun deleteTrack(trackId: String) = withContext(Dispatchers.IO) {
        musicDao.deleteTrack(trackId)
        musicDao.deleteLyrics(trackId)
    }

    // Lyrics handling
    fun getLyrics(trackId: String): Flow<LyricsEntity?> = musicDao.getLyrics(trackId)

    suspend fun saveLyrics(trackId: String, rawLrc: String, offsetMs: Long = 0L) = withContext(Dispatchers.IO) {
        musicDao.insertLyrics(LyricsEntity(trackId = trackId, rawLrc = rawLrc, offsetMs = offsetMs, isUserEdited = true))
    }

    // Playlists
    suspend fun createPlaylist(name: String, description: String = "") = withContext(Dispatchers.IO) {
        val newId = "pl_${System.currentTimeMillis()}"
        musicDao.insertPlaylist(Playlist(id = newId, name = name, description = description, trackIds = emptyList()))
    }

    suspend fun createPlaylistWithTracks(name: String, trackIds: List<String>, description: String = "") = withContext(Dispatchers.IO) {
        val newId = "pl_${System.currentTimeMillis()}"
        musicDao.insertPlaylist(Playlist(id = newId, name = name, description = description, trackIds = trackIds))
        trackIds.forEachIndexed { index, trackId ->
            musicDao.insertPlaylistTrack(PlaylistTrackCrossRef(newId, trackId, index))
        }
    }

    suspend fun addTracksToPlaylist(playlistId: String, trackIds: List<String>) = withContext(Dispatchers.IO) {
        val existing = musicDao.getPlaylistById(playlistId) ?: return@withContext
        val newTrackIds = (existing.trackIds + trackIds).distinct()
        musicDao.insertPlaylist(existing.copy(trackIds = newTrackIds))
        val baseIndex = System.currentTimeMillis().toInt()
        trackIds.forEachIndexed { index, trackId ->
            musicDao.insertPlaylistTrack(PlaylistTrackCrossRef(playlistId, trackId, baseIndex + index))
        }
    }

    suspend fun renamePlaylist(playlistId: String, newName: String) = withContext(Dispatchers.IO) {
        val existing = musicDao.getPlaylistById(playlistId) ?: return@withContext
        musicDao.insertPlaylist(existing.copy(name = newName))
    }

    suspend fun deletePlaylist(playlistId: String) = withContext(Dispatchers.IO) {
        musicDao.deletePlaylist(playlistId)
    }

    suspend fun addTrackToPlaylist(playlistId: String, trackId: String) = withContext(Dispatchers.IO) {
        musicDao.insertPlaylistTrack(PlaylistTrackCrossRef(playlistId, trackId, System.currentTimeMillis().toInt()))
        val existing = musicDao.getPlaylistById(playlistId)
        if (existing != null && !existing.trackIds.contains(trackId)) {
            musicDao.insertPlaylist(existing.copy(trackIds = existing.trackIds + trackId))
        }
    }

    suspend fun removeTrackFromPlaylist(playlistId: String, trackId: String) = withContext(Dispatchers.IO) {
        musicDao.removeTrackFromPlaylist(playlistId, trackId)
        val existing = musicDao.getPlaylistById(playlistId)
        if (existing != null && existing.trackIds.contains(trackId)) {
            musicDao.insertPlaylist(existing.copy(trackIds = existing.trackIds - trackId))
        }
    }

    // Backup & Restore
    suspend fun exportBackupJson(): String = withContext(Dispatchers.IO) {
        val root = JSONObject()
        val allTracksList = musicDao.getAllTracks().firstOrNull() ?: emptyList()
        val allPlaylistsList = musicDao.getAllPlaylists().firstOrNull() ?: emptyList()

        val tracksArray = JSONArray()
        for (t in allTracksList) {
            val obj = JSONObject()
            obj.put("id", t.id)
            obj.put("title", t.title)
            obj.put("artist", t.artist)
            obj.put("album", t.album)
            obj.put("isFavorite", t.isFavorite)
            obj.put("playCount", t.playCount)
            tracksArray.put(obj)
        }
        root.put("tracks", tracksArray)

        val playlistsArray = JSONArray()
        for (p in allPlaylistsList) {
            val obj = JSONObject()
            obj.put("id", p.id)
            obj.put("name", p.name)
            obj.put("description", p.description)
            playlistsArray.put(obj)
        }
        root.put("playlists", playlistsArray)
        root.put("version", 1)
        root.put("exportedAt", System.currentTimeMillis())

        root.toString(2)
    }

    suspend fun importBackupJson(jsonString: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject(jsonString)
            if (root.has("playlists")) {
                val array = root.getJSONArray("playlists")
                for (i in 0 until array.length()) {
                    val item = array.getJSONObject(i)
                    val p = Playlist(
                        id = item.getString("id"),
                        name = item.getString("name"),
                        description = item.optString("description", "")
                    )
                    musicDao.insertPlaylist(p)
                }
            }
            true
        } catch (e: Exception) {
            Log.e(tag, "Import error", e)
            false
        }
    }

    private fun getSampleDemoTracks(): List<Track> {
        return listOf(
            Track(
                id = "aura_demo_01",
                title = "Midnight Resonance",
                artist = "Aura Sound Lab",
                album = "Neon Drift Horizon",
                durationMs = 214000L,
                uri = com.example.audio.DemoAudioGenerator.getOrCreateDemoAudio(context, "aura_demo_01").absolutePath,
                genre = "Synthwave / Cyberpunk",
                year = 2024,
                bitrate = 320,
                sampleRate = 48000,
                isFavorite = true,
                playCount = 12,
                isDemo = true
            ),
            Track(
                id = "aura_demo_02",
                title = "Halo Expansion",
                artist = "Kavinsky & Lorn",
                album = "Sub Bass Odyssey",
                durationMs = 188000L,
                uri = com.example.audio.DemoAudioGenerator.getOrCreateDemoAudio(context, "aura_demo_02").absolutePath,
                genre = "Dark Ambient",
                year = 2024,
                bitrate = 320,
                sampleRate = 44100,
                isFavorite = true,
                playCount = 8,
                isDemo = true
            ),
            Track(
                id = "aura_demo_03",
                title = "Aurora Borealis Pulse",
                artist = "Solar Fields",
                album = "Infinite Horizons",
                durationMs = 245000L,
                uri = com.example.audio.DemoAudioGenerator.getOrCreateDemoAudio(context, "aura_demo_03").absolutePath,
                genre = "Ambient Electronic",
                year = 2023,
                bitrate = 256,
                sampleRate = 44100,
                isFavorite = false,
                playCount = 5,
                isDemo = true
            ),
            Track(
                id = "aura_demo_04",
                title = "Quantum Echoes",
                artist = "Carbon Based Lifeforms",
                album = "Starlight Continuum",
                durationMs = 270000L,
                uri = com.example.audio.DemoAudioGenerator.getOrCreateDemoAudio(context, "aura_demo_04").absolutePath,
                genre = "Chillout / Downtempo",
                year = 2024,
                bitrate = 320,
                sampleRate = 48000,
                isFavorite = false,
                playCount = 3,
                isDemo = true
            ),
            Track(
                id = "aura_demo_05",
                title = "Velocity Zero",
                artist = "Tycho & Com Truise",
                album = "Galactic Sunset",
                durationMs = 195000L,
                uri = com.example.audio.DemoAudioGenerator.getOrCreateDemoAudio(context, "aura_demo_05").absolutePath,
                genre = "Electro Synth",
                year = 2024,
                bitrate = 320,
                sampleRate = 44100,
                isFavorite = false,
                playCount = 1,
                isDemo = true
            )
        )
    }

    private fun getSampleLrcForTrack(trackId: String, title: String, artist: String): String {
        return when (trackId) {
            "aura_demo_01" -> """
                [ti:$title]
                [ar:$artist]
                [00:04.00]<00:04.00>Night <00:04.80>falls <00:05.50>over <00:06.20>the <00:07.00>cyber <00:07.80>skyline
                [00:09.50]<00:09.50>Glowing <00:10.40>lights <00:11.20>reflect <00:12.10>in <00:13.00>distant <00:13.90>eyes
                [00:15.80]<00:15.80>Feel <00:16.50>the <00:17.30>resonance <00:18.80>deep <00:19.60>inside
                [00:21.50]<00:21.50>Synthesizer <00:22.80>rhythms <00:24.00>guiding <00:25.20>the <00:26.10>ride
                [00:28.00]<00:28.00>Echoes <00:29.00>of <00:29.80>midnight <00:31.00>never <00:32.20>die
                [00:35.00]<00:35.00>Bass <00:36.00>pulses <00:37.20>lifting <00:38.50>into <00:39.80>the <00:40.90>light
                [00:43.00]<00:43.00>We <00:43.80>are <00:44.50>the <00:45.30>sound <00:46.40>of <00:47.20>the <00:48.00>aurora
                [00:51.00]<00:51.00>Drifting <00:52.50>in <00:53.30>time <00:54.80>and <00:55.90>frequencies
                [00:59.00]<00:59.00>Endless <01:00.50>waves <01:02.00>across <01:03.50>the <01:05.00>galaxy
            """.trimIndent()
            "aura_demo_02" -> """
                [ti:$title]
                [ar:$artist]
                [00:05.20]<00:05.20>Sub-bass <00:06.50>waves <00:07.80>expand <00:09.20>in <00:10.00>the <00:11.10>dark
                [00:13.50]<00:13.50>Every <00:14.60>kick <00:15.80>a <00:16.40>radiant <00:17.90>spark
                [00:20.00]<00:20.00>The <00:20.80>halo <00:21.90>breathes <00:23.20>with <00:24.40>every <00:25.60>beat
                [00:28.00]<00:28.00>Sonic <00:29.40>frequency <00:31.00>alive <00:32.80>and <00:34.00>complete
                [00:37.50]<00:37.50>Step <00:38.50>inside <00:39.90>the <00:41.00>ambient <00:42.80>glow
                [00:45.00]<00:45.00>Where <00:46.00>the <00:47.00>harmonic <00:48.50>rivers <00:50.00>flow
            """.trimIndent()
            else -> """
                [ti:$title]
                [ar:$artist]
                [00:06.00]Floating on a sea of ambient sound
                [00:14.00]Lost in the harmonic frequencies we found
                [00:22.00]Aura illumination dancing around
                [00:30.00]Pure audio journey without a bound
                [00:38.00]Bassline breathing with each heartbeat
                [00:46.00]Visual spectrum now complete
            """.trimIndent()
        }
    }
}
