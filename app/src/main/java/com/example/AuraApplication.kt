package com.example

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.StrictMode
import com.example.audio.AudioAnalysisEngine
import com.example.audio.AudioEngine
import com.example.data.db.AppDatabase
import com.example.data.repository.MusicRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AuraApplication : Application() {
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val database by lazy { AppDatabase.getInstance(this) }
    val repository by lazy { MusicRepository(this, database.musicDao()) }
    val audioEngine by lazy { AudioEngine(this) }
    val audioAnalysisEngine by lazy { AudioAnalysisEngine() }
    val trackAnalyzer by lazy { com.example.audio.TrackAnalyzer() }

    override fun onCreate() {
        super.onCreate()
        instance = this

        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(
                StrictMode.ThreadPolicy.Builder()
                    .detectDiskReads()
                    .detectDiskWrites()
                    .detectNetwork()
                    .penaltyLog()
                    .build()
            )
            StrictMode.setVmPolicy(
                StrictMode.VmPolicy.Builder()
                    .detectLeakedSqlLiteObjects()
                    .detectLeakedClosableObjects()
                    .penaltyLog()
                    .build()
            )
        }

        // Initialize database defaults and analysis asynchronously off the main thread
        applicationScope.launch(Dispatchers.IO) {
            repository.initDefaultDataIfNeeded()
            audioAnalysisEngine.start(applicationScope)
        }
    }

    companion object {
        lateinit var instance: AuraApplication
            private set
    }
}
