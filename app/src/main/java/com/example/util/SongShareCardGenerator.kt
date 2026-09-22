package com.example.util

import android.content.Context
import android.graphics.*
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import com.example.audio.AmbientPalette
import com.example.data.model.AppLanguage
import com.example.data.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.sin

object SongShareCardGenerator {

    suspend fun generateSongCardBitmap(
        context: Context,
        track: Track,
        palette: AmbientPalette,
        waveformEnvelope: FloatArray? = null,
        lyricSnippet: String? = null,
        lang: AppLanguage = AppLanguage.ENGLISH
    ): Uri? = withContext(Dispatchers.IO) {
        try {
            val width = 1080
            val height = 1920
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            // 1. Dark Gradient Background
            val pColor = palette.primary
            val sColor = palette.secondary
            val aColor = palette.accent

            val pInt = android.graphics.Color.rgb((pColor.red * 255).toInt(), (pColor.green * 255).toInt(), (pColor.blue * 255).toInt())
            val sInt = android.graphics.Color.rgb((sColor.red * 255).toInt(), (sColor.green * 255).toInt(), (sColor.blue * 255).toInt())
            val aInt = android.graphics.Color.rgb((aColor.red * 255).toInt(), (aColor.green * 255).toInt(), (aColor.blue * 255).toInt())

            val bgPaint = Paint().apply {
                shader = LinearGradient(
                    0f, 0f, width.toFloat(), height.toFloat(),
                    intArrayOf(android.graphics.Color.rgb(10, 8, 20), pInt, sInt, android.graphics.Color.rgb(6, 6, 12)),
                    floatArrayOf(0.0f, 0.35f, 0.75f, 1.0f),
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

            // Ambient Glow Orbs
            val orbPaint = Paint().apply {
                isAntiAlias = true
                shader = RadialGradient(
                    width * 0.5f, height * 0.35f, width * 0.6f,
                    intArrayOf(aInt, android.graphics.Color.TRANSPARENT),
                    floatArrayOf(0f, 1f),
                    Shader.TileMode.CLAMP
                )
                alpha = 90
            }
            canvas.drawCircle(width * 0.5f, height * 0.35f, width * 0.6f, orbPaint)

            // 2. Liquid Glass Card Body
            val cardRect = RectF(90f, 220f, width - 90f, height - 260f)
            val cardBgPaint = Paint().apply {
                color = android.graphics.Color.argb(80, 255, 255, 255)
                style = Paint.Style.FILL
                isAntiAlias = true
            }
            canvas.drawRoundRect(cardRect, 64f, 64f, cardBgPaint)

            val cardBorderPaint = Paint().apply {
                color = android.graphics.Color.argb(120, 255, 255, 255)
                style = Paint.Style.STROKE
                strokeWidth = 3.5f
                isAntiAlias = true
            }
            canvas.drawRoundRect(cardRect, 64f, 64f, cardBorderPaint)

            // 3. Album Cover Area
            val artSize = 680f
            val artLeft = (width - artSize) / 2f
            val artTop = 320f
            val artRect = RectF(artLeft, artTop, artLeft + artSize, artTop + artSize)

            var artLoaded = false
            if (!track.artworkUri.isNullOrEmpty()) {
                try {
                    val uri = Uri.parse(track.artworkUri)
                    val input = context.contentResolver.openInputStream(uri)
                    val srcBmp = BitmapFactory.decodeStream(input)
                    input?.close()
                    if (srcBmp != null) {
                        val path = Path().apply {
                            addRoundRect(artRect, 48f, 48f, Path.Direction.CW)
                        }
                        canvas.save()
                        canvas.clipPath(path)
                        canvas.drawBitmap(srcBmp, null, artRect, Paint(Paint.FILTER_BITMAP_FLAG))
                        canvas.restore()
                        artLoaded = true
                    }
                } catch (_: Exception) {}
            }

            if (!artLoaded) {
                val placeholderPaint = Paint().apply {
                    shader = LinearGradient(artLeft, artTop, artLeft + artSize, artTop + artSize, pInt, sInt, Shader.TileMode.CLAMP)
                    isAntiAlias = true
                }
                canvas.drawRoundRect(artRect, 48f, 48f, placeholderPaint)

                val notePaint = Paint().apply {
                    color = android.graphics.Color.WHITE
                    textSize = 140f
                    textAlign = Paint.Align.CENTER
                    isAntiAlias = true
                }
                canvas.drawText("♪", width / 2f, artTop + artSize / 2f + 45f, notePaint)
            }

            // 4. Track Details
            val titlePaint = Paint().apply {
                color = android.graphics.Color.WHITE
                textSize = 58f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }
            val title = if (track.title.length > 28) track.title.take(26) + "..." else track.title
            canvas.drawText(title, width / 2f, artTop + artSize + 110f, titlePaint)

            val artistPaint = Paint().apply {
                color = android.graphics.Color.argb(210, 200, 205, 230)
                textSize = 40f
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }
            val artist = if (track.artist.length > 34) track.artist.take(32) + "..." else track.artist
            canvas.drawText(artist, width / 2f, artTop + artSize + 180f, artistPaint)

            // 5. Waveform Bars
            val waveTop = artTop + artSize + 250f
            val waveHeight = 80f
            val bars = waveformEnvelope ?: FloatArray(36) { (sin(it * 0.3) * 0.4 + 0.5).toFloat() }
            val barCount = bars.size.coerceAtMost(48)
            val totalWaveWidth = 600f
            val barWidth = totalWaveWidth / barCount * 0.65f
            val barGap = totalWaveWidth / barCount * 0.35f
            val waveStart = (width - totalWaveWidth) / 2f

            val wavePaint = Paint().apply {
                color = aInt
                style = Paint.Style.FILL
                isAntiAlias = true
            }

            for (i in 0 until barCount) {
                val h = (bars[i] * waveHeight).coerceIn(12f, waveHeight)
                val bx = waveStart + i * (barWidth + barGap)
                val by = waveTop + (waveHeight - h) / 2f
                canvas.drawRoundRect(RectF(bx, by, bx + barWidth, by + h), 6f, 6f, wavePaint)
            }

            // 6. Lyric Snippet or Tagline
            val snippet = lyricSnippet ?: if (lang == AppLanguage.PERSIAN) "در حال گوش دادن در آئورا موزیک" else "Now Listening on Aura Music"
            val snippetPaint = Paint().apply {
                color = android.graphics.Color.WHITE
                textSize = 34f
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }
            canvas.drawText("“ $snippet ”", width / 2f, waveTop + 140f, snippetPaint)

            // 7. Aura Branding in bottom bar
            val brandPaint = Paint().apply {
                color = aInt
                textSize = 38f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
                letterSpacing = 0.15f
            }
            canvas.drawText("AURA MUSIC • LIQUID SOUND", width / 2f, height - 140f, brandPaint)

            // Save to cacheDir
            val imagesDir = File(context.cacheDir, "images")
            imagesDir.mkdirs()
            val file = File(imagesDir, "share_song_${track.id}_${System.currentTimeMillis()}.png")
            val fos = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
            fos.flush()
            fos.close()

            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (e: Exception) {
            Log.e("SongShareCard", "Error generating share card: ${e.message}", e)
            null
        }
    }
}
