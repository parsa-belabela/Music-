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
import com.example.data.model.RepeatMode
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
    var beatFocusEnabled: Boolean = false
        private set
    var vocalCrystalEnabled: Boolean = false
        private set
    var smartSilenceTrimEnabled: Boolean = true
    var currentPlaybackSpeed: Float = 1.0f
        private set
    var continuousMixEnabled: Boolean = false

    // A-B Beat Looper State
    private val _loopStartMs = MutableStateFlow<Long?>(null)
    val loopStartMs: StateFlow<Long?> = _loopStartMs.asStateFlow()

    private val _loopEndMs = MutableStateFlow<Long?>(null)
    val loopEndMs: StateFlow<Long?> = _loopEndMs.asStateFlow()

    // Smart Silence-Trimming DJ Mix State
    private var activeWaveformEnvelope: FloatArray? = null
    private var smartTrimTargetMs: Long? = null
    private var smartTrimTriggeredForCurrentTrack: Boolean = false

    fun setAbLoop(startMs: Long?, endMs: Long?) {
        _loopStartMs.value = startMs
        _loopEndMs.value = endMs
    }

    fun clearAbLoop() {
        _loopStartMs.value = null
        _loopEndMs.value = null
    }

    fun setTrackWaveformEnvelope(envelope: FloatArray?) {
        activeWaveformEnvelope = envelope
        recalculateSmartTrimPoint()
    }

    private fun recalculateSmartTrimPoint() {
        val env = activeWaveformEnvelope
        val dur = _duration.value
        if (env == null || env.isEmpty() || dur < 25000L) {
            smartTrimTargetMs = null
            return
        }
        // Scan backwards from the end of the waveform (within the last ~15% / max 14 seconds)
        // to find where amplitude drops below 15% (< 0.15f) and stays quiet
        val n = env.size
        var firstQuietIdx = n
        val minScanIdx = (n * 0.84f).toInt().coerceIn(0, n - 1)
        for (i in (n - 1) downTo minScanIdx) {
            if (env[i] < 0.155f) {
                firstQuietIdx = i
            } else {
                break
            }
        }
        if (firstQuietIdx < n) {
            val trimFraction = firstQuietIdx.toFloat() / n.toFloat()
            val candidateMs = (dur * trimFraction).toLong()
            // Clamp so we never trim more than 12 seconds or less than 800ms before the end
            val earliestAllowed = (dur - 12000L).coerceAtLeast(dur / 2)
            val latestAllowed = (dur - 750L).coerceAtLeast(earliestAllowed)
            smartTrimTargetMs = candidateMs.coerceIn(earliestAllowed, latestAllowed)
        } else {
            smartTrimTargetMs = null
        }
    }

    private var deviceCallback: AudioDeviceCallback? = null

    // Seek stabilization & queueing state
    private var isSeekingInProgress = false
    private var pendingSeekPos: Long? = null
    private var lastSeekTimestamp = 0L
    private var seekWatchdogJob: Job? = null

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
                    val hadExternal = _connectedDevice.value != null
                    updateConnectedAudioDevices()
                    val hasExternalNow = _connectedDevice.value != null
                    if (hadExternal && !hasExternalNow && (_status.value == PlayerStatus.PLAYING || _isPlayWhenReady.value)) {
                        Log.d(tag, "External audio device (Headphones/Bluetooth/Speaker) disconnected during playback -> PAUSING IMMEDIATELY")
                        pause()
                    }
                }
            }
            audioManager.registerAudioDeviceCallback(callback, null)
            deviceCallback = callback

            val noisyReceiver = object : android.content.BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: android.content.Intent?) {
                    if (intent?.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
                        Log.d(tag, "ACTION_AUDIO_BECOMING_NOISY received -> PAUSING IMMEDIATELY to prevent internal speaker leak")
                        pause()
                    }
                }
            }
            context.registerReceiver(
                noisyReceiver,
                android.content.IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
            )
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

    var repeatMode: RepeatMode = RepeatMode.OFF
        private set

    fun setRepeatMode(mode: RepeatMode) {
        repeatMode = mode
        try {
            mediaPlayer?.isLooping = (mode == RepeatMode.ONE)
        } catch (_: Exception) {}
    }

    fun setPlaybackSpeed(speed: Float) {
        val safeSpeed = speed.coerceIn(0.5f, 2.0f)
        currentPlaybackSpeed = safeSpeed
        try {
            mediaPlayer?.let { mp ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && isPlayerPrepared) {
                    val params = mp.playbackParams
                    params.speed = safeSpeed
                    mp.playbackParams = params
                }
            }
        } catch (e: Exception) {
            Log.w(tag, "Failed setting playback speed: ${e.message}")
        }
    }

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
        _duration.value = track.durationMs.coerceAtLeast(1L)
        _loopStartMs.value = null
        _loopEndMs.value = null
        smartTrimTriggeredForCurrentTrack = false
        recalculateSmartTrimPoint()
        isSeekingInProgress = false
        pendingSeekPos = null
        seekWatchdogJob?.cancel()
        seekWatchdogJob = null

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

                preloaded.isLooping = (repeatMode == RepeatMode.ONE)

                preloaded.setOnCompletionListener {
                    if (transitionId.get() != currentTransitionId) return@setOnCompletionListener
                    if (repeatMode == RepeatMode.ONE) {
                        try {
                            preloaded.seekTo(0)
                            preloaded.start()
                            _status.value = PlayerStatus.PLAYING
                            _isPlayWhenReady.value = true
                            startProgressTracker()
                        } catch (_: Exception) {}
                        return@setOnCompletionListener
                    }
                    _status.value = PlayerStatus.PAUSED
                    _isPlayWhenReady.value = false
                    stopProgressTracker()
                    onTrackCompleted?.invoke()
                }

                preloaded.setOnSeekCompleteListener {
                    seekWatchdogJob?.cancel()
                    val nextSeek = pendingSeekPos
                    pendingSeekPos = null
                    if (nextSeek != null) {
                        dispatchSeek(nextSeek)
                    } else {
                        isSeekingInProgress = false
                        lastSeekTimestamp = System.currentTimeMillis()
                        if (_isPlayWhenReady.value && preloaded.isPlaying) {
                            startProgressTracker()
                        }
                    }
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

                preparedMp.isLooping = (repeatMode == RepeatMode.ONE)

                if (_isPlayWhenReady.value) {
                    performSoftFadeIn(preparedMp, currentTransitionId)
                } else {
                    _status.value = PlayerStatus.PAUSED
                    stopProgressTracker()
                }
            }

            mp.setOnCompletionListener {
                if (transitionId.get() != currentTransitionId) return@setOnCompletionListener
                if (repeatMode == RepeatMode.ONE) {
                    try {
                        mp.seekTo(0)
                        mp.start()
                        _status.value = PlayerStatus.PLAYING
                        _isPlayWhenReady.value = true
                        startProgressTracker()
                    } catch (_: Exception) {}
                    return@setOnCompletionListener
                }
                _status.value = PlayerStatus.PAUSED
                _isPlayWhenReady.value = false
                stopProgressTracker()
                onTrackCompleted?.invoke()
            }

            mp.setOnSeekCompleteListener {
                seekWatchdogJob?.cancel()
                val nextSeek = pendingSeekPos
                pendingSeekPos = null
                if (nextSeek != null) {
                    dispatchSeek(nextSeek)
                } else {
                    isSeekingInProgress = false
                    lastSeekTimestamp = System.currentTimeMillis()
                    if (_isPlayWhenReady.value && mp.isPlaying) {
                        startProgressTracker()
                    }
                }
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
                DemoAudioGenerator.getOrCreateDemoAudio(context, track.id, track.durationMs)
            }
            val targetFile = if (file.exists() && file.length() > 0) file else DemoAudioGenerator.getOrCreateDemoAudio(context, track.id, track.durationMs)
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
            val fallbackWav = DemoAudioGenerator.getOrCreateDemoAudio(context, track.id, track.durationMs)
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
        val maxDur = _duration.value.coerceAtLeast(1L)
        // جلوگیری از ارسال نقطه ۱۰۰٪ انتهایی در حین کشیدن نوار که باعث پریدن به آهنگ بعدی می‌شود
        val safeClampMax = if (maxDur > 1500L) maxDur - 800L else maxDur
        val safePos = positionMs.coerceIn(0L, safeClampMax)
        _currentPosition.value = safePos
        val mp = mediaPlayer
        if (mp != null && isPlayerPrepared) {
            dispatchSeek(safePos)
        }
    }

    private fun dispatchSeek(safePos: Long) {
        val mp = mediaPlayer ?: return
        if (!isPlayerPrepared) return
        isSeekingInProgress = true
        lastSeekTimestamp = System.currentTimeMillis()
        seekWatchdogJob?.cancel()

        try {
            val mpDur = try { mp.duration.toLong() } catch (_: Exception) { -1L }
            val maxAllowed = if (mpDur > 1200L) mpDur - 600L else mpDur.coerceAtLeast(1L)
            val clampedPos = if (mpDur > 0) safePos.coerceIn(0L, maxAllowed) else safePos
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                mp.seekTo(clampedPos, android.media.MediaPlayer.SEEK_CLOSEST)
            } else {
                mp.seekTo(clampedPos.toInt())
            }
            seekWatchdogJob = engineScope.launch {
                delay(450L)
                isSeekingInProgress = false
            }
            if (_isPlayWhenReady.value && mp.isPlaying) {
                startProgressTracker()
            }
        } catch (e: Exception) {
            isSeekingInProgress = false
            Log.e(tag, "Error seeking to $safePos", e)
        }
    }

    fun setVolume(vol: Float) {
        masterVolume = vol.coerceIn(0f, 1f)
        applyVolume()
    }

    private var loudnessEnhancer: android.media.audiofx.LoudnessEnhancer? = null

    /**
     * حفظ قدرت و پانچ واقعی صدا بدون تضعیف خودکار بیس
     * فقط در صورت بوست بسیار شدید (+10dB به بالا) یک محافظ نرم اعمال میشود تا صدا دیستورت نشود
     */
    private fun getHeadroomScale(): Float {
        if (!isEqEnabled) return 1.0f
        val avgBoost = currentEqBands.filter { it > 0f }.average().toFloat().let { if (it.isNaN()) 0f else it }
        return if (avgBoost > 9.5f) 0.92f else 1.0f
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
            equalizer = Equalizer(100, sessionId).apply {
                enabled = isEqEnabled
            }
        } catch (e: Exception) {
            Log.w(tag, "Equalizer init warning: ${e.message}")
        }

        try {
            bassBoost = BassBoost(100, sessionId).apply {
                enabled = isEqEnabled && currentBassBoostStrength > 0
            }
        } catch (e: Exception) {
            Log.w(tag, "BassBoost init warning: ${e.message}")
        }

        try {
            loudnessEnhancer = android.media.audiofx.LoudnessEnhancer(sessionId).apply {
                enabled = isEqEnabled
            }
        } catch (e: Exception) {
            Log.w(tag, "LoudnessEnhancer init warning: ${e.message}")
        }

        try {
            virtualizer = Virtualizer(100, sessionId).apply {
                enabled = isEqEnabled
            }
        } catch (_: Exception) {}

        applyCurrentEffects()
    }

    fun applyEqualizerSettings(
        enabled: Boolean,
        bands: List<Float>,
        bassBoostStrength: Int,
        beatFocus: Boolean = beatFocusEnabled,
        vocalCrystal: Boolean = vocalCrystalEnabled
    ) {
        isEqEnabled = enabled
        currentEqBands = bands
        currentBassBoostStrength = bassBoostStrength.coerceIn(0, 1000)
        beatFocusEnabled = beatFocus
        vocalCrystalEnabled = vocalCrystal
        applyCurrentEffects()
        applyVolume()
    }

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

    /**
     * اعمال واقعی و قدرتمند اکولایزر ۱۰ بانده + تقویت سابووفر (Sub-Bass Shelf) + Loudness DSP
     */
    private fun applyCurrentEffects() {
        val bassNormalized = (currentBassBoostStrength / 1000f).coerceIn(0f, 1f)

        // ۱. اعمال سختافزاری BassBoost با حداکثر اولویت
        try {
            bassBoost?.let { bb ->
                bb.enabled = isEqEnabled && currentBassBoostStrength > 0
                if (bb.strengthSupported) {
                    bb.setStrength(currentBassBoostStrength.toShort().coerceIn(0, 1000))
                }
            }
        } catch (e: Exception) {
            Log.w(tag, "Failed applying bass boost: ${e.message}")
        }

        // ۲. اعمال باندهای اکولایزر + تزریق Sub-Bass Shelf واقعی روی فرکانسهای زیر 160Hz
        try {
            equalizer?.let { eq ->
                eq.enabled = isEqEnabled
                val numBands = eq.numberOfBands.toInt()
                if (numBands > 0) {
                    val range = eq.bandLevelRange
                    val minLevel = range[0].toInt()
                    val maxLevel = range[1].toInt()

                    for (band in 0 until numBands) {
                        val centerFreqMilliHz = eq.getCenterFreq(band.toShort())
                        val centerFreqHz = (centerFreqMilliHz / 1000f).coerceAtLeast(20f)
                        var gainDb = calculateGainForFreq(centerFreqHz, currentEqBands)

                        // وقتی کاربر اسلایدر Bass Boost را بالا میبرد، علاوه بر BassBoost سختافزاری،
                        // فرکانسهای ساببیس (31Hz تا 150Hz) نیز با منحنی Shelf تقویت میشوند تا بیس کاملاً کوبنده و عمیق شود
                        if (isEqEnabled && bassNormalized > 0f && centerFreqHz <= 180f) {
                            val subBassShelfFactor = ((180f - centerFreqHz) / 150f).coerceIn(0f, 1f)
                            gainDb += (bassNormalized * 9.5f * subBassShelfFactor)
                        }

                        // استودیو: تمرکز روی بیت و 808 (808 & Beat Focus) - تضعیف ملایم وکال میانی و تقویت پانچ کیک و ساب‌بیس
                        if (beatFocusEnabled) {
                            when {
                                centerFreqHz <= 150f -> gainDb += 4.2f
                                centerFreqHz in 420f..2400f -> gainDb -= 3.0f
                                centerFreqHz >= 7500f -> gainDb += 2.2f
                            }
                        }

                        // استودیو: شفاف‌سازی وکال (Vocal Crystal) - تقویت باند حضور صدای خواننده (1kHz تا 4kHz) و شفافیت بالا
                        if (vocalCrystalEnabled) {
                            when {
                                centerFreqHz in 160f..320f -> gainDb -= 1.6f
                                centerFreqHz in 900f..4500f -> gainDb += 4.5f
                                centerFreqHz > 4500f -> gainDb += 2.0f
                            }
                        }

                        // مقیاسدهی دقیق به میلیبل سختافزار
                        val targetMilliBels = (gainDb * 125f).roundToInt().coerceIn(minLevel, maxLevel)
                        eq.setBandLevel(band.toShort(), targetMilliBels.toShort())
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(tag, "Failed applying equalizer bands: ${e.message}")
        }

        // ۳. اعمال LoudnessEnhancer برای جبران افت انرژی و ایجاد پانچ استودیویی
        try {
            loudnessEnhancer?.let { le ->
                le.enabled = isEqEnabled
                if (isEqEnabled) {
                    val positiveEqBoost = currentEqBands.filter { it > 0f }.maxOrNull() ?: 0f
                    // محاسبه گین بر حسب میلیبل (mB) برای تفکیک و قدرت صدای خروجی
                    val targetGainMb = ((bassNormalized * 450f) + (positiveEqBoost * 35f)).roundToInt().coerceIn(0, 900)
                    le.setTargetGain(targetGainMb)
                } else {
                    le.setTargetGain(0)
                }
            }
        } catch (e: Exception) {
            Log.w(tag, "LoudnessEnhancer error: ${e.message}")
        }

        // ۴. ایجاد فضای سهبعدی ملایم متناسب با بیس
        try {
            virtualizer?.let { virt ->
                virt.enabled = isEqEnabled && currentBassBoostStrength > 100
                if (virt.strengthSupported) {
                    virt.setStrength((bassNormalized * 650f).toInt().toShort().coerceIn(0, 1000))
                }
            }
        } catch (_: Exception) {}
    }

    private fun releaseEffects() {
        try { equalizer?.release() } catch (_: Exception) {}
        try { bassBoost?.release() } catch (_: Exception) {}
        try { loudnessEnhancer?.release() } catch (_: Exception) {}
        try { virtualizer?.release() } catch (_: Exception) {}
        equalizer = null
        bassBoost = null
        loudnessEnhancer = null
        virtualizer = null
    }

    private fun startProgressTracker() {
        stopProgressTracker()
        progressJob = engineScope.launch {
            while (isActive) {
                val currentMp = mediaPlayer
                if (currentMp != null && isPlayerPrepared && _isPlayWhenReady.value) {
                    try {
                        val nowMs = System.currentTimeMillis()
                        val inSeekWindow = isSeekingInProgress && (nowMs - lastSeekTimestamp < 500L)
                        if (!inSeekWindow) {
                            if (isSeekingInProgress && (nowMs - lastSeekTimestamp >= 500L)) {
                                isSeekingInProgress = false
                            }
                            if (currentMp.isPlaying) {
                                val realPos = currentMp.currentPosition.toLong()
                                if (realPos >= 0L) {
                                    val safeMax = _duration.value.coerceAtLeast(realPos)
                                    _currentPosition.value = realPos.coerceIn(0L, safeMax)

                                    // 1. A-B Beat Looper enforcement
                                    val loopA = _loopStartMs.value
                                    val loopB = _loopEndMs.value
                                    if (loopA != null && loopB != null && loopB > loopA + 350L && realPos >= loopB) {
                                        seekTo(loopA)
                                    } else if (
                                        // 2. Smart Silence-Trimming DJ Mix enforcement
                                        smartSilenceTrimEnabled &&
                                        loopB == null &&
                                        repeatMode != RepeatMode.ONE &&
                                        !smartTrimTriggeredForCurrentTrack
                                    ) {
                                        val trimPoint = smartTrimTargetMs
                                        if (trimPoint != null && realPos >= trimPoint) {
                                            smartTrimTriggeredForCurrentTrack = true
                                            Log.d(tag, "Smart Silence-Trimming DJ Mix triggered at ${realPos}ms (duration=${_duration.value}ms)")
                                            onTrackCompleted?.invoke()
                                        }
                                    }
                                }
                            }
                        }
                    } catch (_: Exception) {}
                }
                delay(35L)
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
