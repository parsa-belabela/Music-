package com.example.util

import android.content.Context
import android.graphics.*
import android.net.Uri
import android.text.TextPaint
import android.text.TextUtils
import androidx.core.content.FileProvider
import com.example.OnlineMusicRepository
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

            val spotifyGreen = Color.rgb(30, 215, 96)
            val primaryInt = Color.rgb(
                (palette.primary.red * 255).toInt().coerceIn(0, 255),
                (palette.primary.green * 255).toInt().coerceIn(0, 255),
                (palette.primary.blue * 255).toInt().coerceIn(0, 255)
            )

            // 1. Matte Spotify Wrapped Editorial Canvas
            val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(
                    0f, 0f, 0f, height.toFloat(),
                    intArrayOf(
                        Color.rgb(20, 20, 22),
                        Color.rgb(14, 14, 16),
                        Color.rgb(10, 10, 12)
                    ),
                    null,
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

            // Subtle Top Editorial Accent Wash
            val topWashPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = RadialGradient(
                    width * 0.5f, 180f, 720f,
                    intArrayOf(
                        Color.argb(95, Color.red(primaryInt), Color.green(primaryInt), Color.blue(primaryInt)),
                        Color.argb(40, 30, 215, 96),
                        Color.TRANSPARENT
                    ),
                    null,
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawCircle(width * 0.5f, 180f, 720f, topWashPaint)

            // 2. Main Editorial Card Container
            val cardRect = RectF(68f, 120f, width - 68f, height - 120f)
            val cardBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(24, 24, 26)
                style = Paint.Style.FILL
            }
            canvas.drawRoundRect(cardRect, 44f, 44f, cardBgPaint)

            val cardStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(38, 255, 255, 255)
                style = Paint.Style.STROKE
                strokeWidth = 2.5f
            }
            canvas.drawRoundRect(cardRect, 44f, 44f, cardStrokePaint)

            val padX = 124f

            // 3. Top Header Badge & Period Title
            val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = spotifyGreen
                textSize = 26f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                letterSpacing = 0.12f
            }
            val periodName = if (isFa) stats.period.displayNameFa else stats.period.displayNameEn.uppercase()
            canvas.drawText("AURA WRAPPED • $periodName", padX, 215f, badgePaint)

            // 4. Featured #1 Artwork + Hero Stat Row
            val topArtUri = stats.topSongs.firstOrNull()?.artworkUri
                ?: stats.topArtists.firstOrNull()?.artworkUri
                ?: stats.topSongs.firstOrNull()?.trackId?.let {
                    OnlineMusicRepository.getCoverUrlForTrack(it)
                }

            var topBmp: Bitmap? = null
            if (!topArtUri.isNullOrBlank()) {
                topBmp = OnlineMusicRepository.getCachedCoverBitmap(topArtUri)
                    ?: OnlineMusicRepository.loadCoverBitmapBlocking(topArtUri)
            }

            val heroArtRect = RectF(padX, 265f, padX + 280f, 265f + 280f)
            if (topBmp != null) {
                val clipPath = Path().apply {
                    addRoundRect(heroArtRect, 28f, 28f, Path.Direction.CW)
                }
                canvas.save()
                canvas.clipPath(clipPath)
                canvas.drawBitmap(topBmp, null, heroArtRect, Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG))
                canvas.restore()
            } else {
                val fallbackArtPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.rgb(38, 38, 42)
                }
                canvas.drawRoundRect(heroArtRect, 28f, 28f, fallbackArtPaint)
                val noteP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = spotifyGreen
                    textSize = 96f
                    textAlign = Paint.Align.CENTER
                }
                canvas.drawText("♪", heroArtRect.centerX(), heroArtRect.centerY() + 32f, noteP)
            }

            // Minutes Listened & Archetype next to Artwork
            val statsX = heroArtRect.right + 44f
            val minsLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(179, 179, 179)
                textSize = 28f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            }
            val minsValuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = 76f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            canvas.drawText(
                if (isFa) "زمان شنیداری" else "MINUTES LISTENED",
                statsX,
                325f,
                minsLabelPaint
            )
            canvas.drawText(
                fmt(stats.totalListeningMinutes),
                statsX,
                415f,
                minsValuePaint
            )

            val topGenre = stats.topGenres.firstOrNull()?.name ?: "Pop & Hip-Hop"
            val genreTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = spotifyGreen
                textSize = 32f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            canvas.drawText(
                if (isFa) "سبک برتر: $topGenre" else "Top Genre: $topGenre",
                statsX,
                485f,
                genreTitlePaint
            )

            val persona = if (isFa) stats.audioPersonalityFa else stats.audioPersonalityEn
            val personaPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = 28f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val cleanPersona = TextUtils.ellipsize(
                persona,
                personaPaint,
                (width - padX - statsX),
                TextUtils.TruncateAt.END
            ).toString()
            canvas.drawText(cleanPersona, statsX, 532f, personaPaint)

            // Divider
            val divPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(30, 255, 255, 255)
                strokeWidth = 2f
            }
            canvas.drawLine(padX, 600f, width - padX, 600f, divPaint)

            // 5. Two-Column Spotify Wrapped Lists: TOP ARTISTS & TOP SONGS
            val colHeaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(179, 179, 179)
                textSize = 26f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                letterSpacing = 0.08f
            }

            // Section A: Top Songs (Full Width Ranked List)
            canvas.drawText(
                if (isFa) "برترین آهنگ‌ها" else "TOP SONGS",
                padX,
                665f,
                colHeaderPaint
            )

            val itemRankPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = spotifyGreen
                textSize = 36f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val itemTitlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = 36f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val itemSubPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(179, 179, 179)
                textSize = 27f
            }
            val itemCountPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = spotifyGreen
                textSize = 30f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.RIGHT
            }

            var songY = 735f
            stats.topSongs.take(5).forEachIndexed { idx, song ->
                canvas.drawText(fmt(idx + 1), padX, songY, itemRankPaint)

                val maxSongW = width - padX * 2 - 160f
                val sTitle = TextUtils.ellipsize(song.title, itemTitlePaint, maxSongW, TextUtils.TruncateAt.END).toString()
                val sArtist = TextUtils.ellipsize(song.artist, itemSubPaint, maxSongW, TextUtils.TruncateAt.END).toString()

                canvas.drawText(sTitle, padX + 56f, songY - 8f, itemTitlePaint)
                canvas.drawText(sArtist, padX + 56f, songY + 28f, itemSubPaint)
                canvas.drawText("${fmt(song.playCount)}x", width - padX, songY, itemCountPaint)

                songY += 102f
            }

            canvas.drawLine(padX, songY + 10f, width - padX, songY + 10f, divPaint)

            // Section B: Top Artists
            val artistsHeaderY = songY + 72f
            canvas.drawText(
                if (isFa) "برترین خواننده‌ها" else "TOP ARTISTS",
                padX,
                artistsHeaderY,
                colHeaderPaint
            )

            var artistY = artistsHeaderY + 64f
            stats.topArtists.take(4).forEachIndexed { idx, artist ->
                canvas.drawText(fmt(idx + 1), padX, artistY, itemRankPaint)
                val maxArtW = width - padX * 2 - 160f
                val aName = TextUtils.ellipsize(artist.name, itemTitlePaint, maxArtW, TextUtils.TruncateAt.END).toString()
                canvas.drawText(aName, padX + 56f, artistY, itemTitlePaint)
                canvas.drawText("${fmt(artist.count)}x", width - padX, artistY, itemCountPaint)
                artistY += 74f
            }

            // 6. Clean Brand Footer
            val footerBrandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = 30f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val footerUrlPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = spotifyGreen
                textSize = 26f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.RIGHT
            }

            canvas.drawText("Aura Music", padX, height - 175f, footerBrandPaint)
            canvas.drawText("WRAPPED", width - padX, height - 175f, footerUrlPaint)

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
