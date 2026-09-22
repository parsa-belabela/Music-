package com.example.audio

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import android.util.Log
import com.example.data.model.Track
import com.example.data.model.TrackAudioProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.sqrt

class TrackAnalyzer {

    suspend fun analyze(context: Context, track: Track): TrackAudioProfile = withContext(Dispatchers.IO) {
        try {
            if (track.isDemo || track.uri.startsWith("android.resource://") || track.uri.isEmpty()) {
                return@withContext generateFallbackProfile(track)
            }

            val uri = Uri.parse(track.uri)
            val extractor = MediaExtractor()
            try {
                extractor.setDataSource(context, uri, null)
            } catch (e: Exception) {
                Log.w("TrackAnalyzer", "Could not open data source: ${e.message}")
                return@withContext generateFallbackProfile(track)
            }

            var audioTrackIndex = -1
            var format: MediaFormat? = null
            for (i in 0 until extractor.trackCount) {
                val f = extractor.getTrackFormat(i)
                val mime = f.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("audio/")) {
                    audioTrackIndex = i
                    format = f
                    break
                }
            }

            if (audioTrackIndex == -1 || format == null) {
                extractor.release()
                return@withContext generateFallbackProfile(track)
            }

            extractor.selectTrack(audioTrackIndex)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: "audio/mp4a-latm"
            val decoder = try {
                MediaCodec.createDecoderByType(mime)
            } catch (e: Exception) {
                extractor.release()
                return@withContext generateFallbackProfile(track)
            }

            decoder.configure(format, null, null, 0)
            decoder.start()

            val rawRmsList = mutableListOf<Float>()
            val bufferInfo = MediaCodec.BufferInfo()
            var isEOS = false
            val maxSamplesToAnalyze = 1500 // Limit time spent analyzing per track

            var samplesCount = 0
            val timeoutUs = 5000L

            while (!isEOS && samplesCount < maxSamplesToAnalyze) {
                val inIndex = decoder.dequeueInputBuffer(timeoutUs)
                if (inIndex >= 0) {
                    val inputBuffer = decoder.getInputBuffer(inIndex)
                    if (inputBuffer != null) {
                        val sampleSize = extractor.readSampleData(inputBuffer, 0)
                        if (sampleSize < 0) {
                            decoder.queueInputBuffer(inIndex, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            isEOS = true
                        } else {
                            decoder.queueInputBuffer(inIndex, 0, sampleSize, extractor.sampleTime, 0)
                            extractor.advance()
                        }
                    }
                }

                val outIndex = decoder.dequeueOutputBuffer(bufferInfo, timeoutUs)
                if (outIndex >= 0) {
                    val outputBuffer = decoder.getOutputBuffer(outIndex)
                    if (outputBuffer != null && bufferInfo.size > 0) {
                        outputBuffer.position(bufferInfo.offset)
                        outputBuffer.limit(bufferInfo.offset + bufferInfo.size)
                        val shortBuffer = outputBuffer.order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
                        
                        var sumSq = 0.0
                        var count = 0
                        val step = 4 // Subsample
                        while (shortBuffer.hasRemaining()) {
                            val sample = shortBuffer.get()
                            if (count % step == 0) {
                                val norm = sample.toFloat() / 32768f
                                sumSq += (norm * norm)
                            }
                            count++
                        }
                        val rms = if (count > 0) sqrt(sumSq / (count / step + 1)).toFloat() else 0f
                        rawRmsList.add(rms)
                        samplesCount++
                    }
                    decoder.releaseOutputBuffer(outIndex, false)
                }
            }

            decoder.stop()
            decoder.release()
            extractor.release()

            if (rawRmsList.isEmpty()) {
                return@withContext generateFallbackProfile(track)
            }

            // Downsample raw RMS to exactly 140 normalized points (0.0 to 1.0)
            val envelopePoints = 140
            val maxRms = (rawRmsList.maxOrNull() ?: 1f).coerceAtLeast(0.01f)
            val envelope = FloatArray(envelopePoints)
            val blockSize = rawRmsList.size.toFloat() / envelopePoints

            for (i in 0 until envelopePoints) {
                val start = (i * blockSize).toInt().coerceIn(0, rawRmsList.size - 1)
                val end = ((i + 1) * blockSize).toInt().coerceIn(start + 1, rawRmsList.size)
                var sum = 0f
                var count = 0
                for (j in start until end) {
                    sum += rawRmsList[j]
                    count++
                }
                val avg = if (count > 0) sum / count else rawRmsList[start]
                envelope[i] = (avg / maxRms).coerceIn(0.05f, 1.0f)
            }

            val avgEnergy = (envelope.average().toFloat()).coerceIn(0.1f, 0.95f)

            // Lightweight autocorrelation for tempo estimation
            val estimatedBpm = estimateTempo(rawRmsList)
            val tempoBucket = determineTempoBucket(estimatedBpm, track.genre)

            TrackAudioProfile(
                trackId = track.id,
                energyLevel = avgEnergy,
                estimatedTempoBpm = estimatedBpm,
                tempoBucket = tempoBucket,
                waveformEnvelope = envelope.joinToString(",") { "%.3f".format(it) },
                analysisVersion = 1,
                analyzedAtTimestamp = System.currentTimeMillis()
            )
        } catch (e: Exception) {
            Log.w("TrackAnalyzer", "Error analyzing track ${track.id}: ${e.message}")
            generateFallbackProfile(track)
        }
    }

