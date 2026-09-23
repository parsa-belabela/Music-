package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.Virtualizer
import android.net.Uri
import android.os.Build
import android.util.Log
import com.example.data.model.PlayerStatus
import com.example.data.model.Track
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.pow
import kotlin.math.roundToInt

data class ConnectedAudioDevice(
    val name: String,
    val isAirPods: Boolean,
    val isBluetooth: Boolean,
    val isHeadphones: Boolean
)

/**
 * Premium Liquid Glass Audio Engine:
 * - Single Source of Truth for playback intent via [isPlayWhenReady]
 * - Atomic transition guard via [transitionId] preventing race conditions on rapid Next/Prev
 * - Soft, artifact-free audio transitions (gentle curve crossfade, no clicks/pops)
 * - Safe lifecycle management for MediaPlayer & audio effects
 * - Accurate progress tracking decoupled from UI recomposition
 */
class AudioEngine(private val context: Context) {
    private val tag = "AudioEngine"

    private var mediaPlayer: MediaPlayer? = null
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var audioFocusRequest: AudioFocusRequest? = null

    private val _status = MutableStateFlow(PlayerStatus.IDLE)
    val status: StateFlow<PlayerStatus> = _status.asStateFlow()

    private val _isPlayWhenReady = MutableStateFlow(false)
    val isPlayWhenReady: StateFlow<Boolean> = _isPlayWhenReady.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    private val _audioSessionId = MutableStateFlow(0)
    val audioSessionId: StateFlow<Int> = _audioSessionId.asStateFlow()

    private val _connectedDevice = MutableStateFlow<ConnectedAudioDevice?>(null)
    val connectedDevice: StateFlow<ConnectedAudioDevice?> = _connectedDevice.asStateFlow()

