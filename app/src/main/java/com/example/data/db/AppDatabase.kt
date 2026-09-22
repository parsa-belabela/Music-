package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.LyricsEntity
import com.example.data.model.PlaybackEvent
import com.example.data.model.Playlist
import com.example.data.model.PlaylistTrackCrossRef
import com.example.data.model.Track
import com.example.data.model.TrackAudioProfile

@Database(
    entities = [
        Track::class,
        LyricsEntity::class,
        Playlist::class,
        PlaylistTrackCrossRef::class,
        PlaybackEvent::class,
        TrackAudioProfile::class
    ],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun musicDao(): MusicDao
    abstract fun trackAudioProfileDao(): TrackAudioProfileDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Ensure all tables and columns are properly aligned
                db.execSQL("CREATE TABLE IF NOT EXISTS `playback_events` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `trackId` TEXT NOT NULL, `trackTitle` TEXT NOT NULL, `artist` TEXT NOT NULL, `album` TEXT NOT NULL, `playedAt` INTEGER NOT NULL, `durationMs` INTEGER NOT NULL, `completed` INTEGER NOT NULL)")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `tracks` ADD COLUMN `duplicateGroupId` TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE `tracks` ADD COLUMN `isHiddenDuplicate` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `tracks` ADD COLUMN `isInstrumental` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("CREATE TABLE IF NOT EXISTS `track_audio_profiles` (`trackId` TEXT PRIMARY KEY NOT NULL, `energyLevel` REAL NOT NULL, `estimatedTempoBpm` INTEGER, `tempoBucket` TEXT NOT NULL, `waveformEnvelope` TEXT NOT NULL, `analysisVersion` INTEGER NOT NULL, `analyzedAtTimestamp` INTEGER NOT NULL)")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "aura_music_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
