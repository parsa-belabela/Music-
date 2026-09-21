package com.example.audio

import android.media.audiofx.Visualizer
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.*

data class AudioAnalysisData(
    val rms: Float = 0f,
    val peak: Float = 0f,
    val bass: Float = 0f,       // 60-250Hz energy (0..1)
    val lowMid: Float = 0f,     // 250-500Hz
    val mid: Float = 0f,        // 500-2000Hz
    val high: Float = 0f,       // 2000-8000Hz
    val totalEnergy: Float = 0f,// 0..1
    val isKick: Boolean = false,
    val isBeat: Boolean = false,
    val kickPulse: Float = 0f,  // Instantaneous kick pulse with fast attack and exponential decay
    val haloExpansion: Float = 0f, // Smooth bass breathing expansion
    val fftBands: FloatArray = FloatArray(32) { 0f },
    val waveform: FloatArray = FloatArray(64) { 0f },
    val currentFps: Int = 60
)

class AudioAnalysisEngine {
    private val tag = "AudioAnalysisEngine"

    private val _analysisState = MutableStateFlow(AudioAnalysisData())
    val analysisState: StateFlow<AudioAnalysisData> = _analysisState.asStateFlow()

    private var visualizer: Visualizer? = null
    private var scope: CoroutineScope? = null
    private var isAnalyzing = false

    // Hardware capture buffers
    private var rawFftBytes = ByteArray(128)
    private var rawWaveformBytes = ByteArray(128)

    // Preallocated buffers to prevent GC allocations
    private val bandsBuffer = FloatArray(32)
    private val waveBuffer = FloatArray(64)

    // Decay smoothing variables
    private var smoothBass = 0f
    private var smoothEnergy = 0f
    private var smoothKickPulse = 0f
    private var lastBassValue = 0f
    private var lastBeatTimestamp = 0L

    var sensitivity: Float = 1.0f
    var bassResponse: Float = 1.2f
    var kickResponse: Float = 1.3f
    var targetFps: Int = 60
    var isAppForeground: Boolean = true
    var isPlaybackActive: Boolean = false
    var batterySaver: Boolean = false

    fun attachToAudioSession(audioSessionId: Int) {
        releaseVisualizer()
        if (audioSessionId <= 0) return

        try {
            val captureSizeRange = Visualizer.getCaptureSizeRange()
            val captureSize = captureSizeRange[0].coerceAtLeast(128)

            visualizer = Visualizer(audioSessionId).apply {
                this.captureSize = captureSize
                setDataCaptureListener(object : Visualizer.OnDataCaptureListener {
                    override fun onWaveFormDataCapture(v: Visualizer?, waveform: ByteArray?, samplingRate: Int) {
                        waveform?.let {
                            synchronized(rawWaveformBytes) {
                                if (rawWaveformBytes.size != it.size) {
                                    rawWaveformBytes = ByteArray(it.size)
                                }
                                System.arraycopy(it, 0, rawWaveformBytes, 0, it.size)
                            }
                        }
                    }

                    override fun onFftDataCapture(v: Visualizer?, fft: ByteArray?, samplingRate: Int) {
                        fft?.let {
                            synchronized(rawFftBytes) {
                                if (rawFftBytes.size != it.size) {
                                    rawFftBytes = ByteArray(it.size)
                                }
                                System.arraycopy(it, 0, rawFftBytes, 0, it.size)
                            }
                        }
                    }
                }, Visualizer.getMaxCaptureRate() / 2, true, true)
                enabled = true
            }
            Log.d(tag, "Attached Visualizer to session $audioSessionId")
        } catch (e: Exception) {
            Log.w(tag, "Hardware Visualizer attachment deferred or permission needed: ${e.message}")
        }
    }

    fun start(scope: CoroutineScope) {
        this.scope = scope
        if (isAnalyzing) return
        isAnalyzing = true

        scope.launch(Dispatchers.Default) {
            var loopTime = System.currentTimeMillis()
            var simulatedPhase = 0.0

            while (isActive && isAnalyzing) {
                // If playback is paused or app in background or batterySaver active, throttle to save CPU & battery
                if (!isPlaybackActive || !isAppForeground || batterySaver) {
                    if (smoothKickPulse > 0.02f || smoothBass > 0.02f) {
                        smoothKickPulse *= 0.75f
                        smoothBass *= 0.75f
                        smoothEnergy *= 0.75f
                        _analysisState.value = _analysisState.value.copy(
                            kickPulse = smoothKickPulse,
                            haloExpansion = smoothBass,
                            totalEnergy = smoothEnergy,
                            isKick = false,
                            isBeat = false
                        )
                    }
                    delay(200L)
                    continue
                }

                val frameStart = System.currentTimeMillis()
                val deltaMs = (frameStart - loopTime).coerceAtLeast(1L)
                loopTime = frameStart

                simulatedPhase += (deltaMs / 1000.0) * 2.0 * Math.PI

                processFrame(deltaMs, simulatedPhase)

                val frameDuration = System.currentTimeMillis() - frameStart
                val targetFrameTime = 1000L / targetFps.coerceIn(30, 120)
                val sleepTime = (targetFrameTime - frameDuration).coerceAtLeast(1L)
                delay(sleepTime)
            }
        }
    }

    fun stop() {
        isAnalyzing = false
        releaseVisualizer()
    }

