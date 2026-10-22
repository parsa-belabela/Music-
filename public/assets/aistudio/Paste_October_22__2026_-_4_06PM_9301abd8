package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.AmbientPalette
import com.example.data.model.AppLanguage
import com.example.data.model.AppSettings
import kotlin.math.roundToInt

data class CalibratedEqPreset(
    val id: String,
    val name: String,
    val nameFa: String = name,
    val bands: List<Float>, // 31Hz, 62Hz, 125Hz, 250Hz, 500Hz, 1kHz, 2kHz, 4kHz, 8kHz, 16kHz
    val recommendedBassBoost: Int = 0
) {
    fun getName(lang: AppLanguage): String = if (lang == AppLanguage.PERSIAN) nameFa else name
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EqualizerSheet(
    settings: AppSettings,
    palette: AmbientPalette,
    onUpdateSettings: (AppSettings) -> Unit,
    onClose: () -> Unit
) {
    val lang = settings.language
    val isFa = lang == AppLanguage.PERSIAN

    var eqEnabled by remember { mutableStateOf(settings.equalizerEnabled) }
    var bassBoost by remember { mutableFloatStateOf(settings.bassBoostStrength.toFloat()) }
    var crossfade by remember { mutableIntStateOf(settings.crossfadeDurationSeconds) }
    var gapless by remember { mutableStateOf(settings.gaplessEnabled) }
    var currentSpeed by remember { mutableFloatStateOf(settings.playbackSpeed) }
    var activePresetName by remember { mutableStateOf(settings.eqPreset) }

    val bandFrequencies = remember {
        listOf("31", "62", "125", "250", "500", "1k", "2k", "4k", "8k", "16k")
    }

    val bandGains = remember {
        mutableStateListOf<Float>().apply {
            if (settings.eqBands.size == 10) {
                addAll(settings.eqBands)
            } else {
                repeat(10) { add(0f) }
            }
        }
    }

    val calibratedPresets = remember {
        listOf(
            CalibratedEqPreset(
                id = "flat",
                name = "Flat (Studio)",
                nameFa = "استودیویی تخت (Flat)",
                bands = listOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f),
                recommendedBassBoost = 0
            ),
            CalibratedEqPreset(
                id = "sub_bass_punch",
                name = "Extreme Sub-Bass",
                nameFa = "بیس سنگین و ساب‌ووفر",
                bands = listOf(10.5f, 9.5f, 7.0f, 3.5f, 0.5f, 0.0f, 1.0f, 2.5f, 3.5f, 4.0f),
                recommendedBassBoost = 850
            ),
            CalibratedEqPreset(
                id = "hiphop_808",
                name = "Hip-Hop & 808 Punch",
                nameFa = "هیپ‌هاپ و رپ (808 Punch)",
                bands = listOf(9.5f, 8.5f, 6.0f, 2.0f, -1.0f, 1.0f, 2.5f, 3.5f, 4.5f, 5.0f),
                recommendedBassBoost = 700
            ),
            CalibratedEqPreset(
                id = "edm",
                name = "Electronic / Club",
                nameFa = "الکترونیک و کلاب",
                bands = listOf(8.5f, 7.5f, 4.5f, 0.5f, -1.5f, 1.5f, 3.0f, 5.0f, 6.5f, 7.0f),
                recommendedBassBoost = 600
            ),
            CalibratedEqPreset(
                id = "rock",
                name = "Rock & Metal",
                nameFa = "راک و متال",
                bands = listOf(6.5f, 5.5f, 3.5f, -0.5f, -1.5f, 2.0f, 4.0f, 5.5f, 6.0f, 6.0f),
                recommendedBassBoost = 400
            ),
            CalibratedEqPreset(
                id = "pop",
                name = "Pop & Crystal Vocal",
                nameFa = "پاپ و وکال شفاف",
                bands = listOf(3.5f, 4.5f, 4.0f, 1.5f, 1.0f, 2.5f, 4.0f, 5.0f, 4.5f, 3.5f),
                recommendedBassBoost = 350
            ),
            CalibratedEqPreset(
                id = "vocal",
                name = "Vocal & Clarity",
                nameFa = "وضوح صدای خواننده",
                bands = listOf(-2.0f, -1.0f, 1.0f, 3.0f, 5.5f, 6.5f, 5.5f, 4.0f, 2.5f, 1.5f),
                recommendedBassBoost = 100
            )
        )
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onClose,
        sheetState = sheetState,
        containerColor = Color(0xFF0B0D18),
        scrimColor = Color(0xFF030308).copy(alpha = 0.85f),
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color(0x66FFFFFF)) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .navigationBarsPadding()
        ) {
            // هدر ثابت اکولایزر
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(palette.primary.copy(alpha = 0.25f))
                            .border(1.dp, palette.accent.copy(alpha = 0.5f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = null,
                            tint = palette.accent,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = if (isFa) "استودیوی اکولایزر و تقویت بیس (DSP)" else "Pro 10-Band DSP & Sub-Bass",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                        )
                        Text(
                            text = if (eqEnabled) {
                                if (isFa) "موتور سخت‌افزاری + ساب‌ووفر فعال" else "Hardware EQ + Sub-Bass Active"
                            } else {
                                if (isFa) "غیرفعال (صدای خام)" else "Bypassed"
                            },
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = if (eqEnabled) palette.accent else Color(0xFF88889C),
                                fontSize = 11.5.sp
                            )
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // دکمه ریست سریع به حالت Flat
                    IconButton(
                        onClick = {
                            activePresetName = "Flat (Studio)"
                            for (i in 0 until 10) bandGains[i] = 0f
                            bassBoost = 0f
                            onUpdateSettings(
                                settings.copy(
                                    eqPreset = "Flat (Studio)",
                                    eqBands = List(10) { 0f },
                                    bassBoostStrength = 0
                                )
                            )
                        }
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reset", tint = Color.White.copy(alpha = 0.75f))
                    }

                    Switch(
                        checked = eqEnabled,
                        onCheckedChange = {
                            eqEnabled = it
                            onUpdateSettings(settings.copy(equalizerEnabled = it))
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = palette.accent
                        )
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(modifier = Modifier.height(12.dp))

                // ۱. پریست‌های کالیبره‌شده
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(calibratedPresets) { preset ->
                        val isSelected = activePresetName == preset.name
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                eqEnabled = true
                                activePresetName = preset.name
                                for (i in 0 until 10) {
                                    bandGains[i] = preset.bands.getOrElse(i) { 0f }
                                }
                                bassBoost = preset.recommendedBassBoost.toFloat()
                                onUpdateSettings(
                                    settings.copy(
                                        equalizerEnabled = true,
                                        eqPreset = preset.name,
                                        eqBands = bandGains.toList(),
                                        bassBoostStrength = preset.recommendedBassBoost
                                    )
                                )
                            },
                            leadingIcon = if (isSelected) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White) }
                            } else null,
                            label = {
                                Text(
                                    text = preset.getName(lang),
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium
                                )
                            },
                            shape = RoundedCornerShape(18.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = palette.primary,
                                selectedLabelColor = Color.White,
                                containerColor = Color(0x1FFFFFFF),
                                labelColor = Color(0xFFC0C5D8)
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ۲. منحنی زنده پاسخ فرکانسی (شامل تأثیر Sub-Bass Boost روی منحنی)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(92.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0xFF121526))
                        .border(1.dp, palette.primary.copy(alpha = 0.35f), RoundedCornerShape(18.dp))
                        .padding(10.dp)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height
                        val zeroY = h / 2f

                        drawLine(
                            color = Color(0x33FFFFFF),
                            start = Offset(0f, zeroY),
                            end = Offset(w, zeroY),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                        )

                        val bassNorm = (bassBoost / 1000f).coerceIn(0f, 1f)
                        val count = bandGains.size
                        if (count >= 2) {
                            val points = List(count) { i ->
                                val px = (i.toFloat() / (count - 1)) * (w - 20f) + 10f
                                val subBoostExtra = if (i <= 2) (bassNorm * (3 - i) * 2.2f) else 0f
                                val totalGain = (bandGains[i] + subBoostExtra).coerceIn(-12f, 14f)
                                val py = (zeroY - (totalGain / 14f) * (h * 0.44f)).coerceIn(4f, h - 4f)
                                Offset(px, py)
                            }

                            val curvePath = Path().apply {
                                moveTo(points[0].x, points[0].y)
                                for (i in 0 until points.size - 1) {
                                    val p0 = points[i]
                                    val p1 = points[i + 1]
                                    val midX = (p0.x + p1.x) / 2f
                                    cubicTo(midX, p0.y, midX, p1.y, p1.x, p1.y)
                                }
                            }

                            val fillPath = Path().apply {
                                addPath(curvePath)
                                lineTo(points.last().x, h)
                                lineTo(points.first().x, h)
                                close()
                            }

                            drawPath(
                                path = fillPath,
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        palette.accent.copy(alpha = 0.42f),
                                        palette.primary.copy(alpha = 0.15f),
                                        Color.Transparent
                                    )
                                )
                            )

                            drawPath(
                                path = curvePath,
                                brush = Brush.horizontalGradient(listOf(palette.accent, palette.primary, palette.secondary)),
                                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                            )

                            points.forEach { pt ->
                                drawCircle(color = Color.White, radius = 3.5.dp.toPx(), center = pt)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // ۳. فیدرهای لمسی عمودی واقعی ۱۰ بانده (Studio Vertical Touch Faders)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(Color(0xFF131628))
                        .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(22.dp))
                        .padding(horizontal = 8.dp, vertical = 14.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(210.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        bandFrequencies.forEachIndexed { idx, freqLabel ->
                            StudioVerticalEqFader(
                                gainDb = bandGains[idx],
                                freqLabel = freqLabel,
                                enabled = eqEnabled,
                                primaryColor = palette.primary,
                                accentColor = palette.accent,
                                onGainChanged = { newDb ->
                                    bandGains[idx] = newDb
                                    activePresetName = "Custom"
                                    onUpdateSettings(
                                        settings.copy(
                                            equalizerEnabled = true,
                                            eqPreset = "Custom",
                                            eqBands = bandGains.toList()
                                        )
                                    )
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // ۴. تقویت‌کننده قدرتمند ساب‌ووفر و بیس (Deep Sub-Bass Punch)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    palette.primary.copy(alpha = 0.24f),
                                    Color(0xFF141728)
                                )
                            )
                        )
                        .border(1.2.dp, palette.accent.copy(alpha = 0.45f), RoundedCornerShape(22.dp))
                        .padding(18.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = if (isFa) "🔥 قدرت کوبندگی بیس و ساب‌ووفر (Sub-Bass Punch)" else "🔥 Deep Sub-Bass & 808 Punch",
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 14.5.sp
                                )
                                Text(
                                    text = if (isFa) "تقویت همزمان فرکانس‌های زیرین (31-125Hz) و Loudness بدون افت صدا"
                                    else "Simultaneous hardware BassBoost + Low-Shelf + Loudness DSP",
                                    color = Color(0xFFB4B9CE),
                                    fontSize = 11.5.sp
                                )
                            }
                            Text(
                                text = "${(bassBoost / 10).toInt()}%",
                                color = palette.accent,
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp
                            )
                        }

                        Slider(
                            value = bassBoost,
                            onValueChange = {
                                bassBoost = it
                                eqEnabled = true
                                onUpdateSettings(
                                    settings.copy(
                                        equalizerEnabled = true,
                                        bassBoostStrength = it.toInt()
                                    )
                                )
                            },
                            valueRange = 0f..1000f,
                            colors = SliderDefaults.colors(
                                thumbColor = Color.White,
                                activeTrackColor = palette.accent,
                                inactiveTrackColor = Color(0x33FFFFFF)
                            )
                        )

                        HorizontalDivider(color = Color.White.copy(alpha = 0.10f))

                        // سرعت پخش (Pitch & Speed)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isFa) "سرعت پخش (Playback Speed)" else "Playback Speed",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.5.sp
                            )
                            Text(
                                text = "${"%.2f".format(currentSpeed)}x",
                                color = palette.accent,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.5.sp
                            )
                        }

                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            val speeds = listOf(0.75f, 0.90f, 1.0f, 1.15f, 1.25f, 1.5f, 2.0f)
                            items(speeds) { speed ->
                                FilterChip(
                                    selected = (currentSpeed == speed),
                                    onClick = {
                                        currentSpeed = speed
                                        onUpdateSettings(settings.copy(playbackSpeed = speed))
                                    },
                                    label = { Text("${speed}x", fontSize = 11.5.sp, fontWeight = FontWeight.Bold) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = palette.primary,
                                        selectedLabelColor = Color.White,
                                        containerColor = Color(0x1AFFFFFF),
                                        labelColor = Color(0xFFB4B9CE)
                                    )
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))
            }
        }
    }
}

