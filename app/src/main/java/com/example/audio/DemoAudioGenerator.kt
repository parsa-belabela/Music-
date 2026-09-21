package com.example.audio

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.sin

object DemoAudioGenerator {
    private const val TAG = "DemoAudioGenerator"

    fun getOrCreateDemoAudio(context: Context, trackId: String): File {
        val dir = File(context.filesDir, "aura_demos").apply { mkdirs() }
        val safeName = trackId.replace(Regex("[^a-zA-Z0-9_]"), "_")
        val file = File(dir, "$safeName.wav")

        if (file.exists() && file.length() > 1000) {
            return file
        }

        try {
            generateWavFile(file, trackId)
        } catch (e: Exception) {
            Log.e(TAG, "Failed generating demo audio for $trackId", e)
        }

        return file
    }

    private fun generateWavFile(outFile: File, trackId: String) {
        val sampleRate = 22050
        val durationSeconds = 12
        val numSamples = sampleRate * durationSeconds
        val numChannels = 2
        val bitsPerSample = 16
        val byteRate = sampleRate * numChannels * (bitsPerSample / 8)
        val blockAlign = numChannels * (bitsPerSample / 8)
        val subChunk2Size = numSamples * numChannels * (bitsPerSample / 8)
        val chunkSize = 36 + subChunk2Size

        // Musical frequencies based on track
        val baseFreq = when {
            trackId.contains("02") -> 87.31 // F2
            trackId.contains("03") -> 110.0 // A2
            else -> 65.41 // C2
        }

        val chordFreqs = when {
            trackId.contains("02") -> doubleArrayOf(87.31, 130.81, 174.61, 207.65, 261.63) // Fm7
            trackId.contains("03") -> doubleArrayOf(110.0, 164.81, 220.0, 261.63, 329.63)  // Am7
            else -> doubleArrayOf(65.41, 98.0, 130.81, 155.56, 196.0, 293.66)             // Cm9
        }

        FileOutputStream(outFile).use { fos ->
            val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
            // RIFF chunk descriptor
            header.put("RIFF".toByteArray())
            header.putInt(chunkSize)
            header.put("WAVE".toByteArray())
            // fmt sub-chunk
            header.put("fmt ".toByteArray())
            header.putInt(16) // Subchunk1Size for PCM
            header.putShort(1.toShort()) // AudioFormat 1 = PCM
            header.putShort(numChannels.toShort())
            header.putInt(sampleRate)
            header.putInt(byteRate)
            header.putShort(blockAlign.toShort())
            header.putShort(bitsPerSample.toShort())
            // data sub-chunk
            header.put("data".toByteArray())
            header.putInt(subChunk2Size)
            fos.write(header.array())

            // Write PCM audio data in chunks
            val bufferSize = 4096
            val byteBuf = ByteBuffer.allocate(bufferSize * 4).order(ByteOrder.LITTLE_ENDIAN)

            var sampleIndex = 0
            while (sampleIndex < numSamples) {
                byteBuf.clear()
                val chunkEnd = minOf(sampleIndex + bufferSize, numSamples)
                for (i in sampleIndex until chunkEnd) {
                    val t = i.toDouble() / sampleRate

                    // Ambient pad chord synthesis
                    var pad = 0.0
                    for (f in chordFreqs) {
                        pad += sin(2.0 * PI * f * t)
                    }
                    pad /= chordFreqs.size

                    // Sub-bass sine
                    val bass = sin(2.0 * PI * baseFreq * t) * 0.7

                    // 120 BPM Kick beat transient every 0.5s
                    val beatTime = t % 0.5
                    val kickEnvelope = (1.0 - (beatTime / 0.12).coerceIn(0.0, 1.0))
                    val kick = if (beatTime < 0.12) {
                        val sweepFreq = 120.0 * (1.0 - beatTime / 0.12) + 45.0
                        sin(2.0 * PI * sweepFreq * t) * kickEnvelope * 0.9
                    } else 0.0

                    // Gentle stereo tremolo/shimmer
                    val lfo = 0.5 + 0.5 * sin(2.0 * PI * 0.25 * t)
                    val leftMixed = (pad * 0.45 * lfo + bass * 0.35 + kick * 0.6).coerceIn(-1.0, 1.0)
                    val rightMixed = (pad * 0.45 * (1.0 - lfo * 0.3) + bass * 0.35 + kick * 0.6).coerceIn(-1.0, 1.0)

                    val leftShort = (leftMixed * 32767.0).toInt().toShort()
                    val rightShort = (rightMixed * 32767.0).toInt().toShort()

                    byteBuf.putShort(leftShort)
                    byteBuf.putShort(rightShort)
                }

                fos.write(byteBuf.array(), 0, (chunkEnd - sampleIndex) * 4)
                sampleIndex = chunkEnd
            }
        }
    }
}
