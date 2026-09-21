package com.example.ui.viewmodel

import android.app.Application
import android.os.Handler
import android.os.Looper
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

    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private val _appSettings = MutableStateFlow(AppSettings())
    val appSettings: StateFlow<AppSettings> = _appSettings.asStateFlow()

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

    init {
        // Wire audio engine callbacks
        audioEngine.onTrackCompleted = {
            handleTrackCompleted()
        }

        audioEngine.onError = { err ->
            _playbackState.update { it.copy(status = PlayerStatus.ERROR, errorMessage = err) }
        }

        // Attach audio engine position to playbackState
        viewModelScope.launch {
            audioEngine.currentPosition.collect { pos ->
                _playbackState.update { it.copy(currentPositionMs = pos) }
            }
        }

        viewModelScope.launch {
            audioEngine.duration.collect { dur ->
                _playbackState.update { it.copy(durationMs = dur) }
            }
        }

        viewModelScope.launch {
            audioEngine.status.collect { st ->
                _playbackState.update { it.copy(status = st) }
                // Update notification
                val track = _playbackState.value.currentTrack
                if (track != null) {
                    AudioPlaybackService.update(
                        app,
                        track.title,
                        track.artist,
                        st == PlayerStatus.PLAYING
                    )
                }
            }
        }

        viewModelScope.launch {
            audioEngine.audioSessionId.collect { sid ->
                analysisEngine.attachToAudioSession(sid)
            }
        }

        // Notification actions handler
        AudioPlaybackService.actionHandler = { action ->
            when (action) {
                "PLAY_PAUSE" -> togglePlayPause()
                "NEXT" -> nextTrack()
                "PREVIOUS" -> previousTrack()
                "STOP" -> pause()
            }
        }
    }

    fun playTrack(track: Track, newQueue: List<Track>? = null) {
        val queue = newQueue ?: _playbackState.value.queue.ifEmpty { listOf(track) }
        val idx = queue.indexOfFirst { it.id == track.id }.coerceAtLeast(0)

        _playbackState.update {
            it.copy(
                currentTrack = track,
                queue = queue,
                queueIndex = idx,
                status = PlayerStatus.BUFFERING,
                currentPositionMs = 0L,
                durationMs = track.durationMs
            )
        }

        // Extract palette
        val fallback = Color(_appSettings.value.customAccentColor)
        val palette = if (_appSettings.value.autoColorFromArtwork) {
            ArtworkPaletteExtractor.extract(track, fallback)
        } else {
            AmbientPalette(fallback, fallback.copy(alpha = 0.5f), fallback.copy(alpha = 0.25f), fallback)
        }
        _activePalette.value = palette

        // Load lyrics for this track
        loadLyricsForTrack(track.id)

        // Play in engine
        audioEngine.playTrack(track)
        viewModelScope.launch {
            repository.recordPlay(track.id)
        }

        AudioPlaybackService.update(app, track.title, track.artist, true)
    }

    fun togglePlayPause() {
        val current = _playbackState.value
        if (current.status == PlayerStatus.PLAYING) {
            audioEngine.pause()
        } else {
            if (current.currentTrack == null) {
                val first = allTracks.value.firstOrNull()
                if (first != null) playTrack(first, allTracks.value)
            } else {
                audioEngine.play()
            }
        }
    }

    fun pause() {
        audioEngine.pause()
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
        audioEngine.crossfadeSeconds = newSettings.crossfadeDurationSeconds
        audioEngine.gaplessEnabled = newSettings.gaplessEnabled
        audioEngine.setBassBoost(newSettings.bassBoostStrength)
        analysisEngine.sensitivity = newSettings.visualizerSensitivity
        analysisEngine.bassResponse = newSettings.visualizerBassResponse
        analysisEngine.targetFps = newSettings.visualizerFps
    }

    fun setSleepTimer(minutes: Int) {
        sleepTimerJob?.cancel()
        _appSettings.update { it.copy(sleepTimerMinutes = minutes) }

        if (minutes > 0) {
            sleepTimerJob = viewModelScope.launch {
                kotlinx.coroutines.delay(minutes * 60 * 1000L)
                audioEngine.pause()
                _appSettings.update { it.copy(sleepTimerMinutes = 0) }
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

    // Backup & Restore
    suspend fun exportBackupJson(): String = repository.exportBackupJson()
    suspend fun importBackupJson(json: String): Boolean = repository.importBackupJson(json)
}
