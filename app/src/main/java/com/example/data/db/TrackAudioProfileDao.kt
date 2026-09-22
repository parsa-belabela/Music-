package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.TrackAudioProfile

@Dao
interface TrackAudioProfileDao {
    @Query("SELECT * FROM track_audio_profiles WHERE trackId = :trackId LIMIT 1")
    suspend fun getProfile(trackId: String): TrackAudioProfile?

    @Query("SELECT * FROM track_audio_profiles WHERE trackId IN (:trackIds)")
    suspend fun getProfiles(trackIds: List<String>): List<TrackAudioProfile>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(profile: TrackAudioProfile)

    @Query("SELECT trackId FROM track_audio_profiles")
    suspend fun getAllAnalyzedTrackIds(): List<String>
}
