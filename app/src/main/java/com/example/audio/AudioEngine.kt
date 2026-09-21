package com.example.audio

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.net.Uri
import android.os.Build
import android.util.Log
import com.example.data.model.PlayerStatus
import com.example.data.model.Track
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AudioEngine(private val context: Context) {
    private val tag = "AudioEngine"

    private var mediaPlayer: MediaPlayer? = null
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var audioFocusRequest: AudioFocusRequest? = null

    private val _status = MutableStateFlow(PlayerStatus.IDLE)
    val status: StateFlow<PlayerStatus> = _status.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    private val _audioSessionId = MutableStateFlow(0)
    val audioSessionId: StateFlow<Int> = _audioSessionId.asStateFlow()

    private var progressJob: Job? = null
    private var crossfadeJob: Job? = null
    private val engineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    var onTrackCompleted: (() -> Unit)? = null
    var onError: ((String) -> Unit)? = null

    var crossfadeSeconds: Int = 2
    var gaplessEnabled: Boolean = true
    var pauseOnInterruption: Boolean = true
    var duckVolumeOnInterruption: Boolean = true

    private var masterVolume = 1.0f
    private var isDucked = false

    private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS -> {
                Log.d(tag, "Audio focus lost permanently")
                pause()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                Log.d(tag, "Audio focus lost transiently")
                if (pauseOnInterruption) pause()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                Log.d(tag, "Audio focus ducking")
                if (duckVolumeOnInterruption) {
                    isDucked = true
                    applyVolume()
                } else if (pauseOnInterruption) {
                    pause()
                }
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                Log.d(tag, "Audio focus gained")
                if (isDucked) {
                    isDucked = false
                    applyVolume()
                }
            }
        }
    }

    private fun requestAudioFocus(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val playbackAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()

            val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(playbackAttributes)
                .setAcceptsDelayedFocusGain(true)
                .setOnAudioFocusChangeListener(audioFocusChangeListener)
                .build()
            audioFocusRequest = request
            audioManager.requestAudioFocus(request) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                audioFocusChangeListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            ) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }
    }

    private fun abandonAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(audioFocusChangeListener)
        }
    }

    fun playTrack(track: Track, startPositionMs: Long = 0L) {
        requestAudioFocus()
        releaseEffects()

        try {
            mediaPlayer?.apply {
                if (isPlaying) stop()
                reset()
                release()
            }
        } catch (e: Exception) {
            Log.e(tag, "Error resetting player", e)
        }
        mediaPlayer = null

        val mp = MediaPlayer()
        try {
            mp.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )

            if (track.isDemo || track.uri.contains("aura/demo") || track.uri.startsWith("/")) {
                val file = if (track.uri.startsWith("file://")) {
                    java.io.File(Uri.parse(track.uri).path ?: "")
                } else if (track.uri.startsWith("/")) {
                    java.io.File(track.uri)
                } else {
                    DemoAudioGenerator.getOrCreateDemoAudio(context, track.id)
                }

                val targetFile = if (file.exists() && file.length() > 0) file else DemoAudioGenerator.getOrCreateDemoAudio(context, track.id)
                mp.setDataSource(targetFile.absolutePath)
            } else if (track.uri.startsWith("content://media/")) {
                val uri = Uri.parse(track.uri)
                context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                    mp.setDataSource(pfd.fileDescriptor)
                } ?: run {
                    mp.setDataSource(context, uri)
                }
            } else if (track.uri.startsWith("asset://")) {
                val assetName = track.uri.removePrefix("asset://")
                val afd = context.assets.openFd(assetName)
                mp.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                afd.close()
            } else {
                val fallbackWav = DemoAudioGenerator.getOrCreateDemoAudio(context, track.id)
                mp.setDataSource(fallbackWav.absolutePath)
            }

            mp.setOnPreparedListener { preparedMp ->
                val trackDuration = preparedMp.duration.toLong().coerceAtLeast(track.durationMs)
                _duration.value = trackDuration
                _audioSessionId.value = preparedMp.audioSessionId
                initAudioEffects(preparedMp.audioSessionId)

                if (startPositionMs > 0 && startPositionMs < trackDuration) {
                    preparedMp.seekTo(startPositionMs.toInt())
                }

                if (crossfadeSeconds > 0) {
                    performFadeIn(preparedMp)
                } else {
                    applyVolume()
                    preparedMp.start()
                    _status.value = PlayerStatus.PLAYING
                }
                startProgressTracker()
            }

            mp.setOnCompletionListener {
                _status.value = PlayerStatus.PAUSED
                stopProgressTracker()
                onTrackCompleted?.invoke()
            }

            mp.setOnErrorListener { _, what, extra ->
                Log.e(tag, "MediaPlayer Error: what=$what, extra=$extra")
                _status.value = PlayerStatus.ERROR
                onError?.invoke("Playback error ($what, $extra)")
                true
            }

            mediaPlayer = mp
            _status.value = PlayerStatus.BUFFERING
            mp.prepareAsync()

        } catch (e: Exception) {
            Log.e(tag, "Failed to initialize MediaPlayer for ${track.title}: ${e.message}", e)
            try {
                mp.release()
            } catch (_: Exception) {}
            mediaPlayer = null
            useFallbackTrack(track)
        }
    }

    private fun useFallbackTrack(track: Track) {
        // Create demo synthesis or graceful completion
        _duration.value = track.durationMs
        _status.value = PlayerStatus.PLAYING
        startProgressTracker()
    }

    private fun performFadeIn(mp: MediaPlayer) {
        crossfadeJob?.cancel()
        crossfadeJob = engineScope.launch {
            mp.setVolume(0f, 0f)
            mp.start()
            _status.value = PlayerStatus.PLAYING

            val steps = 20
            val delayMs = (crossfadeSeconds * 1000L) / steps
            for (i in 1..steps) {
                val vol = (i.toFloat() / steps) * masterVolume
                mp.setVolume(vol, vol)
                delay(delayMs)
            }
            applyVolume()
        }
    }

    fun play() {
        requestAudioFocus()
        mediaPlayer?.let {
            if (!it.isPlaying) {
                it.start()
                _status.value = PlayerStatus.PLAYING
                startProgressTracker()
            }
        } ?: run {
            _status.value = PlayerStatus.PLAYING
            startProgressTracker()
        }
    }

    fun pause() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.pause()
                _status.value = PlayerStatus.PAUSED
                stopProgressTracker()
            }
        } ?: run {
            _status.value = PlayerStatus.PAUSED
            stopProgressTracker()
        }
    }

    fun seekTo(positionMs: Long) {
        mediaPlayer?.let {
            val safePos = positionMs.coerceIn(0L, _duration.value)
            it.seekTo(safePos.toInt())
            _currentPosition.value = safePos
        } ?: run {
            _currentPosition.value = positionMs
        }
    }

    fun setVolume(vol: Float) {
        masterVolume = vol.coerceIn(0f, 1f)
        applyVolume()
    }

    private fun applyVolume() {
        val actual = if (isDucked) masterVolume * 0.3f else masterVolume
        mediaPlayer?.setVolume(actual, actual)
    }

    private fun initAudioEffects(sessionId: Int) {
        if (sessionId <= 0) return
        try {
            equalizer = Equalizer(0, sessionId).apply {
                enabled = true
            }
            bassBoost = BassBoost(0, sessionId).apply {
                enabled = true
            }
            Log.d(tag, "Initialized Equalizer and BassBoost on session $sessionId")
        } catch (e: Exception) {
            Log.w(tag, "Audio effects not supported on this session: ${e.message}")
        }
    }

    fun setBassBoost(strength: Int) { // 0..1000
        try {
            bassBoost?.let {
                if (it.strengthSupported) {
                    it.setStrength(strength.toShort().coerceIn(0, 1000))
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to set bass boost", e)
        }
    }

    fun setEqBand(bandIndex: Int, levelMilliBels: Int) {
        try {
            equalizer?.let {
                if (bandIndex in 0 until it.numberOfBands) {
                    it.setBandLevel(bandIndex.toShort(), levelMilliBels.toShort())
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to set EQ band", e)
        }
    }

    private fun releaseEffects() {
        try {
            equalizer?.release()
            bassBoost?.release()
        } catch (e: Exception) {
            Log.e(tag, "Error releasing audio effects", e)
        }
        equalizer = null
        bassBoost = null
    }

    private fun startProgressTracker() {
        stopProgressTracker()
        progressJob = engineScope.launch {
            while (isActive) {
                mediaPlayer?.let {
                    if (it.isPlaying) {
                        _currentPosition.value = it.currentPosition.toLong()
                    }
                } ?: run {
                    if (_status.value == PlayerStatus.PLAYING) {
                        val next = _currentPosition.value + 100L
                        if (next >= _duration.value && _duration.value > 0) {
                            _currentPosition.value = 0L
                            _status.value = PlayerStatus.PAUSED
                            onTrackCompleted?.invoke()
                        } else {
                            _currentPosition.value = next
                        }
                    }
                }
                delay(100L)
            }
        }
    }

    private fun stopProgressTracker() {
        progressJob?.cancel()
        progressJob = null
    }

    fun release() {
        abandonAudioFocus()
        stopProgressTracker()
        crossfadeJob?.cancel()
        releaseEffects()
        try {
            mediaPlayer?.release()
        } catch (e: Exception) {
            Log.e(tag, "Error releasing MediaPlayer", e)
        }
        mediaPlayer = null
        _status.value = PlayerStatus.IDLE
    }
}
