package com.example.ui.components

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.AmbientPalette
import com.example.data.model.AppLanguage
import com.example.ui.theme.GlassThickness
import com.example.ui.theme.liquidGlass
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HearingProfileTestSheet(
    palette: AmbientPalette,
    lang: AppLanguage,
    onApplyCalibratedProfile: (List<Float>) -> Unit,
    onDismiss: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    var currentStep by remember { mutableStateOf(0) }

    // User preferences across 5 perceptual frequency ranges
    val userScores = remember { mutableStateListOf(0f, 0f, 0f, 0f, 0f) }

    // Audio test playback state
    var isPlayingSample by remember { mutableStateOf(false) }
    var playingBoostValue by remember { mutableStateOf<Float?>(null) }
    var playbackError by remember { mutableStateOf<String?>(null) }
    var activeAudioJob by remember { mutableStateOf<Job?>(null) }

    val steps = listOf(
        HearingTestStep(
            title = "Sub-Bass (31Hz - 62Hz)",
            freqHz = 55.0,
            descFa = "قدرت بیس عمیق و لرزش فرکانس‌های زیرین",
            descEn = "Deep sub-bass impact & low-end rumble"
        ),
        HearingTestStep(
            title = "Bass & Low Mids (125Hz - 250Hz)",
            freqHz = 180.0,
            descFa = "گرمی صدای خواننده مرد و بدنه گیتار بیس",
            descEn = "Vocal warmth & bassline body"
        ),
        HearingTestStep(
            title = "Midrange (500Hz - 1kHz)",
            freqHz = 800.0,
            descFa = "وضوح ملودی اصلی، پیانو و وکال زنانه",
            descEn = "Main melodies, piano clarity & female vocals"
        ),
        HearingTestStep(
            title = "Upper Mids (2kHz - 4kHz)",
            freqHz = 3000.0,
            descFa = "شفافیت ادای کلمات و حمله سازهای ضربی",
            descEn = "Vocal articulation & snare presence"
        ),
        HearingTestStep(
            title = "Highs & Air (8kHz - 16kHz)",
            freqHz = 9500.0,
            descFa = "درخشش سنج‌ها، کریستالی بودن صدا و هوا",
            descEn = "Cymbal shimmer, sparkle & acoustic air"
        )
    )

    // Stop tone when leaving screen
    DisposableEffect(Unit) {
        onDispose {
            activeAudioJob?.cancel()
        }
    }

    fun playTestTone(freqHz: Double, boostDb: Float) {
        activeAudioJob?.cancel()
        playbackError = null
        isPlayingSample = true
        playingBoostValue = boostDb

        activeAudioJob = coroutineScope.launch(Dispatchers.Default) {
            var audioTrack: AudioTrack? = null
            try {
                val sampleRate = 44100
                val durationSeconds = 1.0
                val numSamples = (durationSeconds * sampleRate).toInt()
                val buffer = ShortArray(numSamples)

                // Gain multiplier based on boost in dB
                val linearGain = (10.0.pow(boostDb / 20.0) * 0.35).coerceIn(0.1, 0.90)

                for (i in 0 until numSamples) {
                    val time = i.toDouble() / sampleRate
                    // Apply smooth Hanning envelope to avoid any audio clicks
                    val envelope = 0.5 * (1.0 - kotlin.math.cos(2.0 * PI * i / numSamples))
                    val sine = sin(2.0 * PI * freqHz * time)
                    // Add gentle musical harmonic for low frequencies
                    val harmonic = if (freqHz < 120.0) 0.3 * sin(4.0 * PI * freqHz * time) else 0.0
                    val sample = ((sine + harmonic) * linearGain * envelope * Short.MAX_VALUE).toInt()
                    buffer[i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                }

                val minBufSize = AudioTrack.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                )
                val bufferSize = maxOf(minBufSize, numSamples * 2)

                audioTrack = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(bufferSize)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()

                audioTrack.play()
                audioTrack.write(buffer, 0, numSamples)

                delay(1050L)
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    playbackError = if (lang == AppLanguage.PERSIAN) "در حال اتصال صدای تست..." else "Connecting test tone..."
                }
            } finally {
                try {
                    audioTrack?.stop()
                    audioTrack?.release()
                } catch (_: Exception) {}

                withContext(Dispatchers.Main) {
                    isPlayingSample = false
                    playingBoostValue = null
                }
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0C0C14),
        scrimColor = Color.Black.copy(alpha = 0.75f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(palette.accent.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Hearing,
                        contentDescription = null,
                        tint = palette.accent,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Text(
                    text = if (lang == AppLanguage.PERSIAN) "تست کالیبراسیون شنوایی استودیویی" else "Studio Hearing Calibration",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Step Progress Indicator
            LinearProgressIndicator(
                progress = { (currentStep + 1) / steps.size.toFloat() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = palette.accent,
                trackColor = Color(0x33FFFFFF)
            )

            Spacer(modifier = Modifier.height(20.dp))

            if (currentStep < steps.size) {
                val step = steps[currentStep]

                Text(
                    text = if (lang == AppLanguage.PERSIAN) "مرحله ${currentStep + 1} از ${steps.size}" else "Step ${currentStep + 1} of ${steps.size}",
                    style = MaterialTheme.typography.labelMedium.copy(color = palette.accent)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = step.title,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    ),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (lang == AppLanguage.PERSIAN) step.descFa else step.descEn,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color(0xFFA0A5BA),
                        lineHeight = 20.sp
                    ),
                    textAlign = TextAlign.Center
                )

                if (playbackError != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = playbackError ?: "",
                        color = Color(0xFFFF5252),
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 3 Interactive choices with A/B sound comparison
                val choices = listOf(
                    ((if (lang == AppLanguage.PERSIAN) "طبیعی و دست‌نخورده" else "Natural / Flat") to if (lang == AppLanguage.PERSIAN) "پاسخ خطی و بدون تغییر (0 dB)" else "Studio linear response (0 dB)") to 0.0f,
                    ((if (lang == AppLanguage.PERSIAN) "تقویت ملایم" else "Subtle Boost") to if (lang == AppLanguage.PERSIAN) "گرم‌تر و واضح‌تر (+۲.۵ dB)" else "Warm presence (+2.5 dB)") to 2.5f,
                    ((if (lang == AppLanguage.PERSIAN) "پرقدرت و شفاف" else "Vibrant Punch") to if (lang == AppLanguage.PERSIAN) "تاکید قوی و درخشان (+۵.۰ dB)" else "Rich emphasis (+5.0 dB)") to 5.0f
                )

                choices.forEach { (textPair, boostValue) ->
                    val (title, sub) = textPair
                    val isThisSamplePlaying = isPlayingSample && playingBoostValue == boostValue

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .liquidGlass(
                                shape = RoundedCornerShape(16.dp),
                                thickness = GlassThickness.REGULAR,
                                tintColor = if (isThisSamplePlaying) palette.accent else palette.primary,
                                tintAlpha = if (isThisSamplePlaying) 0.28f else 0.14f,
                                borderWidth = if (isThisSamplePlaying) 1.5.dp else 1.dp
                            )
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                userScores[currentStep] = boostValue
                                if (currentStep < steps.size - 1) {
                                    currentStep++
                                } else {
                                    // Generate 10-band EQ array
                                    val b0 = userScores[0]
                                    val b1 = (userScores[0] + userScores[1]) / 2f
                                    val b2 = userScores[1]
                                    val b3 = (userScores[1] + userScores[2]) / 2f
                                    val b4 = userScores[2]
                                    val b5 = userScores[2]
                                    val b6 = (userScores[2] + userScores[3]) / 2f
                                    val b7 = userScores[3]
                                    val b8 = (userScores[3] + userScores[4]) / 2f
                                    val b9 = userScores[4]

                                    val finalBands = listOf(b0, b1, b2, b3, b4, b5, b6, b7, b8, b9)
                                    onApplyCalibratedProfile(finalBands)
                                    onDismiss()
                                }
                            }
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(title, fontWeight = FontWeight.Bold, color = Color.White)
                                Text(sub, fontSize = 12.sp, color = Color(0xFFA0A5BA))
                            }

                            // Sound Preview Button with animated wave indicator
                            IconButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    playTestTone(step.freqHz, boostValue)
                                },
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(if (isThisSamplePlaying) palette.accent.copy(alpha = 0.35f) else Color(0x22FFFFFF))
                            ) {
                                if (isThisSamplePlaying) {
                                    AnimatedWaveIcon(color = palette.accent)
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.VolumeUp,
                                        contentDescription = "Test tone",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

private data class HearingTestStep(
    val title: String,
    val freqHz: Double,
    val descFa: String,
    val descEn: String
)

@Composable
private fun AnimatedWaveIcon(color: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "toneWave")
    val wave1 by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(250, easing = LinearEasing), RepeatMode.Reverse),
        label = "w1"
    )
    val wave2 by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 0.2f,
        animationSpec = infiniteRepeatable(tween(320, easing = LinearEasing), RepeatMode.Reverse),
        label = "w2"
    )
    val wave3 by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(tween(200, easing = LinearEasing), RepeatMode.Reverse),
        label = "w3"
    )

    Canvas(modifier = Modifier.size(20.dp)) {
        val w = size.width
        val h = size.height
        val barWidth = 3f

        drawLine(
            color = color,
            start = Offset(w * 0.25f, h * (1f - wave1) / 2f),
            end = Offset(w * 0.25f, h * (1f + wave1) / 2f),
            strokeWidth = barWidth,
            cap = StrokeCap.Round
        )
        drawLine(
            color = color,
            start = Offset(w * 0.50f, h * (1f - wave2) / 2f),
            end = Offset(w * 0.50f, h * (1f + wave2) / 2f),
            strokeWidth = barWidth,
            cap = StrokeCap.Round
        )
        drawLine(
            color = color,
            start = Offset(w * 0.75f, h * (1f - wave3) / 2f),
            end = Offset(w * 0.75f, h * (1f + wave3) / 2f),
            strokeWidth = barWidth,
            cap = StrokeCap.Round
        )
    }
}
