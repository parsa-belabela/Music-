package com.example.util

import android.content.Context
import android.graphics.*
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.audio.AmbientPalette
import com.example.data.model.AppLanguage
import com.example.data.model.WrappedStats
import java.io.File
import java.io.FileOutputStream

object WrappedCardGenerator {

    fun generateWrappedStoryBitmap(
        context: Context,
        stats: WrappedStats,
        palette: AmbientPalette,
        lang: AppLanguage
    ): Uri? {
        return try {
            val width = 1080
            val height = 1920
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            // Background Deep Atmosphere
            val bgPaint = Paint().apply {
                isAntiAlias = true
                style = Paint.Style.FILL
                shader = LinearGradient(
                    0f, 0f, 0f, height.toFloat(),
                    intArrayOf(
                        android.graphics.Color.rgb(15, 12, 30),
                        android.graphics.Color.rgb(8, 7, 18),
                        android.graphics.Color.rgb(4, 3, 10)
                    ),
                    null,
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

            // Radial atmospheric glow
            val glowPaint = Paint().apply {
                isAntiAlias = true
                shader = RadialGradient(
                    width * 0.5f, height * 0.25f, 600f,
                    intArrayOf(
                        android.graphics.Color.argb(120, (palette.primary.red * 255).toInt(), (palette.primary.green * 255).toInt(), (palette.primary.blue * 255).toInt()),
                        android.graphics.Color.argb(40, (palette.accent.red * 255).toInt(), (palette.accent.green * 255).toInt(), (palette.accent.blue * 255).toInt()),
                        android.graphics.Color.TRANSPARENT
                    ),
                    null,
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawCircle(width * 0.5f, height * 0.25f, 600f, glowPaint)

            // Glass Container Card
            val cardRect = RectF(70f, 160f, width - 70f, height - 180f)
            val cardPaint = Paint().apply {
                isAntiAlias = true
                color = android.graphics.Color.argb(180, 25, 23, 45)
                style = Paint.Style.FILL
            }
            canvas.drawRoundRect(cardRect, 48f, 48f, cardPaint)

            // Card Border
            val borderPaint = Paint().apply {
                isAntiAlias = true
                style = Paint.Style.STROKE
                strokeWidth = 3f
                shader = LinearGradient(
                    70f, 160f, width - 70f, height - 180f,
                    intArrayOf(
                        android.graphics.Color.argb(200, 255, 255, 255),
                        android.graphics.Color.argb(60, 255, 255, 255),
                        android.graphics.Color.argb(120, (palette.accent.red * 255).toInt(), (palette.accent.green * 255).toInt(), (palette.accent.blue * 255).toInt())
                    ),
                    null,
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawRoundRect(cardRect, 48f, 48f, borderPaint)

            // Paints for Typography
            val titlePaint = Paint().apply {
                isAntiAlias = true
                color = android.graphics.Color.WHITE
                textSize = 54f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }

            val subtitlePaint = Paint().apply {
                isAntiAlias = true
                color = android.graphics.Color.argb(220, (palette.accent.red * 255).toInt(), (palette.accent.green * 255).toInt(), (palette.accent.blue * 255).toInt())
                textSize = 34f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }

            val textPaint = Paint().apply {
                isAntiAlias = true
                color = android.graphics.Color.WHITE
                textSize = 36f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            }

            val labelPaint = Paint().apply {
                isAntiAlias = true
                color = android.graphics.Color.rgb(170, 170, 195)
                textSize = 28f
            }

            val periodName = if (lang == AppLanguage.PERSIAN) stats.period.displayNameFa else stats.period.displayNameEn
            canvas.drawText("AURA MUSIC WRAPPED", width / 2f, 260f, titlePaint)
            canvas.drawText(periodName.uppercase(), width / 2f, 320f, subtitlePaint)

            // Stat 1: Total Listening Time
            val hours = stats.totalListeningHours
            val timeText = if (hours >= 1.0) "${"%.1f".format(hours)} Hours" else "${stats.totalListeningMinutes} Mins"
            val statPaint = Paint().apply {
                isAntiAlias = true
                color = android.graphics.Color.WHITE
                textSize = 72f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText(timeText, width / 2f, 450f, statPaint)
            canvas.drawText("TOTAL LISTENING TIME", width / 2f, 500f, labelPaint.apply { textAlign = Paint.Align.CENTER })

            // Stat 2: Streak & Persona
            val persona = if (lang == AppLanguage.PERSIAN) stats.audioPersonalityFa else stats.audioPersonalityEn
            canvas.drawText("✨ Audio Personality: $persona", width / 2f, 590f, subtitlePaint.apply { textSize = 32f })
            canvas.drawText("🔥 ${stats.listeningStreakDays}-Day Listening Streak • ${stats.totalTracksPlayed} Tracks", width / 2f, 645f, labelPaint)

            // Section: Top Tracks
            val secPaint = Paint().apply {
                isAntiAlias = true
                color = android.graphics.Color.argb(240, 255, 255, 255)
                textSize = 38f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.LEFT
            }
            canvas.drawText("TOP TRACKS", 130f, 760f, secPaint)

            var yPos = 830f
            stats.topSongs.take(5).forEachIndexed { idx, s ->
                val numPaint = Paint().apply {
                    isAntiAlias = true
                    color = android.graphics.Color.argb(220, (palette.accent.red * 255).toInt(), (palette.accent.green * 255).toInt(), (palette.accent.blue * 255).toInt())
                    textSize = 34f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                }
                canvas.drawText("#${idx + 1}", 130f, yPos, numPaint)
                canvas.drawText(s.title.take(28), 200f, yPos, textPaint.apply { typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) })
                canvas.drawText("${s.artist} • ${s.playCount} plays", 200f, yPos + 40f, labelPaint.apply { textAlign = Paint.Align.LEFT })
                yPos += 105f
            }

            // Section: Top Artists
            if (stats.topArtists.isNotEmpty()) {
                canvas.drawText("TOP ARTISTS", 130f, yPos + 40f, secPaint)
                yPos += 105f
                val artistStr = stats.topArtists.take(3).joinToString("  •  ") { it.name }
                canvas.drawText(artistStr.take(45), 130f, yPos, textPaint.apply { textSize = 32f })
            }

            // Footer branding
            val footerPaint = Paint().apply {
                isAntiAlias = true
                color = android.graphics.Color.rgb(150, 150, 180)
                textSize = 28f
                textAlign = Paint.Align.CENTER
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            canvas.drawText("🌌 Aura Music Player • Liquid Glass Audio Experience", width / 2f, height - 220f, footerPaint)

            // Save to Cache file
            val cachePath = File(context.cacheDir, "images")
            cachePath.mkdirs()
            val file = File(cachePath, "wrapped_story_${System.currentTimeMillis()}.png")
            val stream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            stream.close()

            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (_: Exception) {
            null
        }
    }
}