/**
 * فیدر لمسی عمودی اختصاصی به سبک استودیوهای آهنگسازی (بدون باگ اسلایدر افقی فشرده)
 */
@Composable
private fun StudioVerticalEqFader(
    gainDb: Float,
    freqLabel: String,
    enabled: Boolean,
    primaryColor: Color,
    accentColor: Color,
    onGainChanged: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentOnGainChanged by rememberUpdatedState(onGainChanged)

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "${if (gainDb > 0) "+" else ""}${gainDb.roundToInt()}",
            fontSize = 10.sp,
            fontWeight = FontWeight.ExtraBold,
            color = if (gainDb != 0f && enabled) accentColor else Color(0xFF888EA8)
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(vertical = 6.dp)
                .pointerInput(enabled) {
                    if (!enabled) return@pointerInput
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        down.consume()
                        val h = size.height.toFloat().coerceAtLeast(1f)

                        fun yToDb(y: Float): Float {
                            val norm = 1f - (y / h).coerceIn(0f, 1f) // 0 at bottom (-12dB), 1 at top (+12dB)
                            val rawDb = (norm * 24f) - 12f
                            return ( (rawDb * 2f).roundToInt() / 2f ).coerceIn(-12f, 12f)
                        }

                        currentOnGainChanged(yToDb(down.position.y))

                        var pointerId = down.id
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == pointerId } ?: event.changes.firstOrNull()
                            if (change == null || !change.pressed) {
                                change?.consume()
                                break
                            }
                            pointerId = change.id
                            if (change.positionChange() != Offset.Zero) {
                                change.consume()
                            }
                            currentOnGainChanged(yToDb(change.position.y))
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val centerX = w / 2f
                val trackW = 6.dp.toPx()
                val zeroY = h / 2f

                // شیار پس‌زمینه فیدر عمودی
                drawRoundRect(
                    color = Color(0x28FFFFFF),
                    topLeft = Offset(centerX - trackW / 2f, 0f),
                    size = Size(trackW, h),
                    cornerRadius = CornerRadius(trackW / 2f, trackW / 2f)
                )

                // خط مرجع 0dB
                drawLine(
                    color = Color.White.copy(alpha = 0.35f),
                    start = Offset(centerX - trackW * 1.4f, zeroY),
                    end = Offset(centerX + trackW * 1.4f, zeroY),
                    strokeWidth = 1.dp.toPx()
                )

                // محاسبه موقعیت عمودی دستگیره بر اساس gainDb (-12 تا +12)
                val norm = ((gainDb.coerceIn(-12f, 12f) + 12f) / 24f).coerceIn(0f, 1f)
                val thumbY = h * (1f - norm)

                // نوار رنگی پرشده از مرکز (0dB) تا موقعیت دستگیره
                val fillTop = minOf(zeroY, thumbY)
                val fillBottom = maxOf(zeroY, thumbY)
                val fillHeight = (fillBottom - fillTop).coerceAtLeast(2f)

                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = if (enabled) listOf(accentColor, primaryColor) else listOf(Color.Gray, Color.DarkGray)
                    ),
                    topLeft = Offset(centerX - trackW / 2f, fillTop),
                    size = Size(trackW, fillHeight),
                    cornerRadius = CornerRadius(trackW / 2f, trackW / 2f)
                )

                // دستگیره نوری (Thumb)
                val thumbR = 7.dp.toPx()
                drawCircle(
                    color = if (enabled) accentColor.copy(alpha = 0.40f) else Color.Transparent,
                    radius = thumbR * 1.7f,
                    center = Offset(centerX, thumbY)
                )
                drawCircle(
                    color = if (enabled) Color.White else Color.Gray,
                    radius = thumbR,
                    center = Offset(centerX, thumbY)
                )
                drawCircle(
                    color = if (enabled) accentColor else Color.DarkGray,
                    radius = thumbR * 0.55f,
                    center = Offset(centerX, thumbY)
                )
            }
        }

        Text(
            text = freqLabel,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFFA5ABC0)
        )
    }
}
