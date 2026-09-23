package com.example.audio

import android.content.Context
import android.util.Log
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.sin

object DemoAudioGenerator {
    private const val TAG = "DemoAudioGenerator"

    fun getOrCreateDemoAudio(context: Context, trackId: String, requestedDurationMs: Long = 214000L): File {
        val dir = File(context.filesDir, "aura_demos").apply { mkdirs() }
        val safeName = trackId.replace(Regex("[^a-zA-Z0-9_]"), "_")
        val file = File(dir, "$safeName.wav")

        val durationSec = (requestedDurationMs / 1000L).coerceIn(120L, 420L).toInt()
        val sampleRate = 22050
        val bytesPerSec = sampleRate * 2 * 2 // 16-bit stereo = 88200 bytes/sec
        val expectedMinBytes = (durationSec - 3) * bytesPerSec

        // If file exists and has full duration (not an old 12-second ~1MB stub), reuse it
        if (file.exists() && file.length() >= expectedMinBytes) {
            return file
        }

        try {
            generateWavFile(file, trackId, durationSec, sampleRate)
        } catch (e: Exception) {
            Log.e(TAG, "Failed generating demo audio for $trackId", e)
        }

        return file
    }

    private fun generateWavFile(outFile: File, trackId: String, durationSeconds: Int, sampleRate: Int) {
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
            trackId.contains("04") -> 73.42 // D2
            trackId.contains("05") -> 98.00 // G2
            else -> 65.41 // C2
        }

        val chordFreqs = when {
            trackId.contains("02") -> doubleArrayOf(87.31, 130.81, 174.61, 207.65, 261.63) // Fm7
            trackId.contains("03") -> doubleArrayOf(110.0, 164.81, 220.0, 261.63, 329.63)  // Am7
            trackId.contains("04") -> doubleArrayOf(73.42, 110.0, 146.83, 174.61, 220.0)   // Dm7
            trackId.contains("05") -> doubleArrayOf(98.00, 123.47, 146.83, 196.0, 246.94)  // Gmaj7
            else -> doubleArrayOf(65.41, 98.0, 130.81, 155.56, 196.0, 293.66)             // Cm9
        }

        // Pre-synthesize a 4-second seamless rhythmic loop (8 beats at 120 BPM)
        val loopDurationSec = 4.0
        val loopSamples = (sampleRate * loopDurationSec).toInt()
        val loopBytes = ByteBuffer.allocate(loopSamples * 4).order(ByteOrder.LITTLE_ENDIAN)

        for (i in 0 until loopSamples) {
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

            loopBytes.putShort(leftShort)
            loopBytes.putShort(rightShort)
        }
        val loopArray = loopBytes.array()

        BufferedOutputStream(FileOutputStream(outFile)).use { bos ->
            val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
            header.put("RIFF".toByteArray())
            header.putInt(chunkSize)
            header.put("WAVE".toByteArray())
            header.put("fmt ".toByteArray())
            header.putInt(16)
            header.putShort(1.toShort())
            header.putShort(numChannels.toShort())
            header.putInt(sampleRate)
            header.putInt(byteRate)
            header.putShort(blockAlign.toShort())
            header.putShort(bitsPerSample.toShort())
            header.put("data".toByteArray())
            header.putInt(subChunk2Size)
            bos.write(header.array())

            // Write pre-synthesized loop repeatedly to reach full track duration
            var bytesWritten = 0
            while (bytesWritten < subChunk2Size) {
                val toWrite = minOf(loopArray.size, subChunk2Size - bytesWritten)
                bos.write(loopArray, 0, toWrite)
                bytesWritten += toWrite
            }
            bos.flush()
        }
    }
}
