package com.example.audio

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.Virtualizer
import android.util.Log

/**
 * مدیریت مرکزی و سخت‌افزاری افکت‌های صوتی اندروید (Equalizer, BassBoost, Virtualizer, LoudnessEnhancer)
 * قابل اتصال به هر audioSessionId چه در پلیر اصلی (MediaPlayer) و چه در پلیر استریم آنلاین (ExoPlayer)
 */
object AuraDspController {
    private const val TAG = "AuraDspController"

    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null
    private var boundSessionId: Int = 0

    var isEnabled: Boolean = true
    var bandGainsDb = FloatArray(5) { 0f }
    var bassStrength: Float = 0f
    var virtualizerStrength: Float = 0f
    var loudnessStrength: Float = 0f

    fun bindToAudioSession(audioSessionId: Int) {
        if (audioSessionId <= 0) return
        if (boundSessionId == audioSessionId && equalizer != null) {
            applyCurrentState()
            return
        }
        releaseEffects()
        boundSessionId = audioSessionId

        try {
            equalizer = Equalizer(0, audioSessionId).apply { enabled = isEnabled }
        } catch (e: Exception) {
            Log.w(TAG, "Equalizer init error: ${e.message}")
        }
        try {
            bassBoost = BassBoost(0, audioSessionId).apply { enabled = isEnabled }
        } catch (e: Exception) {
            Log.w(TAG, "BassBoost init error: ${e.message}")
        }
        try {
            virtualizer = Virtualizer(0, audioSessionId).apply { enabled = isEnabled }
        } catch (e: Exception) {
            Log.w(TAG, "Virtualizer init error: ${e.message}")
        }
        try {
            loudnessEnhancer = LoudnessEnhancer(audioSessionId).apply { enabled = isEnabled }
        } catch (e: Exception) {
            Log.w(TAG, "LoudnessEnhancer init error: ${e.message}")
        }
        applyCurrentState()
    }

    fun applyCurrentState() {
        try {
            equalizer?.let { eq ->
                eq.enabled = isEnabled
                val range = eq.bandLevelRange
                val minMb = range[0].toInt()
                val maxMb = range[1].toInt()
                val numBands = eq.numberOfBands.toInt()
                for (b in 0 until numBands) {
                    val idx = b.coerceAtMost(bandGainsDb.size - 1)
                    val db = if (isEnabled) bandGainsDb[idx] else 0f
                    val targetMb = (db * 100f).toInt().coerceIn(minMb, maxMb).toShort()
                    eq.setBandLevel(b.toShort(), targetMb)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Equalizer apply error: ${e.message}")
        }

        try {
            bassBoost?.let { bb ->
                bb.enabled = isEnabled
                if (bb.strengthSupported) {
                    val s = if (isEnabled) (bassStrength.coerceIn(0f, 1f) * 1000f).toInt().toShort() else 0
                    bb.setStrength(s)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "BassBoost apply error: ${e.message}")
        }

        try {
            virtualizer?.let { virt ->
                virt.enabled = isEnabled
                if (virt.strengthSupported) {
                    val s = if (isEnabled) (virtualizerStrength.coerceIn(0f, 1f) * 1000f).toInt().toShort() else 0
                    virt.setStrength(s)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Virtualizer apply error: ${e.message}")
        }

        try {
            loudnessEnhancer?.let { le ->
                le.enabled = isEnabled
                val gainMb = if (isEnabled) (loudnessStrength.coerceIn(0f, 1f) * 1200f).toInt() else 0
                le.setTargetGain(gainMb)
            }
        } catch (e: Exception) {
            Log.w(TAG, "LoudnessEnhancer apply error: ${e.message}")
        }
    }

    fun releaseEffects() {
        try { equalizer?.release() } catch (_: Exception) {}
        try { bassBoost?.release() } catch (_: Exception) {}
        try { virtualizer?.release() } catch (_: Exception) {}
        try { loudnessEnhancer?.release() } catch (_: Exception) {}
        equalizer = null
        bassBoost = null
        virtualizer = null
        loudnessEnhancer = null
    }
}
