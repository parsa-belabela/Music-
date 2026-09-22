package com.example

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
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

    private val noisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
                audioEngine.pause()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        applicationScope.launch {
            repository.initDefaultDataIfNeeded()
        }

        audioAnalysisEngine.start(applicationScope)

        val filter = IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
        registerReceiver(noisyReceiver, filter)
    }

    override fun onTerminate() {
        super.onTerminate()
        unregisterReceiver(noisyReceiver)
        audioAnalysisEngine.stop()
        audioEngine.release()
    }

    companion object {
        lateinit var instance: AuraApplication
            private set
    }
}
