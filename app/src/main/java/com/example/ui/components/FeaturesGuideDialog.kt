package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.audio.AmbientPalette
import com.example.data.model.AppLanguage
import com.example.data.model.AppSettings
import com.example.ui.theme.GlassThickness
import com.example.ui.theme.liquidGlass
import com.example.util.Localization

data class FeatureItem(
    val icon: ImageVector,
    val titleFa: String,
    val titleEn: String,
    val descFa: String,
    val descEn: String,
    val tagFa: String,
    val tagEn: String,
    val color: Color
)

@Composable
fun FeaturesGuideDialog(
    settings: AppSettings,
    palette: AmbientPalette,
    onDismiss: () -> Unit
) {
    val lang = settings.language

    // Animated glow shimmer
    val infiniteTransition = rememberInfiniteTransition(label = "guide_glow")
    val glowPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_phase"
    )

    val features = remember {
        listOf(
            FeatureItem(
                icon = Icons.Default.GraphicEq,
                titleFa = "موتور پردازش صوتی استودیویی Hi-Fi",
                titleEn = "Hi-Fi Studio DSP Audio Engine",
                descFa = "اکولایزر گرافیکی ۱۰ بانده با دقت استودیویی، تقویت‌کننده بیس ساب‌ووفر، افکت صدای سه‌بعدی فراگیر (Spatial Audio)، کنترل تمپو و زیروبمی صدا بدون افت کیفیت، و قابلیت Crossfade پیوسته.",
                descEn = "10-band precision graphic equalizer, subwoofer Bass Boost, 3D Spatial Virtualizer, real-time Pitch/Tempo shift without quality loss, and gapless crossfade transitions.",
                tagFa = "صدای استودیویی",
                tagEn = "Studio Grade",
                color = Color(0xFF6366F1)
            ),
            FeatureItem(
                icon = Icons.Default.Palette,
                titleFa = "۶ تم لمسی و سه‌بعدی منحصر‌به‌فرد",
                titleEn = "6 Tactile 3D Visual Themes",
                descFa = "تم شیشه‌ای مایع (Liquid Glass) با شکست نور، تم لگو (LEGO Cinematic) با قطعات و استودهای برجسته و بافت واقعی پلاستیک ABS، سایبرپانک نئونی، نوار کاست رترو و دارک OLED.",
                descEn = "Liquid Glass with dynamic light refraction, LEGO Cinematic with 3D ABS molded brick textures and studs, Cyberpunk Neon, Retro Cassette Tape, and OLED Minimalist.",
                tagFa = "طراحی اختصاصی",
                tagEn = "3D Shaders",
                color = Color(0xFFEC4899)
            ),
            FeatureItem(
                icon = Icons.Default.Lyrics,
                titleFa = "متن ترانه همگام و ویرایشگر پیشرفته LRC",
                titleEn = "Synchronized Lyrics & LRC Timecode Editor",
                descFa = "نمایش زنده و کلمه به کلمه متن آهنگ به همراه هایلایت کارائوکه، اسکرول هوشمند، و ویرایشگر درون‌برنامه‌ای برای تنظیم دقیق تایم‌کدهای متن ترانه.",
                descEn = "Live word-by-word synchronized lyrics with karaoke glow, auto-follow scrolling, and a built-in visual LRC timecode editor for perfect alignment.",
                tagFa = "کارائوکه زنده",
                tagEn = "Live Sync",
                color = Color(0xFF10B981)
            ),
            FeatureItem(
                icon = Icons.Default.BlurOn,
                titleFa = "۸ اکولایزر بصری و هاله نوری پویا",
                titleEn = "8 Dynamic Visualizers & Ambient Aura Halo",
                descFa = "طیف‌های بصری فرکانسی متنوع شامل اسپکتروم، نوار موج، ذرات معلق، شفق قطبی نئونی و استخراج خودکار پالت رنگ زنده از کاور آهنگ در حال پخش.",
                descEn = "Multiple reactive frequency visualizers including Bar Spectrum, Waveform, Cosmic Particles, Neon Aurora, and automatic real-time color extraction from album art.",
                tagFa = "پاسخ فرکانسی",
                tagEn = "Real-time DSP",
                color = Color(0xFFF59E0B)
            ),
            FeatureItem(
                icon = Icons.Default.AutoGraph,
                titleFa = "خلاصه شنیداری هوشمند (Music Wrapped)",
                titleEn = "Music Wrapped & Listening Intelligence",
                descFa = "تحلیل واقعی دقایق گوش‌دادن، برترین آهنگ‌ها، هنرمندان و سبک‌های محبوب، رکورد روزهای متوالی و امکان ایجاد کارت‌های تصویری برای اشتراک‌گذاری.",
                descEn = "Authentic statistical tracking of your listening hours, top tracks, favorite artists, listening streaks, audio personality, and shareable visual summary cards.",
                tagFa = "آمار تحلیلی",
                tagEn = "Analytics",
                color = Color(0xFF8B5CF6)
            ),
            FeatureItem(
                icon = Icons.Default.EditNote,
                titleFa = "ویرایشگر آفلاین متادیتا و مدیریت پلی‌لیست‌ها",
                titleEn = "Offline Tag Editor & Smart Playlists",
                descFa = "ویرایش کامل تگ‌های عنوان، خواننده، آلبوم، سال و سبک به صورت کاملاً آفلاین، ایجاد پلی‌لیست‌های دلخواه و هوشمند، تایمر خواب با محو ملایم صدا و پشتیبان‌گیری کامل JSON.",
                descEn = "Complete offline ID3 tag editor for titles, artists, albums, genres, custom and smart playlist management, volume fade-out Sleep Timer, and full JSON backup/restore.",
                tagFa = "مدیریت آفلاین",
                tagEn = "Offline First",
                color = Color(0xFF06B6D4)
            )
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.88f)
                .drawBehind {
                    // Volumetric aura back-glow
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                palette.primary.copy(alpha = 0.25f + 0.15f * glowPhase),
                                palette.accent.copy(alpha = 0.12f),
                                Color.Transparent
                            ),
                            center = Offset(size.width / 2f, size.height * 0.25f),
                            radius = size.width * 0.75f
                        )
                    )
                }
                .liquidGlass(
                    shape = RoundedCornerShape(32.dp),
                    thickness = GlassThickness.THICK,
                    tintColor = palette.primary,
                    tintAlpha = 0.32f,
                    borderWidth = 1.4.dp,
                    appTheme = settings.theme
                )
                .padding(22.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header with Glowing Badge
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(palette.primary, palette.accent)
                            )
                        )
                        .border(1.5.dp, Color.White.copy(alpha = 0.4f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = Localization.getString("features_guide_title", lang),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = Localization.getString("features_guide_subtitle", lang),
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFFB0B0CC),
                        lineHeight = 18.sp
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Scrollable Feature Cards
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(features) { item ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .liquidGlass(
                                    shape = RoundedCornerShape(20.dp),
                                    thickness = GlassThickness.REGULAR,
                                    tintColor = item.color,
                                    tintAlpha = 0.18f,
                                    borderWidth = 1.dp,
                                    appTheme = settings.theme
                                )
                                .padding(16.dp)
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f, fill = false)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(38.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(item.color.copy(alpha = 0.22f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = item.icon,
                                                contentDescription = null,
                                                tint = item.color,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        Text(
                                            text = if (lang == AppLanguage.PERSIAN) item.titleFa else item.titleEn,
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        )
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = item.color.copy(alpha = 0.25f)
                                    ) {
                                        Text(
                                            text = if (lang == AppLanguage.PERSIAN) item.tagFa else item.tagEn,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = item.color,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 10.sp
                                            ),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Text(
                                    text = if (lang == AppLanguage.PERSIAN) item.descFa else item.descEn,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = Color(0xFFD4D4E8),
                                        lineHeight = 22.sp
                                    )
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Close Button
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = palette.primary)
                ) {
                    Text(
                        text = Localization.getString("close", lang),
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                }
            }
        }
    }
}