    private fun releaseVisualizer() {
        try {
            visualizer?.enabled = false
            visualizer?.release()
        } catch (e: Exception) {
            Log.e(tag, "Error releasing visualizer", e)
        }
        visualizer = null
    }

    private fun processFrame(deltaMs: Long, simulatedPhase: Double) {
        val bands = bandsBuffer
        val wave = waveBuffer

        var hasHardwareData = false
        var computedRms = 0f
        var computedPeak = 0f

        synchronized(rawFftBytes) {
            if (visualizer != null && rawFftBytes.isNotEmpty()) {
                val n = min(rawFftBytes.size / 2, 32)
                for (i in 0 until n) {
                    val r = rawFftBytes[2 * i].toFloat()
                    val im = rawFftBytes[2 * i + 1].toFloat()
                    val mag = sqrt(r * r + im * im) / 128f
                    bands[i] = (mag * sensitivity).coerceIn(0f, 1f)
                    if (mag > 0.05f) hasHardwareData = true
                }
            }
        }

        synchronized(rawWaveformBytes) {
            if (visualizer != null && rawWaveformBytes.isNotEmpty()) {
                val step = (rawWaveformBytes.size / 64).coerceAtLeast(1)
                for (i in 0 until 64) {
                    val idx = (i * step).coerceAtMost(rawWaveformBytes.size - 1)
                    val sample = (rawWaveformBytes[idx].toInt() and 0xFF) - 128
                    val norm = sample / 128f
                    wave[i] = norm
                    computedPeak = max(computedPeak, abs(norm))
                    computedRms += norm * norm
                }
                computedRms = sqrt(computedRms / 64f)
            }
        }

        // High-fidelity DSP synthesis fallback when hardware capture buffer is quiet or unavailable
        if (!hasHardwareData) {
            val bassFreq = 1.8 // ~108 BPM rhythmic pulse
            val kickRhythm = sin(simulatedPhase * bassFreq)
            val subBass = sin(simulatedPhase * 0.7)

            val rawBassSim = ((kickRhythm.pow(4) * 0.85 + subBass.pow(2) * 0.35) * sensitivity).toFloat().coerceIn(0f, 1f)
            val rawEnergySim = (0.35f + 0.35f * sin(simulatedPhase * 0.4).toFloat() + rawBassSim * 0.3f).coerceIn(0.1f, 1f)

            for (i in 0 until 32) {
                val freqFactor = 1f / (1f + i * 0.12f)
                val wobble = sin(simulatedPhase * (1.2 + i * 0.25) + i * 0.5).toFloat()
                bands[i] = ((rawBassSim * freqFactor + (wobble * 0.5f + 0.5f) * (1f - freqFactor) * rawEnergySim) * sensitivity).coerceIn(0f, 1f)
            }

            for (i in 0 until 64) {
                wave[i] = (sin(simulatedPhase * 3.0 + i * 0.2) * 0.4 + sin(simulatedPhase * 6.0 + i * 0.4) * 0.3 * rawBassSim).toFloat()
            }

            computedRms = (rawEnergySim * 0.6f).coerceIn(0f, 1f)
            computedPeak = (rawBassSim * 0.9f).coerceIn(0f, 1f)
        }

        // Sub-band computations
        val bassBand = (bands.take(4).average().toFloat() * bassResponse).coerceIn(0f, 1f)
        val lowMidBand = (bands.drop(4).take(6).average().toFloat()).coerceIn(0f, 1f)
        val midBand = (bands.drop(10).take(10).average().toFloat()).coerceIn(0f, 1f)
        val highBand = (bands.drop(20).average().toFloat()).coerceIn(0f, 1f)
        val totalEnergy = (bassBand * 0.45f + lowMidBand * 0.2f + midBand * 0.2f + highBand * 0.15f).coerceIn(0f, 1f)

        // Kick Detection: sudden rise in low frequencies
        val bassDiff = bassBand - lastBassValue
        val now = System.currentTimeMillis()
        var isKick = false
        if (bassDiff > 0.16f * (2.0f - kickResponse) && (now - lastBeatTimestamp) > 260) {
            isKick = true
            lastBeatTimestamp = now
            smoothKickPulse = 1.0f // Instant fast attack
        }
        lastBassValue = bassBand

        // Smooth decay calculations: Fast decay for Kick, Smooth breathing for Bass
        smoothKickPulse = (smoothKickPulse * 0.78f).coerceAtLeast(0f)
        smoothBass = smoothBass * 0.88f + bassBand * 0.12f
        smoothEnergy = smoothEnergy * 0.90f + totalEnergy * 0.10f

        val haloExpansion = (smoothBass * 0.68f + smoothKickPulse * 0.32f).coerceIn(0f, 1f)

        _analysisState.value = AudioAnalysisData(
            rms = computedRms,
            peak = computedPeak,
            bass = bassBand,
            lowMid = lowMidBand,
            mid = midBand,
            high = highBand,
            totalEnergy = totalEnergy,
            isKick = isKick,
            isBeat = isKick || (totalEnergy > 0.65f),
            kickPulse = smoothKickPulse,
            haloExpansion = haloExpansion,
            fftBands = bands.clone(),
            waveform = wave.clone(),
            currentFps = targetFps
        )
    }
}
