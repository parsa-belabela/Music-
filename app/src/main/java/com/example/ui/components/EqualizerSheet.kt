package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.AmbientPalette
import com.example.data.model.AppLanguage
import com.example.data.model.AppSettings

data class CalibratedEqPreset(
    val id: String,
    val name: String,
    val nameFa: String = name,
    val bands: List<Float>, // 31Hz, 62Hz, 125Hz, 250Hz, 500Hz, 1kHz, 2kHz, 4kHz, 8kHz, 16kHz
    val recommendedBassBoost: Int = 0
) {
    fun getName(lang: AppLanguage): String = if (lang == AppLanguage.PERSIAN) nameFa else name
}

/**
 * Audiophile-Calibrated Equalizer & DSP Audio Sheet.
 * - 12 scientifically engineered acoustic frequency curves for studio reference
 * - Instant, responsive preset switching with dynamic visual state indication
 * - Real-time 10-band graphic EQ (-12dB to +12dB) & DSP bass enhancer
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EqualizerSheet(
    settings: AppSettings,
    palette: AmbientPalette,
    onUpdateSettings: (AppSettings) -> Unit,
    onClose: () -> Unit
) {
    val lang = settings.language
    var eqEnabled by remember { mutableStateOf(settings.equalizerEnabled) }
    var bassBoost by remember { mutableFloatStateOf(settings.bassBoostStrength.toFloat()) }
    var crossfade by remember { mutableIntStateOf(settings.crossfadeDurationSeconds) }
    var gapless by remember { mutableStateOf(settings.gaplessEnabled) }
    var currentSpeed by remember { mutableFloatStateOf(settings.playbackSpeed) }
    var activePresetName by remember { mutableStateOf(settings.eqPreset) }

    val bandFrequencies = remember {
        listOf("31Hz", "62Hz", "125Hz", "250Hz", "500Hz", "1kHz", "2kHz", "4kHz", "8kHz", "16kHz")
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
                bands = listOf(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f),
                recommendedBassBoost = 0
            ),
            CalibratedEqPreset(
                id = "bass_boost",
                name = "Bass Boost (Deep)",
                nameFa = "تقویت بیس عمیق",
                bands = listOf(8.5f, 7.5f, 6.0f, 3.5f, 1.0f, 0.0f, -0.5f, 0.0f, 1.5f, 2.0f),
                recommendedBassBoost = 650
            ),
            CalibratedEqPreset(
                id = "edm",
                name = "Electronic / EDM",
                nameFa = "الکترونیک / EDM",
                bands = listOf(7.5f, 6.5f, 4.0f, 0.5f, -1.5f, 1.0f, 2.5f, 4.5f, 6.0f, 6.5f),
                recommendedBassBoost = 450
            ),
            CalibratedEqPreset(
                id = "rock",
                name = "Rock & Metal",
                nameFa = "راک و متال",
                bands = listOf(5.5f, 4.5f, 3.0f, -0.5f, -1.5f, 1.5f, 3.0f, 4.5f, 5.0f, 5.5f),
                recommendedBassBoost = 250
            ),
            CalibratedEqPreset(
                id = "pop",
                name = "Pop Hits",
                nameFa = "پاپ (Pop)",
                bands = listOf(2.0f, 3.5f, 4.0f, 1.5f, 0.5f, 2.0f, 3.5f, 4.5f, 4.0f, 3.0f),
                recommendedBassBoost = 200
            ),
            CalibratedEqPreset(
                id = "hiphop",
                name = "Hip-Hop & R&B",
                nameFa = "هیپ‌هاپ و R&B",
                bands = listOf(8.0f, 7.5f, 5.5f, 2.0f, -1.0f, 0.5f, 2.0f, 1.5f, 3.5f, 4.0f),
                recommendedBassBoost = 500
            ),
            CalibratedEqPreset(
                id = "vocal",
                name = "Vocal & Podcast",
                nameFa = "وکال و پادکست",
                bands = listOf(-3.0f, -1.5f, 0.5f, 2.0f, 4.5f, 5.5f, 4.5f, 3.0f, 1.5f, 0.5f),
                recommendedBassBoost = 0
            ),
            CalibratedEqPreset(
                id = "acoustic",
                name = "Acoustic / Warm",
                nameFa = "آکوستیک و گرم",
                bands = listOf(1.0f, 2.0f, 3.0f, 2.0f, 1.5f, 2.0f, 3.0f, 4.0f, 3.5f, 2.5f),
                recommendedBassBoost = 150
            ),
            CalibratedEqPreset(
                id = "classical",
                name = "Classical & Symphony",
                nameFa = "کلاسیک و سمفونی",
                bands = listOf(4.0f, 3.5f, 2.5f, 1.0f, 0.0f, 0.5f, 2.0f, 3.5f, 4.5f, 5.0f),
                recommendedBassBoost = 100
            ),
            CalibratedEqPreset(
                id = "jazz",
                name = "Jazz & Soul",
                nameFa = "جاز و بلوز",
                bands = listOf(3.5f, 3.0f, 2.0f, 1.5f, 0.5f, 1.0f, 2.0f, 3.0f, 3.5f, 3.5f),
                recommendedBassBoost = 150
            ),
            CalibratedEqPreset(
                id = "chill",
                name = "Lounge / Chill",
                nameFa = "ریلکس و آرامش‌بخش",
                bands = listOf(5.0f, 4.5f, 3.0f, 1.5f, 1.0f, 1.5f, 2.0f, 2.0f, 1.5f, 0.5f),
                recommendedBassBoost = 300
            ),
            CalibratedEqPreset(
                id = "treble",
                name = "Bright Treble",
                nameFa = "تقویت فرکانس‌های بالا",
                bands = listOf(-3.0f, -2.0f, -1.0f, 0.5f, 2.0f, 4.0f, 6.0f, 7.5f, 8.5f, 9.0f),
                recommendedBassBoost = 0
            )
        )
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onClose,
        sheetState = sheetState,
        containerColor = Color(0xFF0E0E1A),
        scrimColor = Color(0xFF030308).copy(alpha = 0.85f),
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color(0x66FFFFFF)) },
        modifier = Modifier.fillMaxHeight(0.92f)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Fixed Non-Scrollable Header
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
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(palette.primary.copy(alpha = 0.20f))
                            .border(1.dp, palette.primary.copy(alpha = 0.40f), CircleShape),
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
                            text = if (lang == AppLanguage.PERSIAN) "اکولایزر و پردازشگر صدا (DSP)" else "Audiophile DSP Equalizer",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                        Text(
                            text = if (eqEnabled) {
                                if (lang == AppLanguage.PERSIAN) "پردازش ۱۰ کاناله سخت‌افزاری فعال" else "10-Band Hardware DSP Active"
                            } else {
                                if (lang == AppLanguage.PERSIAN) "غیرفعال (صدای اصلی)" else "Bypassed"
                            },
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = if (eqEnabled) palette.accent else Color(0xFF88889C),
                                fontSize = 11.sp
                            )
                        )
                    }
                }

                Switch(
                    checked = eqEnabled,
                    onCheckedChange = {
                        eqEnabled = it
                        onUpdateSettings(settings.copy(equalizerEnabled = it))
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = palette.primary
                    )
                )
            }

            // Scrollable Content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 20.dp)
                    .verticalScroll(rememberScrollState())
            ) {

            Spacer(modifier = Modifier.height(20.dp))

            // Presets Horizontal list with active selection indicator
            Text(
                text = if (lang == AppLanguage.PERSIAN) "پریست‌های کالیبره‌شده" else "CALIBRATED PRESETS",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = palette.accent,
                    letterSpacing = 1.2.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            Spacer(modifier = Modifier.height(10.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(calibratedPresets) { preset ->
                    val isSelected = activePresetName == preset.name

                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            activePresetName = preset.name
                            for (i in 0 until 10) {
                                bandGains[i] = preset.bands.getOrElse(i) { 0f }
                            }
                            if (preset.recommendedBassBoost > 0) {
                                bassBoost = preset.recommendedBassBoost.toFloat()
                            }
                            onUpdateSettings(
                                settings.copy(
                                    eqPreset = preset.name,
                                    eqBands = bandGains.toList(),
                                    bassBoostStrength = preset.recommendedBassBoost
                                )
                            )
                        },
                        leadingIcon = if (isSelected) {
                            {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = Color.White
                                )
                            }
                        } else null,
                        label = {
                            Text(
                                text = preset.getName(lang),
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        shape = RoundedCornerShape(20.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = palette.primary,
                            selectedLabelColor = Color.White,
                            containerColor = Color(0x22FFFFFF),
                            labelColor = Color(0xFFC0C0D4)
                        ),
                        border = if (isSelected) {
                            FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = true,
                                borderColor = palette.accent,
                                borderWidth = 1.5.dp
                            )
                        } else {
                            FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = false,
                                borderColor = Color(0x1FFFFFFF),
                                borderWidth = 1.dp
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            // 10-Band Graphic EQ representation & Interactive Frequency Response Curve
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (lang == AppLanguage.PERSIAN) "منحنی پاسخ فرکانسی آکوستیک" else "ACOUSTIC FREQUENCY RESPONSE",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color(0xFFA5ABC0),
                        letterSpacing = 1.2.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = if (activePresetName.isNotBlank()) {
                        calibratedPresets.find { it.name == activePresetName }?.getName(lang) ?: (if (lang == AppLanguage.PERSIAN) "شخصی‌سازی" else "Custom")
                    } else (if (lang == AppLanguage.PERSIAN) "شخصی‌سازی" else "Custom"),
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = palette.accent,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
            Spacer(modifier = Modifier.height(10.dp))

            // Live Spline Acoustic Curve Visualizer
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF0C0E1A))
                    .border(1.dp, palette.primary.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
                    .padding(8.dp)
            ) {
                androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val zeroY = h / 2f

                    // Draw 0dB reference line
                    drawLine(
                        color = Color(0x33FFFFFF),
                        start = androidx.compose.ui.geometry.Offset(0f, zeroY),
                        end = androidx.compose.ui.geometry.Offset(w, zeroY),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                    )

                    val count = bandGains.size
                    if (count >= 2) {
                        val points = List(count) { i ->
                            val px = (i.toFloat() / (count - 1)) * (w - 24f) + 12f
                            val gain = bandGains[i].coerceIn(-12f, 12f)
                            // Map -12dB..+12dB to bottom..top
                            val py = zeroY - (gain / 12f) * (h * 0.40f)
                            androidx.compose.ui.geometry.Offset(px, py)
                        }

                        val curvePath = androidx.compose.ui.graphics.Path().apply {
                            moveTo(points[0].x, points[0].y)
                            for (i in 0 until points.size - 1) {
                                val p0 = points[i]
                                val p1 = points[i + 1]
                                val midX = (p0.x + p1.x) / 2f
                                cubicTo(midX, p0.y, midX, p1.y, p1.x, p1.y)
                            }
                        }

                        val fillPath = androidx.compose.ui.graphics.Path().apply {
                            addPath(curvePath)
                            lineTo(points.last().x, zeroY)
                            lineTo(points.first().x, zeroY)
                            close()
                        }

                        // Gradient fill under curve
                        drawPath(
                            path = fillPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    palette.primary.copy(alpha = 0.35f),
                                    palette.secondary.copy(alpha = 0.10f),
                                    Color.Transparent
                                ),
                                startY = 0f,
                                endY = h
                            )
                        )

                        // Glowing curve stroke
                        drawPath(
                            path = curvePath,
                            brush = Brush.horizontalGradient(
                                listOf(palette.primary, palette.accent, palette.secondary)
                            ),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(
                                width = 2.5.dp.toPx(),
                                cap = androidx.compose.ui.graphics.StrokeCap.Round
                            )
                        )

                        // Points
                        points.forEach { pt ->
                            drawCircle(
                                color = palette.accent,
                                radius = 3.dp.toPx(),
                                center = pt
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFF141424))
                    .border(1.dp, Color(0x18FFFFFF), RoundedCornerShape(18.dp))
                    .padding(horizontal = 10.dp, vertical = 14.dp)
            ) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(170.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    items(bandFrequencies.indices.toList()) { idx ->
                        Column(
                            modifier = Modifier
                                .width(32.dp)
                                .fillMaxHeight(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${bandGains[idx].toInt()}dB",
                                fontSize = 10.sp,
                                color = if (bandGains[idx] != 0f) palette.accent else Color(0xFF88889E),
                                fontWeight = FontWeight.Bold
                            )

                            // Vertical slider representation
                            Slider(
                                value = bandGains[idx],
                                onValueChange = {
                                    bandGains[idx] = it
                                    activePresetName = "Custom"
                                    onUpdateSettings(
                                        settings.copy(
                                            eqPreset = "Custom",
                                            eqBands = bandGains.toList()
                                        )
                                    )
                                },
                                valueRange = -12f..12f,
                                modifier = Modifier
                                    .height(115.dp)
                                    .width(30.dp),
                                enabled = eqEnabled,
                                colors = SliderDefaults.colors(
                                    thumbColor = palette.accent,
                                    activeTrackColor = palette.primary,
                                    inactiveTrackColor = Color(0x33FFFFFF)
                                )
                            )

                            Text(
                                text = bandFrequencies[idx],
                                fontSize = 9.sp,
                                color = Color(0xFF8E8EA8),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Sub-Bass Boost & DSP Section
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF141424)),
                shape = RoundedCornerShape(18.dp),
                border = CardDefaults.outlinedCardBorder().copy(brush = Brush.verticalGradient(listOf(Color(0x30FFFFFF), Color(0x0AFFFFFF)))),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Sub-Bass Boost
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (lang == AppLanguage.PERSIAN) "تقویت‌کننده بیس عمیق (Bass Boost)" else "Deep Bass Enhancer",
                            style = MaterialTheme.typography.bodyMedium.copy(color = Color.White, fontWeight = FontWeight.SemiBold)
                        )
                        Text(
                            text = "${(bassBoost / 10).toInt()}%",
                            style = MaterialTheme.typography.bodyMedium.copy(color = palette.accent, fontWeight = FontWeight.Bold)
                        )
                    }
                    Slider(
                        value = bassBoost,
                        onValueChange = {
                            bassBoost = it
                            onUpdateSettings(settings.copy(bassBoostStrength = it.toInt()))
                        },
                        valueRange = 0f..1000f,
                        colors = SliderDefaults.colors(thumbColor = palette.accent, activeTrackColor = palette.primary)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Crossfade
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (lang == AppLanguage.PERSIAN) "محو و اتصال بین آهنگ‌ها (Crossfade)" else "Crossfade Between Songs",
                            style = MaterialTheme.typography.bodyMedium.copy(color = Color.White, fontWeight = FontWeight.SemiBold)
                        )
                        Text(
                            text = if (crossfade > 0) {
                                if (lang == AppLanguage.PERSIAN) "$crossfade ثانیه" else "${crossfade}s"
                            } else {
                                if (lang == AppLanguage.PERSIAN) "خاموش" else "Off"
                            },
                            style = MaterialTheme.typography.bodyMedium.copy(color = palette.accent, fontWeight = FontWeight.Bold)
                        )
                    }
                    Slider(
                        value = crossfade.toFloat(),
                        onValueChange = {
                            crossfade = it.toInt()
                            onUpdateSettings(settings.copy(crossfadeDurationSeconds = it.toInt()))
                        },
                        valueRange = 0f..12f,
                        steps = 11,
                        colors = SliderDefaults.colors(thumbColor = palette.secondary, activeTrackColor = palette.secondary)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = if (lang == AppLanguage.PERSIAN) "پخش بدون وقفه (Gapless)" else "Gapless Playback",
                                style = MaterialTheme.typography.bodyMedium.copy(color = Color.White, fontWeight = FontWeight.SemiBold)
                            )
                            Text(
                                text = if (lang == AppLanguage.PERSIAN) "انتقال یکپارچه بین قطعات بدون سکوت" else "Continuous transitions without pause",
                                style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFA0A0B5), fontSize = 11.sp)
                            )
                        }
                        Switch(
                            checked = gapless,
                            onCheckedChange = {
                                gapless = it
                                onUpdateSettings(settings.copy(gaplessEnabled = it))
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = palette.secondary)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Playback Speed
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (lang == AppLanguage.PERSIAN) "سرعت پخش موسیقی" else "Audio Playback Pitch & Speed",
                            style = MaterialTheme.typography.bodyMedium.copy(color = Color.White, fontWeight = FontWeight.SemiBold)
                        )
                        Text(
                            text = "${"%.2f".format(currentSpeed)}x",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = palette.accent,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val speeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f)
                        items(speeds) { speed ->
                            FilterChip(
                                selected = (currentSpeed == speed),
                                onClick = {
                                    currentSpeed = speed
                                    onUpdateSettings(settings.copy(playbackSpeed = speed))
                                },
                                label = { Text("${speed}x", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = palette.primary,
                                    selectedLabelColor = Color.White,
                                    containerColor = Color(0x18FFFFFF),
                                    labelColor = Color(0xFFA5ABC0)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}
}
