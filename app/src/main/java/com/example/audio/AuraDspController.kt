package com.example.audio

import android.content.Context
import android.media.audiofx.BassBoost
import android.media.audiofx.DynamicsProcessing
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.PresetReverb
import android.media.audiofx.Virtualizer
import android.os.Build
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToInt

data class EqPreset(
    val id: String,
    val nameFa: String,
    val nameEn: String,
    val bands: List<Float>, // 10 bands in dB (-15f .. +15f)
    val bassBoost: Float = 0f,
    val virtualizer: Float = 0f,
    val loudnessDb: Float = 0f,
    val clarity: Float = 0f
)

data class DspState(
    val isEnabled: Boolean = true,
    val audioSessionId: Int = 0,
    val bandFrequencies: List<Int> = AuraDspController.STANDARD_10_BANDS,
    val bandLevelsDb: List<Float> = List(10) { 0f },
    val preampDb: Float = 0f,
    val bassBoostStrength: Float = 0f,       // 0f .. 1f
    val virtualizerStrength: Float = 0f,     // 0f .. 1f
    val loudnessGainDb: Float = 0f,          // 0f .. 12f
    val subBassPunch: Float = 0f,            // 0f .. 1f
    val vocalClarity: Float = 0f,            // 0f .. 1f
    val multibandCompressorEnabled: Boolean = true,
    val brickwallLimiterEnabled: Boolean = true,
    val activePresetId: String = "flat",
    val reverbPreset: Int = 0
)

class AuraDspController(initialSessionId: Int = 0) {

    constructor(context: Context) : this(0)

    private var currentSessionId: Int = initialSessionId
    private var dynamicsProcessing: DynamicsProcessing? = null
    private var systemEqualizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null
    private var presetReverb: PresetReverb? = null

    private val _state = MutableStateFlow(DspState(audioSessionId = initialSessionId))
    val state: StateFlow<DspState> = _state.asStateFlow()

    init {
        INSTANCE = this
        if (initialSessionId >= 0) {
            attachToSession(initialSessionId)
        }
    }

    @Synchronized
    fun attachToSession(audioSessionId: Int) {
        val targetSession = if (audioSessionId < 0) 0 else audioSessionId
        if (currentSessionId == targetSession && (dynamicsProcessing != null || systemEqualizer != null)) {
            applyAllDspParameters()
            return
        }

        releaseEffectsInternal()
        currentSessionId = targetSession
        _state.value = _state.value.copy(audioSessionId = targetSession)

        // 1. Primary Studio Mastering Engine: Android DynamicsProcessing (API 28+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                val config = DynamicsProcessing.Config.Builder(
                    DynamicsProcessing.VARIANT_FAVOR_FREQUENCY_RESOLUTION,
                    2, // Stereo channels
                    true, // Pre-EQ enabled
                    BAND_COUNT,
                    true, // Multi-Band Compressor (MBC) enabled
                    BAND_COUNT,
                    true, // Post-EQ enabled (Psychoacoustic Bass & Air stage)
                    BAND_COUNT,
                    true  // Brickwall Mastering Limiter enabled
                ).build()

