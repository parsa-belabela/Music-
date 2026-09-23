package com.example.ui.screens

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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.audio.AmbientPalette
import com.example.audio.AudioAnalysisData
import com.example.data.model.PlaybackState
import com.example.data.model.Track
import com.example.ui.components.LiquidGlassProgressBar
import com.example.ui.theme.GlassThickness
import com.example.ui.theme.liquidGlass
import kotlin.math.cos
import kotlin.math.sin

enum class TurntablePlinth(val id: String, val titleEn: String, val titleFa: String, val baseColor: Color, val accentColor: Color) {
    WALNUT_WOOD("walnut", "Walnut Masterwood", "چوب گردوی سلطنتی", Color(0xFF2C1810), Color(0xFFC68B59)),
    OBSIDIAN_CARBON("obsidian", "Obsidian Carbon", "ابسیدین مشکی براق", Color(0xFF0D0E14), Color(0xFF00E5FF)),
    VINTAGE_GOLD("gold", "Vintage 24K Brass", "برنجی و طلای کلاسیک", Color(0xFF1E190E), Color(0xFFFFD700))
}

@Composable
fun VinylNowPlayingStyle(
    playbackState: PlaybackState,
    palette: AmbientPalette,
    analysisData: AudioAnalysisData,
    waveformEnvelope: FloatArray? = null,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Long) -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier
) {
    val track = playbackState.currentTrack ?: return
    val isPlaying = playbackState.isPlaying
    val haptic = LocalHapticFeedback.current

    var selectedPlinth by remember { mutableStateOf(TurntablePlinth.WALNUT_WOOD) }
    var vinylWarmthEnabled by remember { mutableStateOf(true) }

    // Infinite rotation for spinning vinyl
    val infiniteTransition = rememberInfiniteTransition(label = "vinylSpin")
    val spinAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "vinylAngle"
    )

    val currentRotation = if (isPlaying) spinAngle else 0f

    // Animated Tonearm / Needle Angle
    val trackProgress = if (playbackState.durationMs > 0) {
        (playbackState.currentPositionMs.toFloat() / playbackState.durationMs.toFloat()).coerceIn(0f, 1f)
    } else 0f

    val targetArmAngle = if (isPlaying) {
        18f + (trackProgress * 14f)
    } else {
        -26f
    }

    val tonearmAngle by animateFloatAsState(
        targetValue = targetArmAngle,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "tonearmAngle"
    )

    // Trigger haptic when needle hits the vinyl record
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            kotlinx.coroutines.delay(260)
            try {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            } catch (_: Exception) {}
        }
    }

    val bassPulse = if (isPlaying) (analysisData.kickPulse * 0.05f) else 0f
    val vinylScale = 1.0f + bassPulse

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Master Plinth Turntable Body
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(32.dp))
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            selectedPlinth.baseColor,
                            Color(0xFF06070B)
                        )
                    )
                )
                .border(
                    width = 1.5.dp,
                    brush = Brush.linearGradient(
                        listOf(
                            selectedPlinth.accentColor.copy(alpha = 0.65f),
                            Color.White.copy(alpha = 0.12f),
                            selectedPlinth.accentColor.copy(alpha = 0.35f)
                        )
                    ),
                    shape = RoundedCornerShape(32.dp)
                )
                .padding(16.dp)
        ) {
            // Analog Platter & Strobe Dots
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width * 0.44f, size.height * 0.50f)
                val platterRadius = size.minDimension * 0.44f

                // Platter Metallic Edge
                drawCircle(
                    brush = Brush.sweepGradient(
                        listOf(
                            Color(0xFF3A3A42),
                            Color(0xFF1E1F24),
                            Color(0xFF4A4B54),
                            Color(0xFF1E1F24),
                            Color(0xFF3A3A42)
                        ),
                        center = center
                    ),
                    radius = platterRadius + 8.dp.toPx(),
                    center = center
                )

                // Strobe dots ring
                val numDots = 36
                for (i in 0 until numDots) {
                    val angle = (i * (360f / numDots) + (if (isPlaying) spinAngle * 0.5f else 0f)) * (Math.PI / 180f)
                    val dotRadius = platterRadius + 4.dp.toPx()
                    val dx = center.x + dotRadius * cos(angle).toFloat()
                    val dy = center.y + dotRadius * sin(angle).toFloat()
                    drawCircle(
                        color = selectedPlinth.accentColor.copy(alpha = 0.75f),
                        radius = 2.dp.toPx(),
                        center = Offset(dx, dy)
                    )
                }
            }

            // Spinning Vinyl Disc
            Box(
                modifier = Modifier
                    .size(260.dp)
                    .align(Alignment.CenterStart)
                    .offset(x = 10.dp)
                    .scale(vinylScale)
            ) {
                // Vinyl Record Background with Micro-Grooves & Dynamic Light Reflections
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .rotate(currentRotation)
                ) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val recordRadius = size.minDimension / 2f

                    // Deep Black Vinyl Resin
                    drawCircle(
                        color = Color(0xFF090A0D),
                        radius = recordRadius,
                        center = center
                    )

                    // 12 Micro Groove Rings with Shimmering Specular Sheen
                    for (i in 1..12) {
                        val r = recordRadius * (0.36f + (i * 0.05f))
                        drawCircle(
                            color = Color.White.copy(alpha = if (i % 2 == 0) 0.08f else 0.04f),
                            radius = r,
                            center = center,
                            style = Stroke(width = 1.2.dp.toPx())
                        )
                    }

                    // Dual Anamorphic Light Reflection Glare (Opposing Wedges)
                    drawCircle(
                        brush = Brush.sweepGradient(
                            listOf(
                                Color.Transparent,
                                Color.White.copy(alpha = 0.22f),
                                Color.Transparent,
                                Color.Transparent,
                                Color.White.copy(alpha = 0.22f),
                                Color.Transparent
                            ),
                            center = center
                        ),
                        radius = recordRadius,
                        center = center
                    )

                    // Outer Rim Vinyl Bevel
                    drawCircle(
                        brush = Brush.radialGradient(
                            listOf(Color.Transparent, Color.White.copy(alpha = 0.15f)),
                            center = center,
                            radius = recordRadius
                        ),
                        radius = recordRadius,
                        center = center,
                        style = Stroke(width = 2.5.dp.toPx())
                    )
                }

                // Center Album Art Sticker / Label
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .align(Alignment.Center)
                        .rotate(currentRotation)
                        .clip(CircleShape)
                        .background(Color(0xFF161822))
                        .border(3.dp, selectedPlinth.accentColor.copy(alpha = 0.8f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (track.artworkUri != null) {
                        AsyncImage(
                            model = track.artworkUri,
                            contentDescription = "Vinyl Label",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = selectedPlinth.accentColor,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    // Center spindle hole
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF0A0B10))
                            .border(1.5.dp, Color(0xFFC0C5D0), CircleShape)
                    )
                }
            }

            // Pivot Base & Animated Tonearm
            Box(
                modifier = Modifier
                    .size(width = 110.dp, height = 230.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = (-4).dp, y = 20.dp)
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                ) {
                    val pivotCenter = Offset(size.width * 0.72f, 32.dp.toPx())

                    // Pivot Turret Base (Solid Metal)
                    drawCircle(
                        brush = Brush.radialGradient(
                            listOf(Color(0xFF808595), Color(0xFF2B2D38), Color(0xFF14151B)),
                            center = pivotCenter,
                            radius = 24.dp.toPx()
                        ),
                        radius = 24.dp.toPx(),
                        center = pivotCenter
                    )
                    drawCircle(
                        color = selectedPlinth.accentColor.copy(alpha = 0.9f),
                        radius = 8.dp.toPx(),
                        center = pivotCenter
                    )

                    // Rotating Tonearm Rod with Cartridge & Stylus
                    rotate(degrees = tonearmAngle, pivot = pivotCenter) {
                        // Curved S-Shaped ToneArm Metal Rod
                        val armLength = 175.dp.toPx()
                        val endX = pivotCenter.x - 48.dp.toPx()
                        val endY = pivotCenter.y + armLength

                        drawLine(
                            brush = Brush.linearGradient(
                                listOf(Color(0xFFE0E5F0), Color(0xFF9095A5), Color(0xFF454854))
                            ),
                            start = pivotCenter,
                            end = Offset(endX, endY),
                            strokeWidth = 5.dp.toPx(),
                            cap = StrokeCap.Round
                        )

                        // Counterweight (Behind pivot)
                        val cwCenter = Offset(pivotCenter.x + 8.dp.toPx(), pivotCenter.y - 18.dp.toPx())
                        drawCircle(
                            color = Color(0xFF1E2028),
                            radius = 12.dp.toPx(),
                            center = cwCenter
                        )
                        drawCircle(
                            color = selectedPlinth.accentColor,
                            radius = 4.dp.toPx(),
                            center = cwCenter
                        )

                        // Headshell & Phono Cartridge with Gold Needle
                        drawRoundRect(
                            color = Color(0xFF10121A),
                            topLeft = Offset(endX - 8.dp.toPx(), endY - 2.dp.toPx()),
                            size = Size(16.dp.toPx(), 26.dp.toPx()),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
                        )
                        // Glowing Needle Tip
                        drawCircle(
                            color = if (isPlaying) Color(0xFFFF4500) else Color(0xFFFFD700),
                            radius = 2.5.dp.toPx(),
                            center = Offset(endX, endY + 24.dp.toPx())
                        )
                    }
                }
            }

            // Top Status Badges: RPM & Plinth Switcher
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 33 ⅓ RPM Speed Pill
                Box(
                    modifier = Modifier
                        .liquidGlass(
                            shape = RoundedCornerShape(10.dp),
                            thickness = GlassThickness.THIN,
                            tintColor = selectedPlinth.accentColor,
                            tintAlpha = 0.2f
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (isPlaying) "33 ⅓ RPM • ANALOG ON" else "STANDBY • NEEDLE UP",
                        color = selectedPlinth.accentColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                // Plinth Material Cycle Button
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TurntablePlinth.values().forEach { plinth ->
                        val isSel = selectedPlinth == plinth
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(plinth.baseColor)
                                .border(
                                    width = if (isSel) 2.dp else 1.dp,
                                    color = if (isSel) plinth.accentColor else Color.White.copy(alpha = 0.3f),
                                    shape = CircleShape
                                )
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    selectedPlinth = plinth
                                }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Track Title & Artist
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
        ) {
            Text(
                text = track.title,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = "${track.artist} • ${track.album}",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color(0xFFA5ABC0),
                    fontSize = 13.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Progress Bar
        LiquidGlassProgressBar(
            currentPositionProvider = { playbackState.currentPositionMs },
            durationMs = playbackState.durationMs,
            palette = palette,
            analysisDataProvider = { analysisData },
            onSeekTo = onSeek,
            waveformEnvelope = waveformEnvelope,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Tactile Turntable Controls (Previous, Play/Pause, Next, Favorite)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Favorite Button
            IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier
                    .size(44.dp)
                    .liquidGlass(
                        shape = CircleShape,
                        thickness = GlassThickness.THIN,
                        tintColor = palette.primary,
                        tintAlpha = 0.15f
                    )
            ) {
                Icon(
                    imageVector = if (track.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = if (track.isFavorite) Color(0xFFFF3366) else Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }

            // Previous Button
            IconButton(
                onClick = onPrevious,
                modifier = Modifier
                    .size(50.dp)
                    .liquidGlass(
                        shape = CircleShape,
                        thickness = GlassThickness.REGULAR,
                        tintColor = selectedPlinth.accentColor,
                        tintAlpha = 0.18f
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.SkipPrevious,
                    contentDescription = "Previous",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }

            // Master Play / Pause Knob
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                selectedPlinth.accentColor,
                                selectedPlinth.accentColor.copy(alpha = 0.65f),
                                Color(0xFF14151E)
                            )
                        )
                    )
                    .border(2.dp, Color.White.copy(alpha = 0.6f), CircleShape)
                    .clickable { onPlayPause() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = "Play/Pause",
                    tint = Color.Black,
                    modifier = Modifier.size(38.dp)
                )
            }

            // Next Button
            IconButton(
                onClick = onNext,
                modifier = Modifier
                    .size(50.dp)
                    .liquidGlass(
                        shape = CircleShape,
                        thickness = GlassThickness.REGULAR,
                        tintColor = selectedPlinth.accentColor,
                        tintAlpha = 0.18f
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.SkipNext,
                    contentDescription = "Next",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }

            // Vinyl Warmth Crackle Toggle
            IconButton(
                onClick = {
                    vinylWarmthEnabled = !vinylWarmthEnabled
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                },
                modifier = Modifier
                    .size(44.dp)
                    .liquidGlass(
                        shape = CircleShape,
                        thickness = GlassThickness.THIN,
                        tintColor = if (vinylWarmthEnabled) selectedPlinth.accentColor else palette.secondary,
                        tintAlpha = if (vinylWarmthEnabled) 0.35f else 0.15f
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.GraphicEq,
                    contentDescription = "Warmth",
                    tint = if (vinylWarmthEnabled) selectedPlinth.accentColor else Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}
