package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HearingProfileTestSheet(
    palette: AmbientPalette,
    lang: AppLanguage,
    onApplyCalibratedProfile: (List<Float>) -> Unit,
    onDismiss: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var currentStep by remember { mutableStateOf(0) }

    // User preferences across 5 perceptual frequency ranges
    val userScores = remember { mutableStateListOf(0f, 0f, 0f, 0f, 0f) }

    val steps = listOf(
        "Sub-Bass (31Hz - 62Hz)" to if (lang == AppLanguage.PERSIAN) "قدرت بیس عمیق و لرزش فرکانس‌های زیرین" else "Deep sub-bass impact & low-end rumble",
        "Bass & Low Mids (125Hz - 250Hz)" to if (lang == AppLanguage.PERSIAN) "گرمی صدای خواننده مرد و بدنه گیتار بیس" else "Vocal warmth & bassline body",
        "Midrange (500Hz - 1kHz)" to if (lang == AppLanguage.PERSIAN) "وضوح ملودی اصلی، پیانو و وکال زنانه" else "Main melodies, piano clarity & female vocals",
        "Upper Mids (2kHz - 4kHz)" to if (lang == AppLanguage.PERSIAN) "شفافیت ادای کلمات و حمله سازهای ضربی" else "Vocal articulation & snare presence",
        "Highs & Air (8kHz - 16kHz)" to if (lang == AppLanguage.PERSIAN) "درخشش سنج‌ها، کریستالی بودن صدا و هوا" else "Cymbal shimmer, sparkle & acoustic air"
    )

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
                    text = if (lang == AppLanguage.PERSIAN) "تست کالیبراسیون شنوایی" else "Personal Hearing Calibration",
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

            Spacer(modifier = Modifier.height(24.dp))

            if (currentStep < steps.size) {
                val (rangeTitle, rangeDesc) = steps[currentStep]

                Text(
                    text = if (lang == AppLanguage.PERSIAN) "مرحله ${currentStep + 1} از ${steps.size}" else "Step ${currentStep + 1} of ${steps.size}",
                    style = MaterialTheme.typography.labelMedium.copy(color = palette.accent)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = rangeTitle,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    ),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = rangeDesc,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color(0xFFA0A5BA),
                        lineHeight = 20.sp
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(28.dp))

                // 3 Simple tactile choices
                val choices = listOf(
                    ((if (lang == AppLanguage.PERSIAN) "طبیعی و تخت" else "Flat / Natural") to if (lang == AppLanguage.PERSIAN) "طبیعی و دست‌نخورده" else "Natural, as produced") to 0.0f,
                    ((if (lang == AppLanguage.PERSIAN) "تقویت ملایم" else "Enhanced") to if (lang == AppLanguage.PERSIAN) "کمی تقویت‌شده (+۲.۵ dB)" else "Subtle boost (+2.5 dB)") to 2.5f,
                    ((if (lang == AppLanguage.PERSIAN) "پرقدرت و کوبنده" else "Vibrant Punch") to if (lang == AppLanguage.PERSIAN) "قدرتمند و پررنگ (+۵ dB)" else "Rich emphasis (+5.0 dB)") to 5.0f
                )

                choices.forEach { (textPair, boostValue) ->
                    val (title, sub) = textPair
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .liquidGlass(
                                shape = RoundedCornerShape(16.dp),
                                thickness = GlassThickness.REGULAR,
                                tintColor = palette.primary,
                                tintAlpha = 0.15f,
                                borderWidth = 1.dp
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
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(title, fontWeight = FontWeight.Bold, color = Color.White)
                                Text(sub, fontSize = 12.sp, color = Color(0xFFA0A5BA))
                            }
                            Icon(
                                imageVector = Icons.Default.VolumeUp,
                                contentDescription = null,
                                tint = palette.accent,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
