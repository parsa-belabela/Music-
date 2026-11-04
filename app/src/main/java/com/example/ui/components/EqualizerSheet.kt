package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Speaker
import androidx.compose.material.icons.rounded.SurroundSound
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.AmbientPalette
import com.example.audio.AuraDspController
import com.example.data.model.AppSettings
import kotlin.math.roundToInt

data class EqPreset(
    val id: String,
    val titleFa: String,
    val bandsDb: List<Float>, // 5 bands: 60Hz, 230Hz, 910Hz, 3.6kHz, 14kHz (-15f to +15f)
    val bassBoost: Float,     // 0f to 1f
    val virtualizer: Float,   // 0f to 1f
    val loudnessGain: Float   // 0f to 1f
)

val DEFAULT_EQ_PRESETS = listOf(
    EqPreset("flat", "استاندارد (Flat)", listOf(0f, 0f, 0f, 0f, 0f), 0f, 0f, 0f),
    EqPreset("hiphop", "هیپ‌هاپ و رپ", listOf(7.5f, 4.5f, -1.5f, 3.0f, 5.5f), 0.75f, 0.35f, 0.3f),
    EqPreset("bass", "بیس سنگین (Bass)", listOf(9.0f, 6.0f, -2.0f, 1.5f, 3.0f), 0.90f, 0.25f, 0.25f),
    EqPreset("electronic", "الکترونیک و باشگاه", listOf(6.5f, 3.0f, -3.0f, 4.5f, 7.0f), 0.70f, 0.60f, 0.35f),
    EqPreset("vocal", "شفافیت وکال", listOf(-2.5f, 1.5f, 6.0f, 5.0f, 2.0f), 0.15f, 0.40f, 0.2f),
    EqPreset("rock", "راک و متال", listOf(5.5f, 3.0f, -1.5f, 4.5f, 6.5f), 0.55f, 0.45f, 0.3f),
    EqPreset("cinema", "سینمایی 3D", listOf(5.0f, 2.5f, 1.0f, 3.5f, 5.0f), 0.65f, 0.85f, 0.4f)
)

private val BAND_FREQUENCIES = listOf("60Hz", "230Hz", "910Hz", "3.6kHz", "14kHz")

