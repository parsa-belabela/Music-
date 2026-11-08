package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.AmbientPalette
import com.example.audio.AuraDspController
import com.example.audio.EqPreset
import com.example.data.model.AppLanguage
import com.example.data.model.AppSettings
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun EqualizerSheet(
    settings: AppSettings,
    palette: AmbientPalette,
    onUpdateSettings: (AppSettings) -> Unit,
    onClose: () -> Unit
) {
    val dspController = remember { AuraDspController.getInstance() }
    val dspState by dspController.state.collectAsState()

    LaunchedEffect(Unit) {
        if (settings.eqBands.isNotEmpty() && dspState.bandLevelsDb.all { it == 0f } && settings.eqBands.any { it != 0f }) {
            val tenBands = if (settings.eqBands.size >= 10) {
                settings.eqBands.take(10)
            } else {
                List(10) { idx -> settings.eqBands.getOrElse(idx / 2) { 0f } }
            }
            dspController.setBandLevels(tenBands, settings.eqPreset.ifBlank { "custom" })
        }
        if (settings.bassBoostStrength > 0 && dspState.bassBoostStrength == 0f) {
            dspController.setBassBoost(settings.bassBoostStrength)
        }
        if (settings.virtualizerStrength > 0f && dspState.virtualizerStrength == 0f) {
            dspController.setVirtualizer(settings.virtualizerStrength)
        }
        if (settings.loudnessStrength > 0f && dspState.loudnessGainDb == 0f) {
            dspController.setLoudnessGain(settings.loudnessStrength * 15f)
        }
        dspController.setEnabled(settings.equalizerEnabled)
    }

    EqualizerSheet(
        onDismiss = onClose,
        audioSessionId = dspState.audioSessionId,
        dspController = dspController,
        isPersian = settings.language == AppLanguage.PERSIAN,
        accentColor = Color(0xFF00E5FF),
        secondaryAccent = Color(0xFF7C4DFF),
        onBandsChanged = { updatedBands ->
            val current = dspController.state.value
            onUpdateSettings(
                settings.copy(
                    equalizerEnabled = current.isEnabled,
                    eqBands = updatedBands,
                    bassBoostStrength = (current.bassBoostStrength * 1000f).roundToInt().coerceIn(0, 1000),
                    virtualizerStrength = current.virtualizerStrength,
                    loudnessStrength = (current.loudnessGainDb / 15f).coerceIn(0f, 1f),
                    eqPreset = current.activePresetId
                )
            )
        },
        onBassBoostChanged = { updatedBass ->
            val current = dspController.state.value
            onUpdateSettings(
                settings.copy(
                    equalizerEnabled = current.isEnabled,
                    eqBands = current.bandLevelsDb,
                    bassBoostStrength = (updatedBass * 1000f).roundToInt().coerceIn(0, 1000),
                    virtualizerStrength = current.virtualizerStrength,
                    loudnessStrength = (current.loudnessGainDb / 15f).coerceIn(0f, 1f),
                    eqPreset = current.activePresetId
                )
            )
        },
        onVirtualizerChanged = { updatedVirt ->
            val current = dspController.state.value
            onUpdateSettings(
                settings.copy(
                    equalizerEnabled = current.isEnabled,
                    eqBands = current.bandLevelsDb,
                    bassBoostStrength = (current.bassBoostStrength * 1000f).roundToInt().coerceIn(0, 1000),
                    virtualizerStrength = updatedVirt,
                    loudnessStrength = (current.loudnessGainDb / 15f).coerceIn(0f, 1f),
                    eqPreset = current.activePresetId
                )
            )
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EqualizerSheet(
    onDismiss: () -> Unit = {},
    audioSessionId: Int = 0,
    dspController: AuraDspController = remember(audioSessionId) {
        AuraDspController.getInstance(audioSessionId)
    },
    isPersian: Boolean = true,
    accentColor: Color = Color(0xFF00E5FF),
    secondaryAccent: Color = Color(0xFF7C4DFF),
    onBandsChanged: ((List<Float>) -> Unit)? = null,
    onBassBoostChanged: ((Float) -> Unit)? = null,
    onVirtualizerChanged: ((Float) -> Unit)? = null
) {
    val dspState by dspController.state.collectAsState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(audioSessionId) {
        if (audioSessionId > 0) {
            dspController.attachToSession(audioSessionId)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF0B0E17),
        contentColor = Color.White,
        scrimColor = Color.Black.copy(alpha = 0.72f),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 6.dp)
                    .size(width = 44.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.25f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(accentColor.copy(alpha = 0.25f), secondaryAccent.copy(alpha = 0.25f))
                                )
                            )
                            .border(1.dp, accentColor.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = null,
                            tint = accentColor
                        )
                    }
                    Column {
                        Text(
                            text = if (isPersian) "موتور پردازش صدای AURA DSP" else "AURA STUDIO MASTERING DSP",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        Text(
                            text = if (isPersian) "۱۰ باند پارامتریک + کمپرسور چندبانده + لیمیتر" else "10-Band Parametric • Multi-Band MBC • Brickwall Limiter",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    IconButton(
                        onClick = {
                            dspController.resetToFlat()
                            onBandsChanged?.invoke(List(10) { 0f })
                            onBassBoostChanged?.invoke(0f)
                            onVirtualizerChanged?.invoke(0f)
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset",
                            tint = Color.White.copy(alpha = 0.75f)
                        )
                    }

                    Switch(
                        checked = dspState.isEnabled,
                        onCheckedChange = { dspController.setEnabled(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Black,
                            checkedTrackColor = accentColor,
                            uncheckedThumbColor = Color.White.copy(alpha = 0.6f),
                            uncheckedTrackColor = Color.White.copy(alpha = 0.12f)
                        )
                    )

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White.copy(alpha = 0.75f)
                        )
                    }
                }
            }

            // 2. Presets Horizontal Ribbon
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AuraDspController.BUILT_IN_PRESETS.forEach { preset ->
                    val isSelected = dspState.activePresetId == preset.id
                    PresetChip(
                        preset = preset,
                        isSelected = isSelected,
                        isPersian = isPersian,
                        accentColor = accentColor,
                        secondaryAccent = secondaryAccent,
                        onClick = {
                            dspController.applyPreset(preset)
                            onBandsChanged?.invoke(preset.bands)
                            onBassBoostChanged?.invoke(preset.bassBoost)
                            onVirtualizerChanged?.invoke(preset.virtualizer)
                        }
                    )
                }
            }

            // 3. Interactive Spline Frequency Curve Visualizer
            SplineResponseVisualizer(
                bandsDb = dspState.bandLevelsDb,
                bassBoost = dspState.bassBoostStrength,
                clarity = dspState.vocalClarity,
                isEnabled = dspState.isEnabled,
                accentColor = accentColor,
                secondaryAccent = secondaryAccent
            )

            // 4. 10-Band Precision Vertical Graphic Sliders + Preamp
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color(0xFF121726))
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(22.dp))
                    .padding(vertical = 14.dp, horizontal = 8.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isPersian) "اکولایزر گرافیکی ۱۰ باند (±15 dB)" else "10-Band Graphic Equalizer (±15 dB)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                        Text(
                            text = "Pre-Amp: ${if (dspState.preampDb >= 0) "+" else ""}${"%.1f".format(dspState.preampDb)} dB",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = accentColor
                        )
                    }

                    // Pre-Amp Horizontal Bar
                    Slider(
                        value = dspState.preampDb,
                        onValueChange = { dspController.setPreampDb(it) },
                        valueRange = -12f..12f,
                        enabled = dspState.isEnabled,
                        colors = SliderDefaults.colors(
                            thumbColor = accentColor,
                            activeTrackColor = accentColor,
                            inactiveTrackColor = Color.White.copy(alpha = 0.12f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(26.dp)
                            .padding(horizontal = 8.dp)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        dspState.bandFrequencies.forEachIndexed { index, freqHz ->
                            val gainDb = dspState.bandLevelsDb.getOrElse(index) { 0f }
                            VerticalEqBandSlider(
                                frequencyHz = freqHz,
                                gainDb = gainDb,
                                isEnabled = dspState.isEnabled,
                                accentColor = accentColor,
                                secondaryAccent = secondaryAccent,
                                onGainChange = { newGain ->
                                    dspController.setBandLevel(index, newGain)
                                    onBandsChanged?.invoke(dspController.state.value.bandLevelsDb)
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // 5. 4 Studio Rotary Knobs (Sub-Bass, 3D Spatial, X-Loudness, Vocal Clarity)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color(0xFF121726))
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(22.dp))
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = if (isPersian) "پردازشگرهای آکوستیک و فضایی" else "Psychoacoustic & Mastering Controls",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        RotaryDspKnob(
                            label = if (isPersian) "توربو بیس" else "Sub-Bass",
                            valueText = "${(dspState.bassBoostStrength * 100).roundToInt()}%",
                            progress = dspState.bassBoostStrength,
                            enabled = dspState.isEnabled,
                            accentColor = Color(0xFFFF3D71),
                            onValueChange = {
                                dspController.setBassBoost(it)
                                onBassBoostChanged?.invoke(it)
                            }
                        )

                        RotaryDspKnob(
                            label = if (isPersian) "صدای 3D" else "3D Spatial",
                            valueText = "${(dspState.virtualizerStrength * 100).roundToInt()}%",
                            progress = dspState.virtualizerStrength,
                            enabled = dspState.isEnabled,
                            accentColor = accentColor,
                            onValueChange = {
                                dspController.setVirtualizer(it)
                                onVirtualizerChanged?.invoke(it)
                            }
                        )

                        RotaryDspKnob(
                            label = if (isPersian) "قدرت صدا" else "X-Loudness",
                            valueText = "+${"%.1f".format(dspState.loudnessGainDb)}dB",
                            progress = (dspState.loudnessGainDb / 15f).coerceIn(0f, 1f),
                            enabled = dspState.isEnabled,
                            accentColor = Color(0xFFFFAA00),
                            onValueChange = {
                                dspController.setLoudnessGain(it * 15f)
                            }
                        )

                        RotaryDspKnob(
                            label = if (isPersian) "شفافیت وکال" else "Vocal Air",
                            valueText = "${(dspState.vocalClarity * 100).roundToInt()}%",
                            progress = dspState.vocalClarity,
                            enabled = dspState.isEnabled,
                            accentColor = Color(0xFF00E096),
                            onValueChange = {
                                dspController.setVocalClarity(it)
                            }
                        )
                    }

                    // Sub-Bass Punch Slider
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (isPersian) "کوبش ساب‌ووفر (Sub-Harmonic Punch)" else "Sub-Harmonic Punch (31Hz - 90Hz)",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                            Text(
                                text = "${(dspState.subBassPunch * 100).roundToInt()}%",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFF3D71)
                            )
                        }
                        Slider(
                            value = dspState.subBassPunch,
                            onValueChange = { dspController.setSubBassPunch(it) },
                            valueRange = 0f..1f,
                            enabled = dspState.isEnabled,
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFFFF3D71),
                                activeTrackColor = Color(0xFFFF3D71),
                                inactiveTrackColor = Color.White.copy(alpha = 0.12f)
                            )
                        )
                    }
                }
            }

            // 6. Mastering Hardware Stage Toggles (Multi-Band Compressor & Brickwall Limiter)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MasteringToggleCard(
                    title = if (isPersian) "کمپرسور چندبانده (MBC)" else "Multi-Band MBC",
                    subtitle = if (isPersian) "تفکیک و پانچ استودیویی" else "Dynamic Punch & Warmth",
                    checked = dspState.multibandCompressorEnabled,
                    enabled = dspState.isEnabled,
                    accentColor = accentColor,
                    onCheckedChange = { dspController.setMultibandCompressor(it) },
                    modifier = Modifier.weight(1f)
                )

                MasteringToggleCard(
                    title = if (isPersian) "لیمیتر ضد خش (Limiter)" else "Brickwall Limiter",
                    subtitle = if (isPersian) "جلوگیری از دیستورت در بیس بالا" else "Zero Clipping Protection",
                    checked = dspState.brickwallLimiterEnabled,
                    enabled = dspState.isEnabled,
                    accentColor = Color(0xFF00E096),
                    onCheckedChange = { dspController.setBrickwallLimiter(it) },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun PresetChip(
    preset: EqPreset,
    isSelected: Boolean,
    isPersian: Boolean,
    accentColor: Color,
    secondaryAccent: Color,
    onClick: () -> Unit
) {
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) accentColor.copy(alpha = 0.22f) else Color(0xFF151B2C),
        label = "chipBg"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) accentColor else Color.White.copy(alpha = 0.10f),
        label = "chipBorder"
    )

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (isPersian) preset.nameFa else preset.nameEn,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) accentColor else Color.White.copy(alpha = 0.8f)
        )
    }
}

