package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.AmbientPalette
import com.example.data.model.AppSettings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EqualizerSheet(
    settings: AppSettings,
    palette: AmbientPalette,
    onUpdateSettings: (AppSettings) -> Unit,
    onClose: () -> Unit
) {
    var eqEnabled by remember { mutableStateOf(settings.equalizerEnabled) }
    var bassBoost by remember { mutableFloatStateOf(settings.bassBoostStrength.toFloat()) }
    var crossfade by remember { mutableIntStateOf(settings.crossfadeDurationSeconds) }
    var gapless by remember { mutableStateOf(settings.gaplessEnabled) }

    val bandFrequencies = listOf("31Hz", "62Hz", "125Hz", "250Hz", "500Hz", "1kHz", "2kHz", "4kHz", "8kHz", "16kHz")
    val bandGains = remember { mutableStateListOf<Float>().apply { addAll(settings.eqBands) } }

    val eqPresets = listOf(
        "Flat" to listOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f),
        "Bass Heavy" to listOf(7f, 6f, 5f, 3f, 1f, 0f, -1f, -1f, 0f, 1f),
        "Electronic" to listOf(5f, 4f, 2f, 0f, -2f, 2f, 1f, 3f, 4f, 5f),
        "Vocal Boost" to listOf(-2f, -1f, 0f, 2f, 5f, 5f, 3f, 2f, 0f, -1f),
        "Rock" to listOf(5f, 3f, 2f, 0f, -1f, 0f, 2f, 3f, 4f, 4f),
        "Chill / Ambient" to listOf(3f, 4f, 2f, 1f, 0f, 0f, 1f, 2f, 3f, 4f)
    )

    ModalBottomSheet(
        onDismissRequest = onClose,
        containerColor = Color(0xFF10101E),
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color(0x66FFFFFF)) },
        modifier = Modifier.fillMaxHeight(0.88f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.GraphicEq, contentDescription = null, tint = palette.accent)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Equalizer & DSP Audio",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                }
                Switch(
                    checked = eqEnabled,
                    onCheckedChange = {
                        eqEnabled = it
                        onUpdateSettings(settings.copy(equalizerEnabled = it))
                    },
                    colors = SwitchDefaults.colors(checkedThumbColor = palette.primary)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Presets Horizontal list
            Text("Presets", style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFA0A0B0)))
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(eqPresets) { (name, presetGains) ->
                    SuggestionChip(
                        onClick = {
                            for (i in bandGains.indices) {
                                bandGains[i] = presetGains.getOrElse(i) { 0f }
                            }
                            onUpdateSettings(settings.copy(eqBands = bandGains.toList()))
                        },
                        label = { Text(name, fontSize = 12.sp) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 10 Band sliders
            Text("10-Band Graphic EQ (-12dB to +12dB)", style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFA0A0B0)))
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                items(bandFrequencies.indices.toList()) { idx ->
                    Column(
                        modifier = Modifier.width(34.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${bandGains[idx].toInt()}dB",
                            fontSize = 10.sp,
                            color = palette.accent,
                            fontWeight = FontWeight.Bold
                        )

                        // Vertical slider representation
                        Slider(
                            value = bandGains[idx],
                            onValueChange = {
                                bandGains[idx] = it
                                onUpdateSettings(settings.copy(eqBands = bandGains.toList()))
                            },
                            valueRange = -12f..12f,
                            modifier = Modifier
                                .height(110.dp)
                                .width(32.dp),
                            enabled = eqEnabled,
                            colors = SliderDefaults.colors(thumbColor = palette.primary, activeTrackColor = palette.secondary)
                        )

                        Text(
                            text = bandFrequencies[idx],
                            fontSize = 10.sp,
                            color = Color(0xFF88889A)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Bass Boost & Crossfade
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF18182C)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Bass Boost
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Bass Boost", style = MaterialTheme.typography.bodyMedium.copy(color = Color.White))
                        Text("${(bassBoost / 10).toInt()}%", style = MaterialTheme.typography.bodyMedium.copy(color = palette.accent))
                    }
                    Slider(
                        value = bassBoost,
                        onValueChange = {
                            bassBoost = it
                            onUpdateSettings(settings.copy(bassBoostStrength = it.toInt()))
                        },
                        valueRange = 0f..1000f,
                        colors = SliderDefaults.colors(thumbColor = palette.primary, activeTrackColor = palette.primary)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Crossfade
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Crossfade Duration", style = MaterialTheme.typography.bodyMedium.copy(color = Color.White))
                        Text("${crossfade}s", style = MaterialTheme.typography.bodyMedium.copy(color = palette.accent))
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

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Gapless Playback", style = MaterialTheme.typography.bodyMedium.copy(color = Color.White))
                        Switch(
                            checked = gapless,
                            onCheckedChange = {
                                gapless = it
                                onUpdateSettings(settings.copy(gaplessEnabled = it))
                            }
                        )
                    }
                }
            }
        }
    }
}