@Composable
fun EqualizerSheet(
    settings: AppSettings,
    palette: AmbientPalette,
    onUpdateSettings: (AppSettings) -> Unit,
    onClose: () -> Unit
) {
    var isEnabled by remember { mutableStateOf(settings.equalizerEnabled) }
    val bandLevels = remember {
        mutableStateListOf<Float>().apply {
            val initial = if (settings.eqBands.size >= 5) settings.eqBands.take(5) else listOf(0f, 0f, 0f, 0f, 0f)
            addAll(initial)
        }
    }
    var bassStrength by remember { mutableFloatStateOf((settings.bassBoostStrength / 1000f).coerceIn(0f, 1f)) }
    var virtualizerStrength by remember { mutableFloatStateOf(settings.virtualizerStrength.coerceIn(0f, 1f)) }
    var loudnessStrength by remember { mutableFloatStateOf(settings.loudnessStrength.coerceIn(0f, 1f)) }
    var selectedPresetId by remember {
        mutableStateOf(
            DEFAULT_EQ_PRESETS.firstOrNull { it.id == settings.eqPreset || it.titleFa == settings.eqPreset }?.id ?: "flat"
        )
    }

    fun pushSettings(
        enabled: Boolean = isEnabled,
        bands: List<Float> = bandLevels.toList(),
        bass: Float = bassStrength,
        virt: Float = virtualizerStrength,
        loud: Float = loudnessStrength,
        presetId: String = selectedPresetId
    ) {
        val updatedGains = FloatArray(5) { i -> bands.getOrElse(i) { 0f } }
        AuraDspController.isEnabled = enabled
        AuraDspController.bandGainsDb = updatedGains
        AuraDspController.bassStrength = bass
        AuraDspController.virtualizerStrength = virt
        AuraDspController.loudnessStrength = loud
        AuraDspController.applyCurrentState()

        val fullBands = if (bands.size == 5) bands + listOf(0f, 0f, 0f, 0f, 0f) else bands
        onUpdateSettings(
            settings.copy(
                equalizerEnabled = enabled,
                eqBands = fullBands,
                bassBoostStrength = (bass * 1000f).roundToInt().coerceIn(0, 1000),
                virtualizerStrength = virt,
                loudnessStrength = loud,
                eqPreset = presetId
            )
        )
    }

    AuraLiquidEqualizerSheet(
        isEnabled = isEnabled,
        bandLevelsDb = bandLevels.toList(),
        bassStrength = bassStrength,
        virtualizerStrength = virtualizerStrength,
        loudnessStrength = loudnessStrength,
        selectedPresetId = selectedPresetId,
        onEnabledChange = { newEnabled ->
            isEnabled = newEnabled
            pushSettings(enabled = newEnabled)
        },
        onBandLevelChange = { index, newDb ->
            if (index in 0 until bandLevels.size) {
                bandLevels[index] = newDb
                selectedPresetId = "custom"
                pushSettings(bands = bandLevels.toList(), presetId = "custom")
            }
        },
        onBassChange = { newBass ->
            bassStrength = newBass
            pushSettings(bass = newBass)
        },
        onVirtualizerChange = { newVirt ->
            virtualizerStrength = newVirt
            pushSettings(virt = newVirt)
        },
        onLoudnessChange = { newLoud ->
            loudnessStrength = newLoud
            pushSettings(loud = newLoud)
        },
        onPresetSelect = { preset ->
            selectedPresetId = preset.id
            bandLevels.clear()
            bandLevels.addAll(preset.bandsDb)
            bassStrength = preset.bassBoost
            virtualizerStrength = preset.virtualizer
            loudnessStrength = preset.loudnessGain
            pushSettings(
                bands = preset.bandsDb,
                bass = preset.bassBoost,
                virt = preset.virtualizer,
                loud = preset.loudnessGain,
                presetId = preset.id
            )
        },
        onReset = {
            selectedPresetId = "flat"
            bandLevels.clear()
            bandLevels.addAll(listOf(0f, 0f, 0f, 0f, 0f))
            bassStrength = 0f
            virtualizerStrength = 0f
            loudnessStrength = 0f
            pushSettings(
                bands = listOf(0f, 0f, 0f, 0f, 0f),
                bass = 0f,
                virt = 0f,
                loud = 0f,
                presetId = "flat"
            )
        },
        onDismiss = onClose
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuraLiquidEqualizerSheet(
    isEnabled: Boolean,
    bandLevelsDb: List<Float>,
    bassStrength: Float,
    virtualizerStrength: Float,
    loudnessStrength: Float,
    selectedPresetId: String,
    onEnabledChange: (Boolean) -> Unit,
    onBandLevelChange: (Int, Float) -> Unit,
    onBassChange: (Float) -> Unit,
    onVirtualizerChange: (Float) -> Unit,
    onLoudnessChange: (Float) -> Unit,
    onPresetSelect: (EqPreset) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val accentCyan = Color(0xFF00E5FF)
    val accentViolet = Color(0xFF9D4EDD)
    val glassSurface = Color(0xFF10131C).copy(alpha = 0.96f)
    val cardGlass = Color.White.copy(alpha = 0.06f)
    val borderGlass = Color.White.copy(alpha = 0.12f)

    val safeBands = remember(bandLevelsDb) {
        if (bandLevelsDb.size >= 5) bandLevelsDb.take(5)
        else listOf(0f, 0f, 0f, 0f, 0f)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = glassSurface,
        scrimColor = Color.Black.copy(alpha = 0.65f),
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
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
                                    colors = listOf(accentCyan.copy(alpha = 0.25f), accentViolet.copy(alpha = 0.25f))
                                )
                            )
                            .border(1.dp, borderGlass, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Equalizer,
                            contentDescription = "Equalizer",
                            tint = if (isEnabled) accentCyan else Color.Gray
                        )
                    }
                    Column {
                        Text(
                            text = "موتور صوتی و اکولایزر Aura",
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isEnabled) "پردازش زنده فرکانس و بیس فعال است" else "اکولایزر غیرفعال است",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 12.sp
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = onReset,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(cardGlass)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = "Reset",
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Switch(
                        checked = isEnabled,
                        onCheckedChange = onEnabledChange,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = accentCyan,
                            uncheckedThumbColor = Color.Gray,
                            uncheckedTrackColor = Color.White.copy(alpha = 0.1f)
                        )
                    )
                }
            }

            // 2. Presets Horizontal Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DEFAULT_EQ_PRESETS.forEach { preset ->
                    val isSelected = selectedPresetId == preset.id
                    val bgColor by animateColorAsState(
                        targetValue = if (isSelected) accentCyan.copy(alpha = 0.22f) else cardGlass,
                        label = "presetBg"
                    )
                    val borderColor by animateColorAsState(
                        targetValue = if (isSelected) accentCyan else borderGlass,
                        label = "presetBorder"
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(bgColor)
                            .border(1.dp, borderColor, RoundedCornerShape(14.dp))
                            .clickable(enabled = isEnabled) { onPresetSelect(preset) }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = preset.titleFa,
                            color = if (isSelected) accentCyan else Color.White.copy(alpha = 0.8f),
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }

            // 3. Live Frequency Response Spline Curve
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(115.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(cardGlass)
                    .border(1.dp, borderGlass, RoundedCornerShape(20.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                val animBands = safeBands.mapIndexed { idx, value ->
                    animateFloatAsState(
                        targetValue = if (isEnabled) value else 0f,
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        label = "bandCurve$idx"
                    ).value
                }

                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val midY = h / 2f

                    // Grid lines
                    drawLine(
                        color = Color.White.copy(alpha = 0.08f),
                        start = Offset(0f, midY),
                        end = Offset(w, midY),
                        strokeWidth = 1.dp.toPx()
                    )

                    val points = animBands.mapIndexed { i, db ->
                        val x = (w / (animBands.size - 1)) * i
                        val normalized = (db.coerceIn(-15f, 15f) / 15f)
                        val y = midY - (normalized * (midY * 0.82f))
                        Offset(x, y)
                    }

                    if (points.size >= 2) {
                        val strokePath = Path().apply {
                            moveTo(points.first().x, points.first().y)
                            for (i in 0 until points.size - 1) {
                                val p0 = points[i]
                                val p1 = points[i + 1]
                                val controlX = (p0.x + p1.x) / 2f
                                cubicTo(controlX, p0.y, controlX, p1.y, p1.x, p1.y)
                            }
                        }

                        val fillPath = Path().apply {
                            addPath(strokePath)
                            lineTo(w, h)
                            lineTo(0f, h)
                            close()
                        }

                        drawPath(
                            path = fillPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    accentCyan.copy(alpha = if (isEnabled) 0.35f else 0.08f),
                                    accentViolet.copy(alpha = 0.02f)
                                )
                            )
                        )

                        drawPath(
                            path = strokePath,
                            brush = Brush.horizontalGradient(
                                colors = if (isEnabled) listOf(accentCyan, accentViolet, accentCyan)
                                else listOf(Color.Gray, Color.DarkGray)
                            ),
                            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                        )

                        points.forEach { pt ->
                            drawCircle(
                                color = if (isEnabled) accentCyan else Color.Gray,
                                radius = 4.5.dp.toPx(),
                                center = pt
                            )
                        }
                    }
                }
            }

            // 4. 5-Band Custom Vertical Glass Sliders
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(cardGlass)
                    .border(1.dp, borderGlass, RoundedCornerShape(24.dp))
                    .padding(vertical = 16.dp, horizontal = 10.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                safeBands.forEachIndexed { index, dbValue ->
                    VerticalEqBandSlider(
                        frequencyLabel = BAND_FREQUENCIES.getOrElse(index) { "" },
                        dbValue = dbValue,
                        enabled = isEnabled,
                        accentCyan = accentCyan,
                        accentViolet = accentViolet,
                        onValueChange = { newDb -> onBandLevelChange(index, newDb) }
                    )
                }
            }

            // 5. Bass Boost, 3D Virtualizer & Loudness Pre-Amp Controls
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(cardGlass)
                    .border(1.dp, borderGlass, RoundedCornerShape(24.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                DspEffectRow(
                    icon = Icons.Rounded.Speaker,
                    title = "تقویت کوبش بیس (Bass Boost)",
                    value = bassStrength,
                    enabled = isEnabled,
                    accentColor = accentCyan,
                    onValueChange = onBassChange
                )
                DspEffectRow(
                    icon = Icons.Rounded.SurroundSound,
                    title = "فضاسازی سه‌بعدی (3D Virtualizer)",
                    value = virtualizerStrength,
                    enabled = isEnabled,
                    accentColor = accentViolet,
                    onValueChange = onVirtualizerChange
                )
                DspEffectRow(
                    icon = Icons.Rounded.VolumeUp,
                    title = "تقویت قدرت خروجی (Loudness Gain)",
                    value = loudnessStrength,
                    enabled = isEnabled,
                    accentColor = Color(0xFF00F5D4),
                    onValueChange = onLoudnessChange
                )
            }
        }
    }
}