@Composable
private fun SplineResponseVisualizer(
    bandsDb: List<Float>,
    bassBoost: Float,
    clarity: Float,
    isEnabled: Boolean,
    accentColor: Color,
    secondaryAccent: Color
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(125.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF0E1322))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(20.dp))
            .padding(12.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val midY = h / 2f

            // Reference dB grid lines (+15, 0, -15)
            val gridColor = Color.White.copy(alpha = 0.07f)
            drawLine(gridColor, Offset(0f, 0f), Offset(w, 0f), strokeWidth = 1f)
            drawLine(
                Color.White.copy(alpha = 0.18f),
                Offset(0f, midY),
                Offset(w, midY),
                strokeWidth = 1.2f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
            )
            drawLine(gridColor, Offset(0f, h), Offset(w, h), strokeWidth = 1f)

            val points = bandsDb.mapIndexed { idx, db ->
                val extra = when (idx) {
                    0 -> bassBoost * 5f
                    1 -> bassBoost * 3.5f
                    8, 9 -> clarity * 3.5f
                    else -> 0f
                }
                val totalDb = if (isEnabled) (db + extra).coerceIn(-15f, 15f) else 0f
                val x = (idx.toFloat() / (bandsDb.size - 1).coerceAtLeast(1)) * w
                val y = midY - (totalDb / 15f) * (midY * 0.88f)
                Offset(x, y)
            }

            if (points.size >= 2) {
                val strokePath = Path().apply {
                    moveTo(points.first().x, points.first().y)
                    for (i in 0 until points.size - 1) {
                        val p0 = points[i]
                        val p1 = points[i + 1]
                        val ctrlX = (p0.x + p1.x) / 2f
                        cubicTo(ctrlX, p0.y, ctrlX, p1.y, p1.x, p1.y)
                    }
                }

                val fillPath = Path().apply {
                    addPath(strokePath)
                    lineTo(w, h)
                    lineTo(0f, h)
                    close()
                }

                val activeColor = if (isEnabled) accentColor else Color.Gray
                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            activeColor.copy(alpha = 0.36f),
                            secondaryAccent.copy(alpha = 0.12f),
                            Color.Transparent
                        )
                    )
                )

                drawPath(
                    path = strokePath,
                    brush = Brush.horizontalGradient(listOf(accentColor, secondaryAccent, accentColor)),
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )

                points.forEach { pt ->
                    drawCircle(
                        color = Color(0xFF0E1322),
                        radius = 5.dp.toPx(),
                        center = pt
                    )
                    drawCircle(
                        color = activeColor,
                        radius = 3.5.dp.toPx(),
                        center = pt
                    )
                }
            }
        }
    }
}

