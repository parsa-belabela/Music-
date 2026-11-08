package com.example

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.media.AudioManager
import android.os.StrictMode
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.example.audio.AudioAnalysisEngine
import com.example.audio.AudioEngine
import com.example.data.db.AppDatabase
import com.example.data.repository.MusicRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AuraApplication : Application(), ImageLoaderFactory {
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .bitmapConfig(Bitmap.Config.ARGB_8888)
            .allowRgb565(false)
            .crossfade(true)
            .build()
    }

    val database by lazy { AppDatabase.getInstance(this) }
    val repository by lazy { MusicRepository(this, database.musicDao()) }
    val audioEngine by lazy { AudioEngine(this) }
    val audioAnalysisEngine by lazy { AudioAnalysisEngine() }
    val trackAnalyzer by lazy { com.example.audio.TrackAnalyzer() }
    val entitlementManager by lazy { com.example.monetization.EntitlementManager.getInstance(this) }

    override fun onCreate() {
        super.onCreate()
        instance = this
        com.example.data.model.UserTrackMetadataStore.init(this)
        entitlementManager.refreshRemoteConfig()

        // راه‌اندازی تپسل با کلید خالی (بعداً کلید واقعی تپسل را اینجا قرار بده)
        com.example.monetization.TapsellPlus.initialize(
            context = this,
            appKey = "" // TODO: Insert Tapsell AppKey here
        )

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
