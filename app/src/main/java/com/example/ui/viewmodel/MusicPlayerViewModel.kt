package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.AuraApplication
import com.example.audio.AmbientPalette
import com.example.audio.ArtworkPaletteExtractor
import com.example.audio.AudioAnalysisData
import com.example.data.model.*
import com.example.lyrics.LrcParser
import com.example.service.AudioPlaybackService
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MusicPlayerViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as AuraApplication
    private val repository = app.repository
    private val audioEngine = app.audioEngine
    private val analysisEngine = app.audioAnalysisEngine

    // UI state
    val allTracks: StateFlow<List<Track>> = repository.allTracks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteTracks: StateFlow<List<Track>> = repository.favoriteTracks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentlyPlayed: StateFlow<List<Track>> = repository.recentlyPlayed
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val mostPlayed: StateFlow<List<Track>> = repository.mostPlayed
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentlyAdded: StateFlow<List<Track>> = repository.recentlyAdded
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val playlists: StateFlow<List<Playlist>> = repository.playlists
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val analysisData: StateFlow<AudioAnalysisData> = analysisEngine.analysisState

    // High-frequency playback position isolated to prevent recomposition cascades
    val currentPositionMs: StateFlow<Long> = audioEngine.currentPosition

    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private val _appSettings = MutableStateFlow(loadPersistedSettings())
    val appSettings: StateFlow<AppSettings> = _appSettings.asStateFlow()

    // Wrapped Statistics States
    private val _wrappedPeriods = MutableStateFlow<List<WrappedPeriod>>(emptyList())
    val wrappedPeriods: StateFlow<List<WrappedPeriod>> = _wrappedPeriods.asStateFlow()

    private val _selectedWrappedPeriod = MutableStateFlow<WrappedPeriod?>(null)
    val selectedWrappedPeriod: StateFlow<WrappedPeriod?> = _selectedWrappedPeriod.asStateFlow()

    private val _wrappedStats = MutableStateFlow<WrappedStats?>(null)
    val wrappedStats: StateFlow<WrappedStats?> = _wrappedStats.asStateFlow()

    private val _activePreset = MutableStateFlow(VisualizerPreset.SOFT)
    val activePreset: StateFlow<VisualizerPreset> = _activePreset.asStateFlow()

    private val _activePalette = MutableStateFlow(
        AmbientPalette(
            primary = Color(0xFF8B5CF6),
            secondary = Color(0xFF38BDF8),
            haloGlow = Color(0x338B5CF6),
            accent = Color(0xFFC084FC)
        )
    )
    val activePalette: StateFlow<AmbientPalette> = _activePalette.asStateFlow()

    private val _currentLyrics = MutableStateFlow<List<LyricsLine>>(emptyList())
    val currentLyrics: StateFlow<List<LyricsLine>> = _currentLyrics.asStateFlow()

    private val _currentLyricsEntity = MutableStateFlow<LyricsEntity?>(null)
    val currentLyricsEntity: StateFlow<LyricsEntity?> = _currentLyricsEntity.asStateFlow()

    // Screen presentation states
    val isNowPlayingExpanded = MutableStateFlow(false)
    val isImmersiveMode = MutableStateFlow(false)
    val showLyricsEditor = MutableStateFlow(false)
    val showEqualizer = MutableStateFlow(false)
    val showQueue = MutableStateFlow(false)
    val showSleepTimer = MutableStateFlow(false)
    val editingTrackMetadata = MutableStateFlow<Track?>(null)

    private var lyricsJob: Job? = null
    private var sleepTimerJob: Job? = null
    private var hasRestoredPlayback = false
    private var trackStartPlayTimestamp: Long = 0L
    private var activePlayingTrackRef: Track? = null

    init {
        // Apply persisted audio engine settings
        applySettingsToEngines(_appSettings.value)

        // Load wrapped periods
        refreshWrappedPeriods()

        // Wire audio engine callbacks
        audioEngine.onTrackCompleted = {
            handleTrackCompleted()
        }

        audioEngine.onError = { err ->
            _playbackState.update { it.copy(status = PlayerStatus.ERROR, errorMessage = err) }
            handleTrackError()
        }

        // Maintain duration and status
        viewModelScope.launch {
            audioEngine.duration.collect { dur ->
                _playbackState.update { it.copy(durationMs = dur) }
            }
        }

        viewModelScope.launch {
            audioEngine.status.collect { st ->
                val isPlaying = st == PlayerStatus.PLAYING
                analysisEngine.isPlaybackActive = isPlaying
                _playbackState.update { it.copy(status = st, isPlayWhenReady = audioEngine.isPlayWhenReady.value) }
                
                // Update system media notification
                val track = _playbackState.value.currentTrack
                if (track != null) {
                    AudioPlaybackService.update(
                        context = app,
                        title = track.title,
                        artist = track.artist,
                        album = track.album,
                        isPlaying = isPlaying,
                        durationMs = _playbackState.value.durationMs,
                        positionMs = audioEngine.currentPosition.value
                    )
                }
            }
        }

        viewModelScope.launch {
            audioEngine.isPlayWhenReady.collect { pwr ->
                _playbackState.update { it.copy(isPlayWhenReady = pwr) }
            }
        }

        viewModelScope.launch {
            audioEngine.audioSessionId.collect { sid ->
                analysisEngine.attachToAudioSession(sid)
            }
        }

        // Instant Resume: Check and restore last playback state once tracks load
        viewModelScope.launch {
            allTracks.collect { tracks ->
                if (!hasRestoredPlayback && tracks.isNotEmpty()) {
                    hasRestoredPlayback = true
                    restoreLastPlaybackIfAvailable(tracks)
                }
            }
        }

        // Notification & External system actions handler
        AudioPlaybackService.actionHandler = { action ->
            when (action) {
                "PLAY" -> play()
                "PAUSE" -> pause()
                "PLAY_PAUSE" -> togglePlayPause()
                "NEXT" -> nextTrack()
                "PREVIOUS" -> previousTrack()
                "STOP" -> pause()
                "HEADPHONES_DISCONNECTED" -> {
                    if (_appSettings.value.pauseOnHeadphoneDisconnect) {
                        pause()
                    }
                }
            }
        }

        AudioPlaybackService.seekHandler = { pos ->
            seekTo(pos)
        }
    }

    fun setAppForeground(isForeground: Boolean) {
        analysisEngine.isAppForeground = isForeground
    }

    private fun handleTrackError() {
        val state = _playbackState.value
        if (state.queue.size > 1) {
            // Auto skip corrupted track gracefully
            viewModelScope.launch {
                kotlinx.coroutines.delay(600L)
                nextTrack()
            }
        }
    }

    fun playTrack(track: Track, newQueue: List<Track>? = null, startPositionMs: Long = 0L) {
        val queue = newQueue ?: _playbackState.value.queue.ifEmpty { listOf(track) }
        val idx = queue.indexOfFirst { it.id == track.id }.coerceAtLeast(0)

        // Record previous track listen session if active
        val prevTrack = activePlayingTrackRef
        val startTs = trackStartPlayTimestamp
        if (prevTrack != null && startTs > 0L) {
            val listenedMs = (System.currentTimeMillis() - startTs).coerceAtLeast(0L)
            viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                repository.recordPlaybackSession(prevTrack, listenedMs, wasSkipped = true)
            }
        }

        activePlayingTrackRef = track
        trackStartPlayTimestamp = System.currentTimeMillis()

        // Immediately clear old lyrics so previous track words NEVER linger or flash
        _currentLyrics.value = emptyList()
        _currentLyricsEntity.value = null

        _playbackState.update {
            it.copy(
                currentTrack = track,
                queue = queue,
                queueIndex = idx,
                status = PlayerStatus.BUFFERING,
                isPlayWhenReady = true,
                currentPositionMs = startPositionMs,
                durationMs = track.durationMs
            )
        }

        // 1. Play in engine immediately with zero blocking
        audioEngine.playTrack(track, startPositionMs)

        // 2. Load lyrics strictly for this track
        loadLyricsForTrack(track.id)

        // 3. Extract palette and record metadata in background
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.Default) {
            val fallback = Color(_appSettings.value.customAccentColor)
            val palette = if (_appSettings.value.autoColorFromArtwork) {
                ArtworkPaletteExtractor.extract(track, fallback)
            } else {
                AmbientPalette(fallback, fallback.copy(alpha = 0.5f), fallback.copy(alpha = 0.25f), fallback)
            }
            _activePalette.value = palette

            repository.recordPlay(track.id)
            saveLastPlayback(track.id, queue.map { it.id }, startPositionMs)
            
            // Preload next track if enabled
            if (_appSettings.value.preloadNextTrack && queue.size > 1) {
                val nextIdx = (idx + 1) % queue.size
                queue.getOrNull(nextIdx)?.let { nextTrack ->
                    audioEngine.preloadTrack(nextTrack)
                }
            }

            // Refresh wrapped stats after record
            refreshCurrentWrappedStats()
        }

        AudioPlaybackService.update(
            context = app,
            title = track.title,
            artist = track.artist,
            album = track.album,
            isPlaying = true,
            durationMs = track.durationMs,
            positionMs = startPositionMs
        )
    }

    fun togglePlayPause() {
        val current = _playbackState.value
        if (current.currentTrack == null) {
            val first = allTracks.value.firstOrNull()
            if (first != null) playTrack(first, allTracks.value)
            return
        }
        audioEngine.togglePlayPause()
        
        // Save current position on pause
        if (_playbackState.value.isPlayWhenReady) {
            saveLastPlayback(
                trackId = current.currentTrack.id,
                queueIds = current.queue.map { it.id },
                positionMs = audioEngine.currentPosition.value
            )
        }
    }

    fun play() {
        if (_playbackState.value.currentTrack == null) {
            val first = allTracks.value.firstOrNull()
            if (first != null) playTrack(first, allTracks.value)
            return
        }
        audioEngine.play()
    }

    fun pause() {
        audioEngine.pause()
        _playbackState.value.currentTrack?.let { track ->
            saveLastPlayback(
                trackId = track.id,
                queueIds = _playbackState.value.queue.map { it.id },
                positionMs = audioEngine.currentPosition.value
            )
        }
    }

    fun nextTrack() {
        val state = _playbackState.value
        if (state.queue.isEmpty()) return

        val nextIndex = if (state.isShuffle) {
            state.queue.indices.random()
        } else {
            (state.queueIndex + 1) % state.queue.size
        }

        val next = state.queue.getOrNull(nextIndex) ?: return
        playTrack(next, state.queue)
    }

    fun previousTrack() {
        val state = _playbackState.value
        if (state.currentPositionMs > 3000L) {
            // Seek to beginning if already past 3s
            seekTo(0L)
            return
        }

        if (state.queue.isEmpty()) return
        val prevIndex = if (state.queueIndex - 1 < 0) state.queue.size - 1 else state.queueIndex - 1
        val prev = state.queue.getOrNull(prevIndex) ?: return
        playTrack(prev, state.queue)
    }

    private fun handleTrackCompleted() {
        val state = _playbackState.value
        val track = state.currentTrack
        val startTs = trackStartPlayTimestamp
        if (track != null && startTs > 0L) {
            val listenedMs = (System.currentTimeMillis() - startTs).coerceAtLeast(0L)
            viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                repository.recordPlaybackSession(track, listenedMs, wasSkipped = false)
            }
        }
        trackStartPlayTimestamp = 0L
        activePlayingTrackRef = null

        when (state.repeatMode) {
            RepeatMode.ONE -> {
                state.currentTrack?.let { playTrack(it, state.queue) }
            }
            RepeatMode.ALL -> {
                nextTrack()
            }
            RepeatMode.OFF -> {
                if (state.queueIndex + 1 < state.queue.size) {
                    nextTrack()
                } else {
                    audioEngine.pause()
                }
            }
        }
    }

    fun seekTo(positionMs: Long) {
        audioEngine.seekTo(positionMs)
        _playbackState.update { it.copy(currentPositionMs = positionMs) }
        _playbackState.value.currentTrack?.let { track ->
            saveLastPlayback(track.id, _playbackState.value.queue.map { it.id }, positionMs)
        }
    }

    fun setVolume(vol: Float) {
        audioEngine.setVolume(vol)
        _playbackState.update { it.copy(volume = vol, isMuted = vol == 0f) }
    }

    fun toggleShuffle() {
        _playbackState.update { it.copy(isShuffle = !it.isShuffle) }
    }

    fun cycleRepeatMode() {
        _playbackState.update {
            val next = when (it.repeatMode) {
                RepeatMode.OFF -> RepeatMode.ALL
                RepeatMode.ALL -> RepeatMode.ONE
                RepeatMode.ONE -> RepeatMode.OFF
            }
            it.copy(repeatMode = next)
        }
    }

    fun toggleFavorite(track: Track) {
        viewModelScope.launch {
            repository.toggleFavorite(track.id, track.isFavorite)
            if (_playbackState.value.currentTrack?.id == track.id) {
                _playbackState.update {
                    it.copy(currentTrack = it.currentTrack?.copy(isFavorite = !track.isFavorite))
                }
            }
        }
    }

    // Lyrics
    private fun loadLyricsForTrack(trackId: String) {
        lyricsJob?.cancel()
        lyricsJob = viewModelScope.launch {
            repository.getLyrics(trackId).collect { entity ->
                // Guard: verify track is still the currently active track
                if (_playbackState.value.currentTrack?.id != trackId) return@collect
                
                _currentLyricsEntity.value = entity
                if (entity != null && entity.rawLrc.isNotBlank()) {
                    val parsed = LrcParser.parse(entity.rawLrc, entity.offsetMs)
                    _currentLyrics.value = parsed
                } else {
                    _currentLyrics.value = emptyList()
                }
            }
        }
    }

    fun saveLyrics(trackId: String, rawLrc: String, offsetMs: Long = 0L) {
        viewModelScope.launch {
            repository.saveLyrics(trackId, rawLrc, offsetMs)
            _currentLyrics.value = LrcParser.parse(rawLrc, offsetMs)
        }
    }

    fun shiftLyricsOffset(deltaMs: Long) {
        val track = _playbackState.value.currentTrack ?: return
        val entity = _currentLyricsEntity.value ?: return
        val newOffset = entity.offsetMs + deltaMs
        saveLyrics(track.id, entity.rawLrc, newOffset)
    }

    // Queue actions
    fun playNextInQueue(track: Track) {
        val q = _playbackState.value.queue.toMutableList()
        val insertIdx = (_playbackState.value.queueIndex + 1).coerceAtMost(q.size)
        q.add(insertIdx, track)
        _playbackState.update { it.copy(queue = q) }
    }

    fun addToQueue(track: Track) {
        val q = _playbackState.value.queue.toMutableList()
        q.add(track)
        _playbackState.update { it.copy(queue = q) }
    }

    fun removeFromQueue(index: Int) {
        val state = _playbackState.value
        if (index in state.queue.indices) {
            val q = state.queue.toMutableList()
            q.removeAt(index)
            val newIdx = if (index < state.queueIndex) state.queueIndex - 1 else state.queueIndex
            _playbackState.update { it.copy(queue = q, queueIndex = newIdx.coerceAtLeast(0)) }
        }
    }

    fun reorderQueue(fromIndex: Int, toIndex: Int) {
        val state = _playbackState.value
        if (fromIndex in state.queue.indices && toIndex in state.queue.indices) {
            val q = state.queue.toMutableList()
            val item = q.removeAt(fromIndex)
            q.add(toIndex, item)
            var curIdx = state.queueIndex
            if (curIdx == fromIndex) curIdx = toIndex
            else if (fromIndex < curIdx && toIndex >= curIdx) curIdx--
            else if (fromIndex > curIdx && toIndex <= curIdx) curIdx++
            _playbackState.update { it.copy(queue = q, queueIndex = curIdx) }
        }
    }

    fun clearQueue() {
        val cur = _playbackState.value.currentTrack
        _playbackState.update {
            it.copy(
                queue = if (cur != null) listOf(cur) else emptyList(),
                queueIndex = 0
            )
        }
    }

    // Visualizer Controls & Presets
    fun setVisualizerMode(mode: VisualizerMode) {
        _appSettings.update { it.copy(visualizerMode = mode) }
    }

    fun setVisualizerPreset(preset: VisualizerPreset) {
        _activePreset.value = preset
        _appSettings.update {
            it.copy(
                visualizerMode = preset.mode,
                visualizerSensitivity = preset.sensitivity,
                visualizerBassResponse = preset.bassResponse,
                visualizerGlow = preset.glow,
                visualizerFps = preset.fps
            )
        }
        analysisEngine.sensitivity = preset.sensitivity
        analysisEngine.bassResponse = preset.bassResponse
        analysisEngine.kickResponse = preset.kickResponse
        analysisEngine.targetFps = preset.fps
    }

    fun updateSettings(newSettings: AppSettings) {
        _appSettings.value = newSettings
        savePersistedSettings(newSettings)
        applySettingsToEngines(newSettings)
    }

    private fun applySettingsToEngines(settings: AppSettings) {
        audioEngine.crossfadeSeconds = settings.crossfadeDurationSeconds
        audioEngine.gaplessEnabled = settings.gaplessEnabled
        audioEngine.setBassBoost(settings.bassBoostStrength)
        analysisEngine.sensitivity = settings.visualizerSensitivity
        analysisEngine.bassResponse = settings.visualizerBassResponse
        analysisEngine.targetFps = settings.visualizerFps
        analysisEngine.batterySaver = settings.batterySaver
    }

    private fun loadPersistedSettings(): AppSettings {
        val prefs = app.getSharedPreferences("aura_settings", Context.MODE_PRIVATE)
        val langStr = prefs.getString("language", AppLanguage.ENGLISH.name) ?: AppLanguage.ENGLISH.name
        val lang = try { AppLanguage.valueOf(langStr) } catch (e: Exception) { AppLanguage.ENGLISH }
        val themeStr = prefs.getString("theme", AppTheme.MIDNIGHT.name) ?: AppTheme.MIDNIGHT.name
        val theme = try { AppTheme.valueOf(themeStr) } catch (e: Exception) { AppTheme.MIDNIGHT }
        val modeStr = prefs.getString("viz_mode", VisualizerMode.AMBIENT_HALO.name) ?: VisualizerMode.AMBIENT_HALO.name
        val vizMode = try { VisualizerMode.valueOf(modeStr) } catch (e: Exception) { VisualizerMode.AMBIENT_HALO }

        return AppSettings(
            theme = theme,
            language = lang,
            visualizerMode = vizMode,
            visualizerSensitivity = prefs.getFloat("viz_sens", 1.0f),
            visualizerGlow = prefs.getFloat("viz_glow", 0.65f),
            visualizerFps = prefs.getInt("viz_fps", 60),
            autoColorFromArtwork = prefs.getBoolean("auto_color", true),
            customAccentColor = prefs.getLong("accent_color", 0xFF8B5CF6L),
            lyricsFontSize = prefs.getFloat("lyrics_size", 20.0f),
            lyricsKaraokeWordHighlight = prefs.getBoolean("karaoke_hl", true),
            crossfadeDurationSeconds = prefs.getInt("crossfade_sec", 0),
            gaplessEnabled = prefs.getBoolean("gapless", true),
            hapticFeedbackEnabled = prefs.getBoolean("haptic", true),
            pauseOnHeadphoneDisconnect = prefs.getBoolean("pause_disconnect", true),
            duckVolumeOnInterruption = prefs.getBoolean("duck_volume", true)
        )
    }

    private fun savePersistedSettings(settings: AppSettings) {
        val prefs = app.getSharedPreferences("aura_settings", Context.MODE_PRIVATE)
        prefs.edit()
            .putString("language", settings.language.name)
            .putString("theme", settings.theme.name)
            .putString("viz_mode", settings.visualizerMode.name)
            .putFloat("viz_sens", settings.visualizerSensitivity)
            .putFloat("viz_glow", settings.visualizerGlow)
            .putInt("viz_fps", settings.visualizerFps)
            .putBoolean("auto_color", settings.autoColorFromArtwork)
            .putLong("accent_color", settings.customAccentColor)
            .putFloat("lyrics_size", settings.lyricsFontSize)
            .putBoolean("karaoke_hl", settings.lyricsKaraokeWordHighlight)
            .putInt("crossfade_sec", settings.crossfadeDurationSeconds)
            .putBoolean("gapless", settings.gaplessEnabled)
            .putBoolean("haptic", settings.hapticFeedbackEnabled)
            .putBoolean("pause_disconnect", settings.pauseOnHeadphoneDisconnect)
            .putBoolean("duck_volume", settings.duckVolumeOnInterruption)
            .apply()
    }

    // Spotify-Wrapped Listen Stats
    fun refreshWrappedPeriods() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val periods = repository.getAvailableWrappedPeriods()
            _wrappedPeriods.value = periods
            if (_selectedWrappedPeriod.value == null && periods.isNotEmpty()) {
                selectWrappedPeriod(periods.first())
            }
        }
    }

    fun selectWrappedPeriod(period: WrappedPeriod) {
        _selectedWrappedPeriod.value = period
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val stats = repository.getWrappedStats(period)
            _wrappedStats.value = stats
        }
    }

    private fun refreshCurrentWrappedStats() {
        val period = _selectedWrappedPeriod.value
        if (period != null) {
            viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                val stats = repository.getWrappedStats(period)
                _wrappedStats.value = stats
            }
        }
    }

    fun clearPlaybackHistory() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            repository.clearPlaybackHistory()
            refreshWrappedPeriods()
            _wrappedStats.value = null
        }
    }

    fun setSleepTimer(minutes: Int) {
        sleepTimerJob?.cancel()
        _appSettings.update { it.copy(sleepTimerMinutes = minutes) }

        if (minutes > 0) {
            sleepTimerJob = viewModelScope.launch {
                kotlinx.coroutines.delay(minutes * 60 * 1000L)
                if (_appSettings.value.sleepTimerFadeOut) {
                    audioEngine.performFadeOut(durationSeconds = 3) {
                        _appSettings.update { it.copy(sleepTimerMinutes = 0) }
                    }
                } else {
                    audioEngine.pause()
                    _appSettings.update { it.copy(sleepTimerMinutes = 0) }
                }
            }
        }
    }

    // Playlists
    fun createPlaylist(name: String, description: String = "") {
        viewModelScope.launch {
            repository.createPlaylist(name, description)
        }
    }

    fun deletePlaylist(id: String) {
        viewModelScope.launch {
            repository.deletePlaylist(id)
        }
    }

    fun addTrackToPlaylist(playlistId: String, trackId: String) {
        viewModelScope.launch {
            repository.addTrackToPlaylist(playlistId, trackId)
        }
    }

    // Library Scan
    fun scanDeviceLibrary(onComplete: ((Int) -> Unit)? = null) {
        viewModelScope.launch {
            val count = repository.scanDeviceMusic()
            onComplete?.invoke(count)
        }
    }

    // Metadata update
    fun updateTrackMetadata(track: Track, newTitle: String, newArtist: String, newAlbum: String, newGenre: String, newYear: Int) {
        viewModelScope.launch {
            val updated = track.copy(
                title = newTitle,
                artist = newArtist,
                album = newAlbum,
                genre = newGenre,
                year = newYear
            )
            repository.updateTrackMetadata(updated)
            if (_playbackState.value.currentTrack?.id == track.id) {
                _playbackState.update { it.copy(currentTrack = updated) }
            }
        }
    }

    // Instant Resume persistence helpers
    private fun restoreLastPlaybackIfAvailable(tracks: List<Track>) {
        if (_playbackState.value.currentTrack != null || tracks.isEmpty()) return
        val prefs = app.getSharedPreferences("aura_playback_state", Context.MODE_PRIVATE)
        val lastTrackId = prefs.getString("last_track_id", null) ?: return
        val lastPos = prefs.getLong("last_pos_ms", 0L)
        val queueIdsStr = prefs.getString("last_queue_ids", "") ?: ""

        val track = tracks.firstOrNull { it.id == lastTrackId } ?: tracks.firstOrNull() ?: return
        val trackMap = tracks.associateBy { it.id }
        val restoredQueue = if (queueIdsStr.isNotBlank()) {
            queueIdsStr.split(",").mapNotNull { trackMap[it] }.ifEmpty { tracks }
        } else {
            tracks
        }
        val qIdx = restoredQueue.indexOfFirst { it.id == track.id }.coerceAtLeast(0)

        val fallback = Color(_appSettings.value.customAccentColor)
        val palette = if (_appSettings.value.autoColorFromArtwork) {
            ArtworkPaletteExtractor.extract(track, fallback)
        } else {
            AmbientPalette(fallback, fallback.copy(alpha = 0.5f), fallback.copy(alpha = 0.25f), fallback)
        }
        _activePalette.value = palette
        loadLyricsForTrack(track.id)

        _playbackState.update {
            it.copy(
                currentTrack = track,
                queue = restoredQueue,
                queueIndex = qIdx,
                status = PlayerStatus.PAUSED,
                isPlayWhenReady = false,
                currentPositionMs = lastPos,
                durationMs = track.durationMs
            )
        }
    }

    private fun saveLastPlayback(trackId: String, queueIds: List<String>, positionMs: Long) {
        val prefs = app.getSharedPreferences("aura_playback_state", Context.MODE_PRIVATE)
        prefs.edit()
            .putString("last_track_id", trackId)
            .putLong("last_pos_ms", positionMs)
            .putString("last_queue_ids", queueIds.joinToString(","))
            .apply()
    }

    // Backup & Restore
    suspend fun exportBackupJson(): String = repository.exportBackupJson()
    suspend fun importBackupJson(json: String): Boolean = repository.importBackupJson(json)
}
