package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "track_audio_profiles")
data class TrackAudioProfile(
    @PrimaryKey val trackId: String,
    val energyLevel: Float = 0.5f,
    val estimatedTempoBpm: Int? = null,
    val tempoBucket: String = "MEDIUM", // "SLOW" | "MEDIUM" | "FAST"
    val waveformEnvelope: String = "",  // Normalized envelope floats separated by comma (~150 points)
    val analysisVersion: Int = 1,
    val analyzedAtTimestamp: Long = System.currentTimeMillis()
)