@Composable
private fun VerticalEqBandSlider(
    frequencyLabel: String,
    dbValue: Float,
    enabled: Boolean,
    accentCyan: Color,
    accentViolet: Color,
    onValueChange: (Float) -> Unit
) {
    val currentDb by rememberUpdatedState(dbValue)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "${if (dbValue > 0) "+" else ""}${dbValue.roundToInt()}dB",
            color = if (enabled) Color.White.copy(alpha = 0.9f) else Color.Gray,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )

        Box(
            modifier = Modifier
                .width(38.dp)
                .height(155.dp)
                .clip(RoundedCornerShape(19.dp))
                .background(Color.Black.copy(alpha = 0.35f))
                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(19.dp))
                .pointerInput(enabled) {
                    if (!enabled) return@pointerInput
                    detectVerticalDragGestures { change, dragAmount ->
                        change.consume()
                        val heightPx = size.height.toFloat().coerceAtLeast(1f)
                        val deltaDb = (-dragAmount / heightPx) * 30f
                        val updated = (currentDb + deltaDb).coerceIn(-15f, 15f)
                        onValueChange(updated)
                    }
                },
            contentAlignment = Alignment.BottomCenter
        ) {
            val fraction = ((dbValue.coerceIn(-15f, 15f) + 15f) / 30f).coerceIn(0.05f, 1f)
            val animFraction by animateFloatAsState(
                targetValue = if (enabled) fraction else 0.5f,
                label = "sliderFill"
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(animFraction)
                    .clip(RoundedCornerShape(19.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = if (enabled) listOf(accentCyan, accentViolet)
                            else listOf(Color.Gray.copy(alpha = 0.4f), Color.DarkGray.copy(alpha = 0.4f))
                        )
                    )
            )
        }

        Text(
            text = frequencyLabel,
            color = Color.White.copy(alpha = 0.65f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun DspEffectRow(
    icon: ImageVector,
    title: String,
    value: Float,
    enabled: Boolean,
    accentColor: Color,
    onValueChange: (Float) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = if (enabled) accentColor else Color.Gray,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = title,
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            Text(
                text = "${(value.coerceIn(0f, 1f) * 100).roundToInt()}%",
                color = if (enabled) accentColor else Color.Gray,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Slider(
            value = value.coerceIn(0f, 1f),
            onValueChange = onValueChange,
            enabled = enabled,
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = accentColor,
                inactiveTrackColor = Color.White.copy(alpha = 0.12f)
            )
        )
    }
}