@Composable
private fun VerticalEqBandSlider(
    frequencyHz: Int,
    gainDb: Float,
    isEnabled: Boolean,
    accentColor: Color,
    secondaryAccent: Color,
    onGainChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val freqLabel = remember(frequencyHz) {
        if (frequencyHz >= 1000) "${frequencyHz / 1000}k" else "$frequencyHz"
    }

    Column(
        modifier = modifier.fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "${if (gainDb > 0.05f) "+" else ""}${gainDb.roundToInt()}",
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = if (gainDb != 0f && isEnabled) accentColor else Color.White.copy(alpha = 0.45f),
            textAlign = TextAlign.Center
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(vertical = 6.dp)
                .pointerInput(isEnabled) {
                    if (!isEnabled) return@pointerInput
                    detectTapGestures(
                        onDoubleTap = { onGainChange(0f) },
                        onPress = { offset ->
                            val norm = 1f - (offset.y / size.height).coerceIn(0f, 1f)
                            val newDb = (norm * 30f) - 15f
                            onGainChange(newDb)
                        }
                    )
                }
                .pointerInput(isEnabled) {
                    if (!isEnabled) return@pointerInput
                    detectDragGestures { change, _ ->
                        change.consume()
                        val norm = 1f - (change.position.y / size.height).coerceIn(0f, 1f)
                        val newDb = (norm * 30f) - 15f
                        onGainChange(newDb)
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val centerX = size.width / 2f
                val trackTop = 8.dp.toPx()
                val trackBottom = size.height - 8.dp.toPx()
                val trackHeight = (trackBottom - trackTop).coerceAtLeast(1f)
                val midY = trackTop + trackHeight / 2f

                // Background Track
                drawLine(
                    color = Color.White.copy(alpha = 0.12f),
                    start = Offset(centerX, trackTop),
                    end = Offset(centerX, trackBottom),
                    strokeWidth = 5.dp.toPx(),
                    cap = StrokeCap.Round
                )

                val normalized = ((gainDb + 15f) / 30f).coerceIn(0f, 1f)
                val thumbY = trackBottom - normalized * trackHeight

                // Active Fill from 0 dB center
                val activeColor = if (isEnabled) accentColor else Color.Gray
                drawLine(
                    brush = Brush.verticalGradient(listOf(accentColor, secondaryAccent)),
                    start = Offset(centerX, midY),
                    end = Offset(centerX, thumbY),
                    strokeWidth = 5.dp.toPx(),
                    cap = StrokeCap.Round
                )

                // Thumb Glow & Cap
                drawCircle(
                    color = activeColor.copy(alpha = 0.28f),
                    radius = 10.dp.toPx(),
                    center = Offset(centerX, thumbY)
                )
                drawCircle(
                    color = activeColor,
                    radius = 6.dp.toPx(),
                    center = Offset(centerX, thumbY)
                )
                drawCircle(
                    color = Color.White,
                    radius = 2.5.dp.toPx(),
                    center = Offset(centerX, thumbY)
                )
            }
        }

        Text(
            text = freqLabel,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White.copy(alpha = 0.65f),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun RotaryDspKnob(
    label: String,
    valueText: String,
    progress: Float,
    enabled: Boolean,
    accentColor: Color,
    onValueChange: (Float) -> Unit
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(120, easing = FastOutSlowInEasing),
        label = "knobProgress"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .pointerInput(enabled) {
                    if (!enabled) return@pointerInput
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val pos = change.position
                        val angle = Math.toDegrees(
                            atan2(
                                (pos.y - center.y).toDouble(),
                                (pos.x - center.x).toDouble()
                            )
                        ).toFloat()
                        // Map angle (-225..45 degrees) or vertical drag
                        val delta = (-dragAmount.y + dragAmount.x * 0.5f) / 140f
                        onValueChange((progress + delta).coerceIn(0f, 1f))
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 6.dp.toPx()
                val arcPadding = strokeWidth + 4.dp.toPx()
                val arcSize = Size(size.width - arcPadding * 2, size.height - arcPadding * 2)
                val topLeft = Offset(arcPadding, arcPadding)

                // Background Arc (135 deg to 405 deg -> 270 deg sweep)
                drawArc(
                    color = Color.White.copy(alpha = 0.10f),
                    startAngle = 135f,
                    sweepAngle = 270f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )

                val activeColor = if (enabled) accentColor else Color.Gray
                drawArc(
                    color = activeColor,
                    startAngle = 135f,
                    sweepAngle = 270f * animatedProgress,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )

                // Inner Knob Dial
                val radius = (size.minDimension / 2f) - arcPadding - 6.dp.toPx()
                drawCircle(
                    color = Color(0xFF1A2136),
                    radius = radius,
                    center = center
                )

                // Indicator Dot
                val angleRad = Math.toRadians((135f + 270f * animatedProgress).toDouble())
                val indicatorDist = radius * 0.68f
                val indicatorPos = Offset(
                    x = center.x + (cos(angleRad) * indicatorDist).toFloat(),
                    y = center.y + (sin(angleRad) * indicatorDist).toFloat()
                )
                drawCircle(
                    color = activeColor,
                    radius = 3.dp.toPx(),
                    center = indicatorPos
                )
            }

            Text(
                text = valueText,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = if (enabled) Color.White else Color.White.copy(alpha = 0.4f)
            )
        }

        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White.copy(alpha = 0.8f)
        )
    }
}

@Composable
private fun MasteringToggleCard(
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean,
    accentColor: Color,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFF121726))
            .border(
                width = 1.dp,
                color = if (checked && enabled) accentColor.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.08f),
                shape = RoundedCornerShape(18.dp)
            )
            .clickable(enabled = enabled) { onCheckedChange(!checked) }
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = subtitle,
                fontSize = 10.sp,
                color = Color.White.copy(alpha = 0.55f)
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.Black,
                checkedTrackColor = accentColor
            )
        )
    }
}