    private fun estimateTempo(rmsList: List<Float>): Int? {
        if (rmsList.size < 60) return null
        val n = min(rmsList.size, 300)
        var maxCorr = 0f
        var bestLag = -1

        // Search lags between ~60 BPM and ~180 BPM
        // Assuming ~10 frames per second: lag 3 (~200 bpm) to lag 10 (~60 bpm)
        for (lag in 3..12) {
            var corr = 0f
            for (i in 0 until (n - lag)) {
                corr += rmsList[i] * rmsList[i + lag]
            }
            if (corr > maxCorr) {
                maxCorr = corr
                bestLag = lag
            }
        }

        return if (bestLag > 0 && maxCorr > 0.05f) {
            val approxBpm = (600 / bestLag).coerceIn(65, 175)
            approxBpm
        } else {
            null
        }
    }

    private fun determineTempoBucket(bpm: Int?, genre: String): String {
        if (bpm != null) {
            return when {
                bpm < 95 -> "SLOW"
                bpm > 128 -> "FAST"
                else -> "MEDIUM"
            }
        }
        val g = genre.lowercase()
        return when {
            g.contains("ballad") || g.contains("ambient") || g.contains("chill") || g.contains("acoustic") || g.contains("lo-fi") -> "SLOW"
            g.contains("dance") || g.contains("edm") || g.contains("techno") || g.contains("rock") || g.contains("house") -> "FAST"
            else -> "MEDIUM"
        }
    }

    private fun generateFallbackProfile(track: Track): TrackAudioProfile {
        // Create an organic pseudo-waveform derived from track metadata hash
        val points = 140
        val hash = (track.title + track.artist + track.id).hashCode()
        val random = java.util.Random(hash.toLong())
        val envelope = FloatArray(points)
        
        var current = 0.4f
        for (i in 0 until points) {
            val wave1 = (sinValue(i * 0.12 + hash % 10) * 0.25).toFloat()
            val wave2 = (sinValue(i * 0.04 + hash % 7) * 0.35).toFloat()
            val noise = (random.nextFloat() - 0.5f) * 0.15f
            current = (0.5f + wave1 + wave2 + noise).coerceIn(0.12f, 0.98f)
            envelope[i] = current
        }

        val tempoBucket = determineTempoBucket(null, track.genre)
        val energy = when (tempoBucket) {
            "FAST" -> 0.78f
            "SLOW" -> 0.38f
            else -> 0.55f
        }

        return TrackAudioProfile(
            trackId = track.id,
            energyLevel = energy,
            estimatedTempoBpm = if (tempoBucket == "FAST") 130 else if (tempoBucket == "SLOW") 80 else 110,
            tempoBucket = tempoBucket,
            waveformEnvelope = envelope.joinToString(",") { "%.3f".format(it) },
            analysisVersion = 1,
            analyzedAtTimestamp = System.currentTimeMillis()
        )
    }

    private fun sinValue(angle: Double): Double = kotlin.math.sin(angle)
}
