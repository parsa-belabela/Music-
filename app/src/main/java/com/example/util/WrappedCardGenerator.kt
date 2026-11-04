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
            val isFa = lang == AppLanguage.PERSIAN
            fun fmt(v: Any): String = if (isFa) WrappedEngine.toPersianDigits(v.toString()) else v.toString()

            val width = 1080
            val height = 1920
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            // ۱. پس‌زمینه عمیق سینمایی
            val bgPaint = Paint().apply {
                isAntiAlias = true
                style = Paint.Style.FILL
                shader = LinearGradient(
                    0f, 0f, 0f, height.toFloat(),
                    intArrayOf(
                        Color.rgb(12, 14, 28),
                        Color.rgb(7, 9, 18),
                        Color.rgb(4, 5, 11)
                    ),
                    null,
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

            // ۲. هاله نوری نئونی اسپاتیفای
            val glowPaint = Paint().apply {
                isAntiAlias = true
                shader = RadialGradient(
                    width * 0.5f, height * 0.24f, 650f,
                    intArrayOf(
                        Color.argb(135, (palette.primary.red * 255).toInt(), (palette.primary.green * 255).toInt(), (palette.primary.blue * 255).toInt()),
                        Color.argb(55, 30, 215, 96),
                        Color.TRANSPARENT
                    ),
                    null,
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawCircle(width * 0.5f, height * 0.24f, 650f, glowPaint)

            // ۳. کارت شیشه‌ای مرکزی
            val cardRect = RectF(72f, 150f, width - 72f, height - 170f)
            val cardPaint = Paint().apply {
                isAntiAlias = true
                color = Color.argb(205, 18, 22, 38)
                style = Paint.Style.FILL
            }
            canvas.drawRoundRect(cardRect, 54f, 54f, cardPaint)

            val borderPaint = Paint().apply {
                isAntiAlias = true
                style = Paint.Style.STROKE
                strokeWidth = 3.5f
                shader = LinearGradient(
                    72f, 150f, width - 72f, height - 170f,
                    intArrayOf(
                        Color.argb(220, 30, 215, 96),
                        Color.argb(140, (palette.primary.red * 255).toInt(), (palette.primary.green * 255).toInt(), (palette.primary.blue * 255).toInt()),
                        Color.argb(90, 255, 255, 255)
                    ),
                    null,
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawRoundRect(cardRect, 54f, 54f, borderPaint)

            val titlePaint = Paint().apply {
                isAntiAlias = true
                color = Color.WHITE
                textSize = 52f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }

            val subtitlePaint = Paint().apply {
                isAntiAlias = true
                color = Color.rgb(30, 215, 96)
                textSize = 34f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }

            val textPaint = Paint().apply {
                isAntiAlias = true
                color = Color.WHITE
                textSize = 35f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }

            val labelPaint = Paint().apply {
                isAntiAlias = true
                color = Color.rgb(172, 180, 204)
                textSize = 28f
                textAlign = Paint.Align.CENTER
            }

            val periodName = if (isFa) stats.period.displayNameFa else stats.period.displayNameEn
            canvas.drawText("AURA MUSIC WRAPPED", width / 2f, 255f, titlePaint)
            canvas.drawText(periodName, width / 2f, 315f, subtitlePaint)

            // ۴. مجموع زمان گوش دادن
            val minsText = if (isFa) "${fmt(stats.totalListeningMinutes)} دقیقه" else "${stats.totalListeningMinutes} Minutes"
            val statPaint = Paint().apply {
                isAntiAlias = true
                color = Color.WHITE
                textSize = 78f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText(minsText, width / 2f, 445f, statPaint)
            canvas.drawText(
                if (isFa) "مجموع زمان غرق شدن در موسیقی" else "TOTAL LISTENING TIME",
                width / 2f,
                498f,
                labelPaint
            )

            // ۵. شخصیت شنیداری و هنرمند شماره ۱
            val persona = if (isFa) stats.audioPersonalityFa else stats.audioPersonalityEn
            val topArtistName = stats.topArtists.firstOrNull()?.name ?: "-"
            canvas.drawText(
                if (isFa) "✨ شخصیت شنیداری: $persona" else "✨ Audio Persona: $persona",
                width / 2f,
                585f,
                subtitlePaint.apply { textSize = 32f }
            )
            canvas.drawText(
                if (isFa) "👑 خواننده شماره ۱: $topArtistName" else "👑 #1 Artist: $topArtistName",
                width / 2f,
                645f,
                textPaint
            )

            // ۶. لیست ۵ آهنگ برتر
            val secPaint = Paint().apply {
                isAntiAlias = true
                color = Color.rgb(30, 215, 96)
                textSize = 36f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText(
                if (isFa) "🏆 ۵ آهنگ برتر شما" else "🏆 YOUR TOP 5 SONGS",
                width / 2f,
                755f,
                secPaint
            )

            var yPos = 835f
            stats.topSongs.take(5).forEachIndexed { idx, s ->
                val boxRect = RectF(120f, yPos - 48f, width - 120f, yPos + 46f)
                val rowBgPaint = Paint().apply {
                    isAntiAlias = true
                    color = Color.argb(140, 28, 34, 56)
                }
                canvas.drawRoundRect(boxRect, 22f, 22f, rowBgPaint)

                val rowTitlePaint = Paint().apply {
                    isAntiAlias = true
                    color = Color.WHITE
                    textSize = 32f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    textAlign = Paint.Align.LEFT
                }
                val rowSubPaint = Paint().apply {
                    isAntiAlias = true
                    color = Color.rgb(30, 215, 96)
                    textSize = 28f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    textAlign = Paint.Align.RIGHT
                }
                canvas.drawText("#${idx + 1}  ${s.title.take(24)} - ${s.artist.take(16)}", 155f, yPos + 10f, rowTitlePaint)
                canvas.drawText("${fmt(s.playCount)}x", width - 155f, yPos + 10f, rowSubPaint)
                yPos += 118f
            }

            // ۷. سبک برتر
            val topGenre = stats.topGenres.firstOrNull()?.name ?: "Hip-Hop & Pop"
            canvas.drawText(
                if (isFa) "سبک غالب: $topGenre" else "Top Genre: $topGenre",
                width / 2f,
                height - 310f,
                textPaint.apply { textSize = 32f }
            )

            // ۸. فوتر برندینگ
            val footerPaint = Paint().apply {
                isAntiAlias = true
                color = Color.rgb(148, 158, 186)
                textSize = 27f
                textAlign = Paint.Align.CENTER
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            canvas.drawText("AURA MUSIC • SPOTIFY-GRADE AUDIO EXPERIENCE", width / 2f, height - 220f, footerPaint)

            val cachePath = File(context.cacheDir, "images")
            cachePath.mkdirs()
            val file = File(cachePath, "wrapped_story_${System.currentTimeMillis()}.png")
            FileOutputStream(file).use { stream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            }

            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (_: Exception) {
            null
        }
    }
}
