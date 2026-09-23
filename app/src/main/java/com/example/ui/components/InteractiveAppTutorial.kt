package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppLanguage
import com.example.ui.theme.GlassThickness
import com.example.ui.theme.liquidGlass

data class TutorialStep(
    val titleFa: String,
    val titleEn: String,
    val descFa: String,
    val descEn: String,
    val icon: ImageVector,
    val accentColor: Color
)

@Composable
fun InteractiveAppTutorial(
    lang: AppLanguage = AppLanguage.PERSIAN,
    onFinishTutorial: () -> Unit,
    modifier: Modifier = Modifier
) {
    val steps = remember {
        listOf(
            TutorialStep(
                titleFa = "کوئیک پلی و نورپردازی زنده",
                titleEn = "QuickPlay & Ambient Light",
                descFa = "در بالای صفحه اصلی، آهنگ فعال با نورپردازی متحرک بر اساس رنگ کاور و جلوه شیشه‌ای مایع پخش می‌شود.",
                descEn = "Instantly resume tracks with dynamic ambient glow matching your album artwork.",
                icon = Icons.Default.PlayCircleFilled,
                accentColor = Color(0xFF00E5FF)
            ),
            TutorialStep(
                titleFa = "مینی پلیر سریع و باریک",
                titleEn = "Sleek Floating MiniPlayer",
                descFa = "کنترل موسیقی، جابجایی بین آهنگ‌ها و نوار پیشرفت لمسی همیشه در دسترس شماست.",
                descEn = "Seamless playback controls and scrubbing right at your fingertips.",
                icon = Icons.Default.GraphicEq,
                accentColor = Color(0xFF7C4DFF)
            ),
            TutorialStep(
                titleFa = "پلی‌لیست و دسته‌بندی هوشمند",
                titleEn = "Playlists & Organization",
                descFa = "لیست‌های پخش سفارشی بسازید، آهنگ‌ها را بر اساس سبک یا هنرمند فیلتر کنید و اطلاعات آهنگ را ویرایش کنید.",
                descEn = "Create custom playlists and organize your local music effortlessly.",
                icon = Icons.Default.QueueMusic,
                accentColor = Color(0xFF00E676)
            ),
            TutorialStep(
                titleFa = "کالیبراسیون شنوایی و اکولایزر",
                titleEn = "Hi-Fi Studio Equalizer",
                descFa = "با تست فرکانسی اختصاصی، صدای موزیک پلیر را بر اساس دقیق‌ترین حساسیت گوش و هدفون خود کالیبره کنید.",
                descEn = "Tune frequencies to match your personal hearing and headphone profile.",
                icon = Icons.Default.Hearing,
                accentColor = Color(0xFFFF9100)
            ),
            TutorialStep(
                titleFa = "پوسته‌های کلکسیونی VIP",
                titleEn = "VIP Collector Skins",
                descFa = "از صفحه‌های اختصاصی گرامافون لوکس، باربی دریم (Barbie)، شوالیه تاریکی (Batman) و لست آف آز (The Last of Us) لذت ببرید.",
                descEn = "Unlock Luxury Vinyl, Barbie Dream, Batman Knight, and The Last of Us animated skins.",
                icon = Icons.Default.WorkspacePremium,
                accentColor = Color(0xFFFF1493)
            )
        )
    }

    var currentStepIdx by remember { mutableIntStateOf(0) }
    val currentStep = steps[currentStepIdx]

    val infiniteTransition = rememberInfiniteTransition(label = "tutPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xE006070D))
            .clickable(enabled = false) {}
            .testTag("interactive_tutorial_overlay"),
        contentAlignment = Alignment.Center
    ) {
        // Ambient background glow
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(
                        currentStep.accentColor.copy(alpha = 0.35f),
                        Color.Transparent
                    ),
                    center = Offset(size.width / 2f, size.height * 0.45f),
                    radius = size.minDimension * 0.65f
                ),
                radius = size.minDimension * 0.65f,
                center = Offset(size.width / 2f, size.height * 0.45f)
            )
        }

        // Floating Glass Card
        Box(
            modifier = Modifier
                .fillMaxWidth(0.90f)
                .liquidGlass(
                    shape = RoundedCornerShape(32.dp),
                    thickness = GlassThickness.REGULAR,
                    tintColor = currentStep.accentColor,
                    tintAlpha = 0.18f,
                    borderWidth = 1.5.dp
                )
                .padding(26.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Step Indicator Badge & Skip
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .liquidGlass(
                                shape = RoundedCornerShape(12.dp),
                                thickness = GlassThickness.THIN,
                                tintColor = currentStep.accentColor,
                                tintAlpha = 0.25f
                            )
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (lang == AppLanguage.PERSIAN) "مرحله ${currentStepIdx + 1} از ${steps.size}" else "Step ${currentStepIdx + 1} of ${steps.size}",
                            color = currentStep.accentColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }

                    TextButton(onClick = onFinishTutorial) {
                        Text(
                            text = if (lang == AppLanguage.PERSIAN) "رد کردن" else "Skip",
                            color = Color.White.copy(alpha = 0.65f),
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Pulsing Center Icon
                Box(
                    modifier = Modifier
                        .size(86.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    currentStep.accentColor.copy(alpha = 0.4f),
                                    currentStep.accentColor.copy(alpha = 0.1f)
                                )
                            )
                        )
                        .border(2.dp, currentStep.accentColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = currentStep.icon,
                        contentDescription = null,
                        tint = currentStep.accentColor,
                        modifier = Modifier.size(46.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Title
                Text(
                    text = if (lang == AppLanguage.PERSIAN) currentStep.titleFa else currentStep.titleEn,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Description
                Text(
                    text = if (lang == AppLanguage.PERSIAN) currentStep.descFa else currentStep.descEn,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color(0xFFD0D5E5),
                        lineHeight = 22.sp
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Step Dots
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    steps.indices.forEach { idx ->
                        val isCurrent = idx == currentStepIdx
                        Box(
                            modifier = Modifier
                                .height(6.dp)
                                .width(if (isCurrent) 22.dp else 6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(if (isCurrent) currentStep.accentColor else Color.White.copy(alpha = 0.25f))
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Next / Previous Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (currentStepIdx > 0) {
                        OutlinedButton(
                            onClick = { currentStepIdx-- },
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (lang == AppLanguage.PERSIAN) "قبلی" else "Back")
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                    }

                    Button(
                        onClick = {
                            if (currentStepIdx < steps.size - 1) {
                                currentStepIdx++
                            } else {
                                onFinishTutorial()
                            }
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = currentStep.accentColor),
                        modifier = Modifier.weight(if (currentStepIdx > 0) 1.2f else 1f)
                    ) {
                        Text(
                            text = if (currentStepIdx == steps.size - 1) {
                                if (lang == AppLanguage.PERSIAN) "شروع تجربه" else "Get Started"
                            } else {
                                if (lang == AppLanguage.PERSIAN) "بعدی" else "Next"
                            },
                            color = Color.Black,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