                dynamicsProcessing = DynamicsProcessing(Int.MAX_VALUE, targetSession, config).apply {
                    enabled = _state.value.isEnabled
                }
            } catch (e: Throwable) {
                Log.w(TAG, "DynamicsProcessing init fallback on session $targetSession: ${e.message}")
                dynamicsProcessing = null
            }
        }

        // 2. Hardware Native Equalizer (Secondary / Fallback sync)
        try {
            systemEqualizer = Equalizer(Int.MAX_VALUE - 1, targetSession).apply {
                enabled = _state.value.isEnabled
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Equalizer init failed: ${e.message}")
            systemEqualizer = null
        }

        // 3. Hardware BassBoost
        try {
            bassBoost = BassBoost(Int.MAX_VALUE - 2, targetSession).apply {
                enabled = _state.value.isEnabled && _state.value.bassBoostStrength > 0.01f
            }
        } catch (e: Throwable) {
            Log.w(TAG, "BassBoost init failed: ${e.message}")
            bassBoost = null
        }

        // 4. Hardware 3D Virtualizer
        try {
            virtualizer = Virtualizer(Int.MAX_VALUE - 3, targetSession).apply {
                enabled = _state.value.isEnabled && _state.value.virtualizerStrength > 0.01f
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Virtualizer init failed: ${e.message}")
            virtualizer = null
        }

        // 5. Hardware LoudnessEnhancer (Pre-Amp & Output Gain)
        try {
            loudnessEnhancer = LoudnessEnhancer(targetSession).apply {
                enabled = _state.value.isEnabled
            }
        } catch (e: Throwable) {
            Log.w(TAG, "LoudnessEnhancer init failed: ${e.message}")
            loudnessEnhancer = null
        }

        // 6. PresetReverb
        try {
            presetReverb = PresetReverb(Int.MAX_VALUE - 4, targetSession).apply {
                enabled = _state.value.isEnabled && _state.value.reverbPreset > 0
            }
        } catch (e: Throwable) {
            presetReverb = null
        }

        applyAllDspParameters()
    }

    fun bindAudioSession(audioSessionId: Int) = attachToSession(audioSessionId)
    fun setAudioSessionId(audioSessionId: Int) = attachToSession(audioSessionId)
    fun initialize(audioSessionId: Int) = attachToSession(audioSessionId)

    fun setEnabled(enabled: Boolean) {
        _state.value = _state.value.copy(isEnabled = enabled)
        applyAllDspParameters()
    }

    fun setBandLevel(bandIndex: Int, levelDb: Float) {
        if (bandIndex !in 0 until BAND_COUNT) return
        val clamped = levelDb.coerceIn(-MAX_DB_RANGE, MAX_DB_RANGE)
        val updated = _state.value.bandLevelsDb.toMutableList().apply {
            this[bandIndex] = clamped
        }
        _state.value = _state.value.copy(
            bandLevelsDb = updated,
            activePresetId = "custom"
        )
        applyAllDspParameters()
    }

    fun setBandLevels(levelsDb: List<Float>, presetId: String = "custom") {
        val normalized = List(BAND_COUNT) { idx ->
            levelsDb.getOrElse(idx) { 0f }.coerceIn(-MAX_DB_RANGE, MAX_DB_RANGE)
        }
        _state.value = _state.value.copy(
            bandLevelsDb = normalized,
            activePresetId = presetId
        )
        applyAllDspParameters()
    }

    fun setPreampDb(preampDb: Float) {
        _state.value = _state.value.copy(preampDb = preampDb.coerceIn(-12f, 12f))
        applyAllDspParameters()
    }

    fun setBassBoost(strength: Float) {
        val normalized = if (strength > 1f) (strength / 1000f).coerceIn(0f, 1f) else strength.coerceIn(0f, 1f)
        _state.value = _state.value.copy(bassBoostStrength = normalized)
        applyAllDspParameters()
    }

    fun setBassBoost(strengthPermille: Int) {
        setBassBoost(strengthPermille / 1000f)
    }

    fun setVirtualizer(strength: Float) {
        val normalized = if (strength > 1f) (strength / 1000f).coerceIn(0f, 1f) else strength.coerceIn(0f, 1f)
        _state.value = _state.value.copy(virtualizerStrength = normalized)
        applyAllDspParameters()
    }

    fun setVirtualizer(strengthPermille: Int) {
        setVirtualizer(strengthPermille / 1000f)
    }

    fun setStereoWidening(strength: Float) = setVirtualizer(strength)

    fun setLoudnessGain(gainDb: Float) {
        _state.value = _state.value.copy(loudnessGainDb = gainDb.coerceIn(0f, 15f))
        applyAllDspParameters()
    }

    fun setSubBassPunch(strength: Float) {
        _state.value = _state.value.copy(subBassPunch = strength.coerceIn(0f, 1f))
        applyAllDspParameters()
    }

    fun setVocalClarity(strength: Float) {
        _state.value = _state.value.copy(vocalClarity = strength.coerceIn(0f, 1f))
        applyAllDspParameters()
    }

    fun setMultibandCompressor(enabled: Boolean) {
        _state.value = _state.value.copy(multibandCompressorEnabled = enabled)
        applyAllDspParameters()
    }

    fun setBrickwallLimiter(enabled: Boolean) {
        _state.value = _state.value.copy(brickwallLimiterEnabled = enabled)
        applyAllDspParameters()
    }

    fun setReverbPreset(preset: Int) {
        _state.value = _state.value.copy(reverbPreset = preset.coerceIn(0, 6))
        applyAllDspParameters()
    }

    fun applyPreset(preset: EqPreset) {
        _state.value = _state.value.copy(
            bandLevelsDb = preset.bands.map { it.coerceIn(-MAX_DB_RANGE, MAX_DB_RANGE) },
            bassBoostStrength = preset.bassBoost.coerceIn(0f, 1f),
            virtualizerStrength = preset.virtualizer.coerceIn(0f, 1f),
            loudnessGainDb = preset.loudnessDb.coerceIn(0f, 15f),
            vocalClarity = preset.clarity.coerceIn(0f, 1f),
            activePresetId = preset.id
        )
        applyAllDspParameters()
    }

    fun applyPresetById(presetId: String) {
        val preset = BUILT_IN_PRESETS.find {
            it.id.equals(presetId, ignoreCase = true) ||
                it.nameEn.equals(presetId, ignoreCase = true) ||
                it.nameFa == presetId
        } ?: return
        applyPreset(preset)
    }

    fun resetToFlat() {
        _state.value = _state.value.copy(
            bandLevelsDb = List(BAND_COUNT) { 0f },
            preampDb = 0f,
            bassBoostStrength = 0f,
            virtualizerStrength = 0f,
            loudnessGainDb = 0f,
            subBassPunch = 0f,
            vocalClarity = 0f,
            reverbPreset = 0,
            activePresetId = "flat"
        )
        applyAllDspParameters()
    }

    @Synchronized
    private fun applyAllDspParameters() {
        val s = _state.value

        // Calculate composite per-band gains (User 10-band EQ + Sub-Bass Punch + Vocal Clarity/Air)
        val effectiveBandsDb = FloatArray(BAND_COUNT) { idx ->
            var gain = s.bandLevelsDb.getOrElse(idx) { 0f }
            // Sub-Bass harmonic shelf on 31Hz, 62Hz, 125Hz
            when (idx) {
                0 -> gain += s.bassBoostStrength * 6.5f + s.subBassPunch * 5.0f
                1 -> gain += s.bassBoostStrength * 5.0f + s.subBassPunch * 4.0f
                2 -> gain += s.bassBoostStrength * 2.5f + s.subBassPunch * 2.0f
                3 -> gain -= s.bassBoostStrength * 0.8f // Anti-mud cut at 250Hz
            }
            // Vocal Clarity & High-Air presence on 2kHz, 4kHz, 8kHz, 16kHz
            when (idx) {
                6 -> gain += s.vocalClarity * 2.5f
                7 -> gain += s.vocalClarity * 4.0f
                8 -> gain += s.vocalClarity * 4.5f
                9 -> gain += s.vocalClarity * 5.5f
            }
            gain.coerceIn(-18f, 18f)
        }

        // Automatic Headroom Compensation to prevent digital clipping when bands are boosted
        val maxBoost = (effectiveBandsDb.maxOrNull() ?: 0f).coerceAtLeast(0f)
        val autoHeadroomDb = if (s.brickwallLimiterEnabled && maxBoost > 6f) {
            -(maxBoost - 6f) * 0.45f
        } else {
            0f
        }

        // 1. Apply to Android 9+ DynamicsProcessing (Full 10-Band Pre-EQ + MBC + Post-EQ + Limiter)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            dynamicsProcessing?.let { dp ->
                try {
                    dp.enabled = s.isEnabled
                    if (s.isEnabled) {
                        dp.setInputGainAllChannelsTo(s.preampDb + autoHeadroomDb)

                        val preEq = DynamicsProcessing.Eq(true, true, BAND_COUNT)
                        val postEq = DynamicsProcessing.Eq(true, true, BAND_COUNT)
                        val mbc = DynamicsProcessing.Mbc(true, s.multibandCompressorEnabled, BAND_COUNT)

                        for (i in 0 until BAND_COUNT) {
                            val cutoffFreq = CUTOFF_FREQUENCIES[i]
                            val userGain = s.bandLevelsDb.getOrElse(i) { 0f }
                            val postExtraGain = effectiveBandsDb[i] - userGain

                            preEq.setBand(i, DynamicsProcessing.EqBand(true, cutoffFreq, userGain))
                            postEq.setBand(i, DynamicsProcessing.EqBand(true, cutoffFreq, postExtraGain))

                            // Studio Multi-Band Compressor curve tuned per frequency zone
                            val isBassZone = i <= 2
                            val isVocalZone = i in 5..7
                            val attackTime = if (isBassZone) 8f else 3f
                            val releaseTime = if (isBassZone) 90f else 50f
                            val ratio = if (isBassZone) 3.2f else if (isVocalZone) 2.2f else 2.0f
                            val threshold = if (isBassZone) -16f else -18f
                            val kneeWidth = 6f
                            val noiseGate = -75f
                            val expanderRatio = 1.0f
                            val preGain = if (isBassZone) (s.subBassPunch * 2.5f) else 0f
                            val postGain = if (s.multibandCompressorEnabled) 1.5f else 0f

                            mbc.setBand(
                                i,
                                DynamicsProcessing.MbcBand(
                                    s.multibandCompressorEnabled,
                                    cutoffFreq,
                                    attackTime,
                                    releaseTime,
                                    ratio,
                                    threshold,
                                    kneeWidth,
                                    noiseGate,
                                    expanderRatio,
                                    preGain,
                                    postGain
                                )
                            )
                        }

                        dp.setPreEqAllChannelsTo(preEq)
                        dp.setMbcAllChannelsTo(mbc)
                        dp.setPostEqAllChannelsTo(postEq)

                        val limiter = DynamicsProcessing.Limiter(
                            true,
                            s.brickwallLimiterEnabled,
                            0,
                            1.5f,   // 1.5ms ultra-fast attack
                            60f,    // 60ms smooth musical release
                            10f,    // 10:1 brickwall ratio
                            -0.8f,  // -0.8 dBFS ceiling
                            s.loudnessGainDb * 0.6f
                        )
                        dp.setLimiterAllChannelsTo(limiter)
                    }
                } catch (e: Throwable) {
                    Log.w(TAG, "Error applying DynamicsProcessing: ${e.message}")
                }
            }
        }

        // 2. Apply to Native Hardware Equalizer (interpolating 10 bands to device's hardware bands)
        systemEqualizer?.let { eq ->
            try {
                eq.enabled = s.isEnabled
                if (s.isEnabled) {
                    val numBands = eq.numberOfBands.toInt()
                    val levelRange = eq.bandLevelRange
                    val minMb = levelRange[0].toInt()
                    val maxMb = levelRange[1].toInt()

                    // If DynamicsProcessing is active, we apply 40% reinforcement on hardware EQ;
                    // if DynamicsProcessing is unavailable, hardware EQ carries 100% of the curve.
                    val scale = if (dynamicsProcessing != null) 0.45f else 1.0f

                    for (b in 0 until numBands) {
                        val centerFreqHz = (eq.getCenterFreq(b.toShort()) / 1000).toFloat()
                        val interpolatedDb = interpolateGainForFrequency(centerFreqHz, effectiveBandsDb) * scale
                        val targetMb = (interpolatedDb * 100f).roundToInt().coerceIn(minMb, maxMb).toShort()
                        eq.setBandLevel(b.toShort(), targetMb)
                    }
                }
            } catch (e: Throwable) {
                Log.w(TAG, "Error applying Equalizer: ${e.message}")
            }
        }

        // 3. Apply Hardware BassBoost
        bassBoost?.let { bb ->
            try {
                val active = s.isEnabled && (s.bassBoostStrength > 0.01f || s.subBassPunch > 0.01f)
                bb.enabled = active
                if (active && bb.strengthSupported) {
                    val combined = ((s.bassBoostStrength * 0.75f + s.subBassPunch * 0.25f) * 1000f)
                        .roundToInt()
                        .coerceIn(0, 1000)
                        .toShort()
                    bb.setStrength(combined)
                }
            } catch (e: Throwable) {
                Log.w(TAG, "Error applying BassBoost: ${e.message}")
            }
        }

        // 4. Apply Hardware 3D Virtualizer
        virtualizer?.let { virt ->
            try {
                val active = s.isEnabled && s.virtualizerStrength > 0.01f
                virt.enabled = active
                if (active && virt.strengthSupported) {
                    val strength = (s.virtualizerStrength * 1000f).roundToInt().coerceIn(0, 1000).toShort()
                    virt.setStrength(strength)
                }
            } catch (e: Throwable) {
                Log.w(TAG, "Error applying Virtualizer: ${e.message}")
            }
        }

        // 5. Apply LoudnessEnhancer (Pre-amp + Loudness in millibels)
        loudnessEnhancer?.let { le ->
            try {
                val totalGainDb = (s.loudnessGainDb + s.preampDb.coerceAtLeast(0f)).coerceIn(0f, 18f)
                val active = s.isEnabled && totalGainDb > 0.05f
                le.enabled = active
                if (active) {
                    val targetMb = (totalGainDb * 100f).roundToInt()
                    le.setTargetGain(targetMb)
                } else {
                    le.setTargetGain(0)
                }
            } catch (e: Throwable) {
                Log.w(TAG, "Error applying LoudnessEnhancer: ${e.message}")
            }
        }

        // 6. Apply PresetReverb
        presetReverb?.let { rev ->
            try {
                val active = s.isEnabled && s.reverbPreset > 0
                rev.enabled = active
                if (active) {
                    rev.preset = s.reverbPreset.toShort()
                }
            } catch (e: Throwable) {
                // Ignore reverb unsupported
            }
        }
    }

    private fun interpolateGainForFrequency(freqHz: Float, gainsDb: FloatArray): Float {
        val freqs = STANDARD_10_BANDS
        if (freqHz <= freqs.first()) return gainsDb.first()
        if (freqHz >= freqs.last()) return gainsDb.last()

        for (i in 0 until freqs.size - 1) {
            val f1 = freqs[i].toFloat()
            val f2 = freqs[i + 1].toFloat()
            if (freqHz in f1..f2) {
                val t = (freqHz - f1) / (f2 - f1)
                return gainsDb[i] + t * (gainsDb[i + 1] - gainsDb[i])
            }
        }
        return 0f
    }

    private fun releaseEffectsInternal() {
        try { dynamicsProcessing?.release() } catch (_: Throwable) {}
        try { systemEqualizer?.release() } catch (_: Throwable) {}
        try { bassBoost?.release() } catch (_: Throwable) {}
        try { virtualizer?.release() } catch (_: Throwable) {}
        try { loudnessEnhancer?.release() } catch (_: Throwable) {}
        try { presetReverb?.release() } catch (_: Throwable) {}

        dynamicsProcessing = null
        systemEqualizer = null
        bassBoost = null
        virtualizer = null
        loudnessEnhancer = null
        presetReverb = null
    }

    fun release() {
        releaseEffectsInternal()
    }

    companion object {
        private const val TAG = "AuraDspController"
        const val BAND_COUNT = 10
        const val MAX_DB_RANGE = 15f

        val STANDARD_10_BANDS = listOf(31, 62, 125, 250, 500, 1000, 2000, 4000, 8000, 16000)

        private val CUTOFF_FREQUENCIES = floatArrayOf(
            45f, 90f, 180f, 355f, 710f, 1400f, 2800f, 5600f, 11200f, 20000f
        )

        @Volatile
        private var INSTANCE: AuraDspController? = null

        fun getInstance(audioSessionId: Int = 0): AuraDspController {
            return INSTANCE?.also {
                if (audioSessionId > 0 && it.currentSessionId != audioSessionId) {
                    it.attachToSession(audioSessionId)
                }
            } ?: synchronized(this) {
                INSTANCE ?: AuraDspController(audioSessionId).also { INSTANCE = it }
            }
        }

        val BUILT_IN_PRESETS = listOf(
            EqPreset(
                id = "flat",
                nameFa = "استودیو (Flat)",
                nameEn = "Flat Studio",
                bands = listOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f),
                bassBoost = 0f, virtualizer = 0f, loudnessDb = 0f, clarity = 0f
            ),
            EqPreset(
                id = "aura_signature",
                nameFa = "امضای Aura Pro",
                nameEn = "Aura Signature",
                bands = listOf(6.5f, 5.5f, 2.5f, -1.0f, -0.5f, 1.5f, 3.5f, 5.0f, 6.0f, 5.5f),
                bassBoost = 0.55f, virtualizer = 0.45f, loudnessDb = 3.5f, clarity = 0.5f
            ),
            EqPreset(
                id = "bass_extreme",
                nameFa = "بیس فوق‌سنگین",
                nameEn = "Extreme Sub-Bass",
                bands = listOf(11.0f, 9.5f, 6.5f, 2.0f, -1.5f, -1.0f, 1.5f, 3.0f, 4.0f, 3.5f),
                bassBoost = 0.90f, virtualizer = 0.35f, loudnessDb = 4.5f, clarity = 0.35f
            ),
            EqPreset(
                id = "persian_pop",
                nameFa = "پاپ ایرانی",
                nameEn = "Persian Pop",
                bands = listOf(5.5f, 6.0f, 3.0f, 0.0f, 1.5f, 3.5f, 4.5f, 4.0f, 5.5f, 5.0f),
                bassBoost = 0.50f, virtualizer = 0.40f, loudnessDb = 3.0f, clarity = 0.60f
            ),
            EqPreset(
                id = "hiphop_trap",
                nameFa = "هیپ‌هاپ و ترپ",
                nameEn = "Hip-Hop & Trap",
                bands = listOf(9.5f, 8.5f, 4.5f, 0.5f, -1.5f, 0.5f, 2.5f, 4.5f, 6.5f, 7.0f),
                bassBoost = 0.80f, virtualizer = 0.50f, loudnessDb = 4.0f, clarity = 0.45f
            ),
            EqPreset(
                id = "car_audio",
                nameFa = "سیستم ماشین",
                nameEn = "Car Audio System",
                bands = listOf(8.5f, 7.5f, 4.0f, -1.5f, -2.0f, 1.0f, 3.5f, 5.5f, 7.5f, 8.0f),
                bassBoost = 0.75f, virtualizer = 0.65f, loudnessDb = 5.0f, clarity = 0.55f
            ),
            EqPreset(
                id = "edm_club",
                nameFa = "الکترونیک و کلاب",
                nameEn = "EDM & Club",
                bands = listOf(8.0f, 7.0f, 3.5f, -1.0f, -2.0f, 0.5f, 3.0f, 5.5f, 7.0f, 7.5f),
                bassBoost = 0.70f, virtualizer = 0.60f, loudnessDb = 4.0f, clarity = 0.50f
            ),
            EqPreset(
                id = "vocal_crystal",
                nameFa = "شفافیت وکال",
                nameEn = "Crystal Vocal",
                bands = listOf(-2.0f, -1.0f, 0.5f, 2.5f, 4.5f, 6.0f, 5.5f, 4.0f, 2.5f, 1.5f),
                bassBoost = 0.10f, virtualizer = 0.25f, loudnessDb = 2.5f, clarity = 0.85f
            ),
            EqPreset(
                id = "rock_metal",
                nameFa = "راک و متال",
                nameEn = "Rock & Metal",
                bands = listOf(6.0f, 5.0f, 3.0f, -1.5f, -2.5f, 1.5f, 4.5f, 6.5f, 7.0f, 6.5f),
                bassBoost = 0.45f, virtualizer = 0.40f, loudnessDb = 3.5f, clarity = 0.60f
            ),
            EqPreset(
                id = "spatial_3d",
                nameFa = "سه‌بعدی فضایی",
                nameEn = "3D Spatial Hall",
                bands = listOf(4.5f, 4.0f, 1.5f, -0.5f, 0.5f, 2.0f, 4.5f, 6.5f, 8.0f, 8.5f),
                bassBoost = 0.40f, virtualizer = 1.00f, loudnessDb = 3.0f, clarity = 0.75f
            )
        )
    }
}