    private var isEqEnabled: Boolean = true
    private var currentEqBands: List<Float> = listOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f)
    private var currentBassBoostStrength: Int = 0
    var currentPlaybackSpeed: Float = 1.0f
        private set
    var continuousMixEnabled: Boolean = false

    private var deviceCallback: AudioDeviceCallback? = null

    init {
        initAudioDeviceMonitor()
    }

    private fun initAudioDeviceMonitor() {
        try {
            updateConnectedAudioDevices()
            val callback = object : AudioDeviceCallback() {
                override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) {
                    updateConnectedAudioDevices()
                }

                override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {
                    updateConnectedAudioDevices()
                }
            }
            audioManager.registerAudioDeviceCallback(callback, null)
            deviceCallback = callback
        } catch (e: Exception) {
            Log.w(tag, "AudioDeviceCallback registration failed: ${e.message}")
        }
    }

    private fun updateConnectedAudioDevices() {
        try {
            val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            var detected: ConnectedAudioDevice? = null

            for (device in devices) {
                val type = device.type
                val name = device.productName?.toString()?.trim() ?: ""
                val isAirPods = name.contains("AirPods", ignoreCase = true) || name.contains("AirPod", ignoreCase = true)
                val isBluetooth = type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                        type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                        (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && (type == AudioDeviceInfo.TYPE_BLE_HEADSET || type == AudioDeviceInfo.TYPE_BLE_SPEAKER)) ||
                        name.contains("Bluetooth", ignoreCase = true) ||
                        name.contains("Buds", ignoreCase = true) ||
                        name.contains("Beats", ignoreCase = true) ||
                        name.contains("WH-", ignoreCase = true) ||
                        name.contains("WF-", ignoreCase = true)

                val isHeadphones = type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                        type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                        type == AudioDeviceInfo.TYPE_USB_HEADSET

                if (isAirPods) {
                    detected = ConnectedAudioDevice(
                        name = if (name.isNotBlank()) name else "AirPods",
                        isAirPods = true,
                        isBluetooth = true,
                        isHeadphones = true
                    )
                    break
                } else if (isBluetooth && detected == null) {
                    detected = ConnectedAudioDevice(
                        name = if (name.isNotBlank()) name else "Bluetooth Audio",
                        isAirPods = false,
                        isBluetooth = true,
                        isHeadphones = true
                    )
                } else if (isHeadphones && detected == null) {
                    detected = ConnectedAudioDevice(
                        name = if (name.isNotBlank()) name else "Headphones",
                        isAirPods = false,
                        isBluetooth = false,
                        isHeadphones = true
                    )
                }
            }
            _connectedDevice.value = detected
        } catch (e: Exception) {
            Log.w(tag, "Failed checking audio devices: ${e.message}")
        }
    }

    private var progressJob: Job? = null
    private var fadeJob: Job? = null
    private val engineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val transitionId = AtomicLong(0L)
    @Volatile private var isPlayerPrepared = false
    private var preloadedPlayer: MediaPlayer? = null
    private var preloadedTrackId: String? = null

    var onTrackCompleted: (() -> Unit)? = null
    var onError: ((String) -> Unit)? = null

    var crossfadeSeconds: Int = 1
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

    var currentActiveTrack: Track? = null
        private set

    val isPlayerReady: Boolean
        get() = mediaPlayer != null && isPlayerPrepared

    val currentTrackId: String?
        get() = currentActiveTrack?.id

    fun prepareTrack(track: Track, startPositionMs: Long = 0L) {
        playTrack(track, startPositionMs, autoPlay = false)
    }

    fun playTrack(track: Track, startPositionMs: Long = 0L, autoPlay: Boolean = true) {
        val currentTransitionId = transitionId.incrementAndGet()
        currentActiveTrack = track
        _isPlayWhenReady.value = autoPlay
        isPlayerPrepared = false
        fadeJob?.cancel()
        fadeJob = null

        if (autoPlay) {
            requestAudioFocus()
        }
        releaseEffects()

        // Safely release previous player
        val oldPlayer = mediaPlayer
        mediaPlayer = null
        if (oldPlayer != null) {
            try {
                if (oldPlayer.isPlaying) oldPlayer.stop()
                oldPlayer.reset()
                oldPlayer.release()
            } catch (e: Exception) {
                Log.w(tag, "Error releasing previous player", e)
            }
        }

        _status.value = if (autoPlay) PlayerStatus.BUFFERING else PlayerStatus.PAUSED
        _currentPosition.value = startPositionMs

        // Check if track was preloaded
        val preloaded = preloadedPlayer
        if (preloaded != null && preloadedTrackId == track.id) {
            preloadedPlayer = null
            preloadedTrackId = null
            try {
                mediaPlayer = preloaded
                isPlayerPrepared = true
                val trackDuration = preloaded.duration.toLong().coerceAtLeast(track.durationMs)
                _duration.value = trackDuration
                _audioSessionId.value = preloaded.audioSessionId
                initAudioEffects(preloaded.audioSessionId)

                if (currentPlaybackSpeed != 1.0f) {
                    try {
                        val params = preloaded.playbackParams
                        params.speed = currentPlaybackSpeed
                        preloaded.playbackParams = params
                    } catch (e: Exception) {
                        Log.w(tag, "Failed applying playback speed to preloaded player: ${e.message}")
                    }
                }

                preloaded.setOnCompletionListener {
                    if (transitionId.get() != currentTransitionId) return@setOnCompletionListener
                    _status.value = PlayerStatus.PAUSED
                    _isPlayWhenReady.value = false
                    stopProgressTracker()
                    onTrackCompleted?.invoke()
                }

                preloaded.setOnErrorListener { _, what, extra ->
                    if (transitionId.get() != currentTransitionId) return@setOnErrorListener true
                    Log.e(tag, "Preloaded Player Error: what=$what, extra=$extra")
                    _status.value = PlayerStatus.ERROR
                    _isPlayWhenReady.value = false
                    stopProgressTracker()
                    onError?.invoke("Playback error ($what, $extra)")
                    true
                }

                if (startPositionMs > 0 && startPositionMs < trackDuration) {
                    preloaded.seekTo(startPositionMs.toInt())
                }

                if (_isPlayWhenReady.value) {
                    performSoftFadeIn(preloaded, currentTransitionId)
                } else {
                    _status.value = PlayerStatus.PAUSED
                    stopProgressTracker()
                }
                return
            } catch (e: Exception) {
                Log.w(tag, "Failed using preloaded player, creating fresh instance", e)
                try { preloaded.release() } catch (_: Exception) {}
                mediaPlayer = null
                isPlayerPrepared = false
            }
        }

        val mp = MediaPlayer()
        try {
            mp.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )

            setDataSourceForTrack(mp, track)

            mp.setOnPreparedListener { preparedMp ->
                // Stale callback check: If transition has advanced, discard this player
                if (transitionId.get() != currentTransitionId) {
                    Log.d(tag, "Discarding stale prepared player for track ${track.title}")
                    try {
                        preparedMp.stop()
                        preparedMp.reset()
                        preparedMp.release()
                    } catch (_: Exception) {}
                    return@setOnPreparedListener
                }

                isPlayerPrepared = true
                val trackDuration = preparedMp.duration.toLong().coerceAtLeast(track.durationMs)
                _duration.value = trackDuration
                _audioSessionId.value = preparedMp.audioSessionId
                initAudioEffects(preparedMp.audioSessionId)

                if (currentPlaybackSpeed != 1.0f) {
                    try {
                        val params = preparedMp.playbackParams
                        params.speed = currentPlaybackSpeed
                        preparedMp.playbackParams = params
                    } catch (e: Exception) {
                        Log.w(tag, "Failed applying playback speed to player: ${e.message}")
                    }
                }

                if (startPositionMs > 0 && startPositionMs < trackDuration) {
                    preparedMp.seekTo(startPositionMs.toInt())
                }

                if (_isPlayWhenReady.value) {
                    performSoftFadeIn(preparedMp, currentTransitionId)
                } else {
                    _status.value = PlayerStatus.PAUSED
                    stopProgressTracker()
                }
            }

            mp.setOnCompletionListener {
                if (transitionId.get() != currentTransitionId) return@setOnCompletionListener
                _status.value = PlayerStatus.PAUSED
                _isPlayWhenReady.value = false
                stopProgressTracker()
                onTrackCompleted?.invoke()
            }

            mp.setOnErrorListener { _, what, extra ->
                if (transitionId.get() != currentTransitionId) return@setOnErrorListener true
                Log.e(tag, "MediaPlayer Error: what=$what, extra=$extra for track ${track.title}")
                _status.value = PlayerStatus.ERROR
                _isPlayWhenReady.value = false
                stopProgressTracker()
                onError?.invoke("Playback error ($what, $extra)")
                true
            }

            mediaPlayer = mp
            mp.prepareAsync()

        } catch (e: Exception) {
            Log.e(tag, "Failed to initialize MediaPlayer for ${track.title}: ${e.message}", e)
            try {
                mp.release()
            } catch (_: Exception) {}
            mediaPlayer = null
            isPlayerPrepared = false
            _status.value = PlayerStatus.ERROR
            _isPlayWhenReady.value = false
            onError?.invoke("Cannot open audio: ${e.localizedMessage ?: "File missing"}")
        }
    }

    private fun setDataSourceForTrack(mp: MediaPlayer, track: Track) {
        if (track.isDemo || track.uri.contains("aura/demo") || track.uri.startsWith("/")) {
            val file = if (track.uri.startsWith("file://")) {
                File(Uri.parse(track.uri).path ?: "")
            } else if (track.uri.startsWith("/")) {
                File(track.uri)
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
    }

    fun preloadTrack(track: Track) {
        if (preloadedTrackId == track.id && preloadedPlayer != null) return
        try {
            preloadedPlayer?.release()
        } catch (_: Exception) {}
        preloadedPlayer = null
        preloadedTrackId = null

        val mp = MediaPlayer()
        try {
            mp.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            setDataSourceForTrack(mp, track)

            mp.setOnPreparedListener {
                preloadedPlayer = mp
                preloadedTrackId = track.id
                Log.d(tag, "Successfully preloaded track: ${track.title}")
            }
            mp.setOnErrorListener { _, _, _ ->
                try { mp.release() } catch (_: Exception) {}
                preloadedPlayer = null
                preloadedTrackId = null
                true
            }
            mp.prepareAsync()
        } catch (e: Exception) {
            Log.w(tag, "Failed preloading track ${track.title}: ${e.message}")
            try { mp.release() } catch (_: Exception) {}
        }
    }

    /**
     * Soft, silky audio fade-in that prevents pops and creates a cinematic entrance
     */
    private fun performSoftFadeIn(mp: MediaPlayer, currentTransitionId: Long) {
        fadeJob?.cancel()
        val targetVolume = if (isDucked) masterVolume * 0.3f else masterVolume
        
        try {
            mp.setVolume(0.01f, 0.01f)
            mp.start()
            _status.value = PlayerStatus.PLAYING
            startProgressTracker()
        } catch (e: Exception) {
            Log.e(tag, "Error starting player in performSoftFadeIn", e)
            return
        }

        fadeJob = engineScope.launch {
            val steps = 16
            val totalDurationMs = 300L
            val stepDelay = totalDurationMs / steps

            for (i in 1..steps) {
                if (transitionId.get() != currentTransitionId || !_isPlayWhenReady.value) break
                val t = i.toFloat() / steps
                // Equal-power fade in curve: sin(pi/2 * t)
                val equalPowerCurve = kotlin.math.sin((Math.PI / 2.0) * t).toFloat()
                val currentVol = equalPowerCurve * targetVolume
                try {
                    mp.setVolume(currentVol, currentVol)
                } catch (_: Exception) {}
                delay(stepDelay)
            }
            if (transitionId.get() == currentTransitionId && _isPlayWhenReady.value) {
                applyVolume()
            }
        }
    }

    fun play() {
        _isPlayWhenReady.value = true
        requestAudioFocus()
        val mp = mediaPlayer
        if (mp != null && isPlayerPrepared) {
            try {
                if (!mp.isPlaying) {
                    applyVolume()
                    mp.start()
                }
                _status.value = PlayerStatus.PLAYING
                startProgressTracker()
            } catch (e: Exception) {
                Log.e(tag, "Error in play()", e)
            }
        } else if (currentActiveTrack != null) {
            playTrack(currentActiveTrack!!, _currentPosition.value, autoPlay = true)
        } else {
            // Player is still buffering/preparing, it will start automatically in onPrepared
            _status.value = PlayerStatus.BUFFERING
        }
    }

    fun pause() {
        _isPlayWhenReady.value = false
        fadeJob?.cancel()
        fadeJob = null
        val mp = mediaPlayer
        if (mp != null && isPlayerPrepared) {
            try {
                if (mp.isPlaying) {
                    mp.pause()
                }
            } catch (e: Exception) {
                Log.e(tag, "Error in pause()", e)
            }
        }
        _status.value = PlayerStatus.PAUSED
        stopProgressTracker()
    }

    fun togglePlayPause() {
        if (_isPlayWhenReady.value) {
            pause()
        } else {
            play()
        }
    }

    fun seekTo(positionMs: Long) {
        val safePos = positionMs.coerceIn(0L, _duration.value.coerceAtLeast(positionMs))
        _currentPosition.value = safePos
        val mp = mediaPlayer
        if (mp != null && isPlayerPrepared) {
            try {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    mp.seekTo(safePos, android.media.MediaPlayer.SEEK_CLOSEST)
                } else {
                    mp.seekTo(safePos.toInt())
                }
                if (_isPlayWhenReady.value && mp.isPlaying) {
                    startProgressTracker()
                }
            } catch (e: Exception) {
                Log.e(tag, "Error seeking to $safePos", e)
            }
        }
    }

    fun setVolume(vol: Float) {
        masterVolume = vol.coerceIn(0f, 1f)
        applyVolume()
    }

    /**
     * Calculates audiophile headroom attenuation to prevent digital clipping when EQ or BassBoost are boosted.
     * When any band is raised above 0dB, attenuation is applied smoothly so the DAC never clips.
     */
    private fun getHeadroomScale(): Float {
        if (!isEqEnabled) return 1.0f
        val maxBandBoostDb = currentEqBands.maxOrNull()?.coerceAtLeast(0f) ?: 0f
        val bbBoostDb = if (currentBassBoostStrength > 0) (currentBassBoostStrength / 1000f) * 6.0f else 0f
        val totalPeakBoost = maxOf(maxBandBoostDb, bbBoostDb)
        if (totalPeakBoost <= 0.5f) return 1.0f

        val attenuationDb = totalPeakBoost * 0.85f
        return (10.0.pow(-attenuationDb.toDouble() / 20.0)).toFloat().coerceIn(0.25f, 1.0f)
    }

    private fun applyVolume() {
        val base = if (isDucked) masterVolume * 0.3f else masterVolume
        val actual = (base * getHeadroomScale()).coerceIn(0f, 1f)
        try {
            mediaPlayer?.setVolume(actual, actual)
        } catch (_: Exception) {}
    }

    fun performFadeOut(durationSeconds: Int = 2, onComplete: () -> Unit) {
        fadeJob?.cancel()
        val currentMp = mediaPlayer ?: run {
            onComplete()
            return
        }
        fadeJob = engineScope.launch {
            val steps = 24
            val delayMs = (durationSeconds * 1000L) / steps
            val headroom = getHeadroomScale()
            for (i in steps downTo 0) {
                val t = i.toFloat() / steps
                // Equal-power fade out curve: sin(pi/2 * t)
                val equalPowerFactor = kotlin.math.sin((Math.PI / 2.0) * t).toFloat()
                val vol = equalPowerFactor * masterVolume * headroom
                try {
                    currentMp.setVolume(vol, vol)
                } catch (_: Exception) {}
                delay(delayMs)
            }
            pause()
            applyVolume()
            onComplete()
        }
    }

    private fun initAudioEffects(sessionId: Int) {
        if (sessionId <= 0) return
        try {
            equalizer = Equalizer(0, sessionId).apply {
                enabled = isEqEnabled
            }
            bassBoost = BassBoost(0, sessionId).apply {
                enabled = true
            }
            try {
                virtualizer = Virtualizer(0, sessionId).apply {
                    enabled = true
                }
            } catch (ve: Exception) {
                Log.w(tag, "Virtualizer not supported on device: ${ve.message}")
            }
            applyCurrentEffects()
            Log.d(tag, "Initialized Equalizer, BassBoost, and Virtualizer on session $sessionId")
        } catch (e: Exception) {
            Log.w(tag, "Audio effects not supported on this session: ${e.message}")
        }
    }

    fun applyEqualizerSettings(enabled: Boolean, bands: List<Float>, bassBoostStrength: Int) {
        isEqEnabled = enabled
        currentEqBands = bands
        currentBassBoostStrength = bassBoostStrength
        applyCurrentEffects()
        applyVolume()
    }

    /**
     * Maps user 10-band graphic EQ (-12dB to +12dB) to hardware bands using logarithmic frequency interpolation.
     */
    private fun calculateGainForFreq(freqHz: Float, gains: List<Float>): Float {
        if (gains.isEmpty()) return 0f
        val nominalFreqs = floatArrayOf(31f, 62f, 125f, 250f, 500f, 1000f, 2000f, 4000f, 8000f, 16000f)
        if (freqHz <= nominalFreqs.first()) return gains.first()
        if (freqHz >= nominalFreqs.last()) return gains.last()

        for (i in 0 until nominalFreqs.size - 1) {
            val f1 = nominalFreqs[i]
            val f2 = nominalFreqs[i + 1]
            if (freqHz in f1..f2) {
                val log1 = kotlin.math.ln(f1.toDouble())
                val log2 = kotlin.math.ln(f2.toDouble())
                val logFreq = kotlin.math.ln(freqHz.toDouble())
                val fraction = ((logFreq - log1) / (log2 - log1)).toFloat().coerceIn(0f, 1f)
                val g1 = gains.getOrElse(i) { 0f }
                val g2 = gains.getOrElse(i + 1) { 0f }
                return g1 + fraction * (g2 - g1)
            }
        }
        return gains.last()
    }

    private fun applyCurrentEffects() {
        try {
            bassBoost?.let { bb ->
                if (bb.strengthSupported) {
                    bb.enabled = isEqEnabled && currentBassBoostStrength > 0
                    bb.setStrength(currentBassBoostStrength.toShort().coerceIn(0, 1000))
                }
            }
        } catch (e: Exception) {
            Log.w(tag, "Failed applying bass boost: ${e.message}")
        }

        try {
            equalizer?.let { eq ->
                eq.enabled = isEqEnabled
                val numBands = eq.numberOfBands.toInt()
                if (numBands > 0) {
                    val range = eq.bandLevelRange
                    val minLevel = range[0]
                    val maxLevel = range[1]

                    for (band in 0 until numBands) {
                        val centerFreqMilliHz = eq.getCenterFreq(band.toShort())
                        val centerFreqHz = (centerFreqMilliHz / 1000f).coerceAtLeast(20f)
                        val gainDb = calculateGainForFreq(centerFreqHz, currentEqBands)
                        val targetMilliBels = (gainDb * 100).roundToInt().coerceIn(minLevel.toInt(), maxLevel.toInt())
                        eq.setBandLevel(band.toShort(), targetMilliBels.toShort())
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(tag, "Failed applying equalizer bands: ${e.message}")
        }
    }

    fun setPlaybackSpeed(speed: Float) {
        val targetSpeed = speed.coerceIn(0.5f, 2.0f)
        // If speed has not changed, do nothing!
        if (targetSpeed == currentPlaybackSpeed) {
            return
        }
        currentPlaybackSpeed = targetSpeed
        val mp = mediaPlayer
        try {
            if (mp != null && isPlayerPrepared) {
                val wasPlaying = _isPlayWhenReady.value && mp.isPlaying
                val params = mp.playbackParams
                params.speed = currentPlaybackSpeed
                mp.playbackParams = params
                // MediaPlayer.setPlaybackParams() in Android inherently resumes/starts playback.
                // If playback was stopped/paused, restore paused state immediately!
                if (!wasPlaying) {
                    mp.pause()
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed setting playback speed: ${e.message}", e)
        }
    }

    fun setBassBoost(strength: Int) { // 0..1000
        currentBassBoostStrength = strength
        applyCurrentEffects()
        applyVolume()
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
        } catch (_: Exception) {}
        try {
            bassBoost?.release()
        } catch (_: Exception) {}
        try {
            virtualizer?.release()
        } catch (_: Exception) {}
        equalizer = null
        bassBoost = null
        virtualizer = null
    }

    private fun startProgressTracker() {
        stopProgressTracker()
        progressJob = engineScope.launch {
            var anchorPos = _currentPosition.value
            var anchorNano = System.nanoTime()
            var lastRealSyncMs = System.currentTimeMillis()

            val mp = mediaPlayer
            if (mp != null && isPlayerPrepared) {
                try {
                    val pos = mp.currentPosition.toLong()
                    if (pos >= 0L) {
                        anchorPos = pos
                        anchorNano = System.nanoTime()
                        _currentPosition.value = anchorPos
                    }
                } catch (_: Exception) {}
            }

            while (isActive) {
                val currentMp = mediaPlayer
                if (currentMp != null && isPlayerPrepared && _isPlayWhenReady.value) {
                    try {
                        val nowNano = System.nanoTime()
                        val nowMs = System.currentTimeMillis()

                        // Poll real MediaPlayer position every 60ms with smooth anti-drift convergence
                        if (nowMs - lastRealSyncMs >= 60L) {
                            if (currentMp.isPlaying) {
                                val realPos = currentMp.currentPosition.toLong()
                                val currentEstimated = (anchorPos + ((nowNano - anchorNano) / 1_000_000L * currentPlaybackSpeed).toLong())
                                val drift = realPos - currentEstimated

                                // If drift is large (> 250ms like seek or stall), snap immediately
                                if (kotlin.math.abs(drift) > 250L) {
                                    anchorPos = realPos
                                } else {
                                    // Softly blend 40% towards real hardware position to eliminate any micro-stutter
                                    anchorPos = (currentEstimated + (drift * 0.4f).toLong())
                                }
                                anchorNano = nowNano
                                lastRealSyncMs = nowMs
                                _currentPosition.value = anchorPos.coerceIn(0L, _duration.value.coerceAtLeast(anchorPos))
                            }
                        } else {
                            // Sub-millisecond smooth interpolation between MediaPlayer polls
                            val elapsedSinceAnchor = ((nowNano - anchorNano) / 1_000_000L * currentPlaybackSpeed).toLong()
                            val interpolatedPos = (anchorPos + elapsedSinceAnchor).coerceIn(0L, _duration.value.coerceAtLeast(anchorPos))
                            _currentPosition.value = interpolatedPos
                        }
                    } catch (_: Exception) {}
                }
                delay(16L)
            }
        }
    }

    private fun stopProgressTracker() {
        progressJob?.cancel()
        progressJob = null
    }

    fun release() {
        abandonAudioFocus()
        try {
            deviceCallback?.let { audioManager.unregisterAudioDeviceCallback(it) }
        } catch (_: Exception) {}
        deviceCallback = null
        stopProgressTracker()
        fadeJob?.cancel()
        fadeJob = null
        releaseEffects()
        try {
            mediaPlayer?.release()
        } catch (e: Exception) {
            Log.e(tag, "Error releasing MediaPlayer", e)
        }
        mediaPlayer = null
        try {
            preloadedPlayer?.release()
        } catch (_: Exception) {}
        preloadedPlayer = null
        preloadedTrackId = null
        _status.value = PlayerStatus.IDLE
        _isPlayWhenReady.value = false
    }
}
