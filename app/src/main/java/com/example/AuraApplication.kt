package com.example

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.graphics.Bitmap
import android.os.StrictMode
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
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

    val database by lazy { AppDatabase.getInstance(this) }
    val repository by lazy { MusicRepository(this, database.musicDao()) }
    val audioEngine by lazy { AudioEngine(this) }
    val audioAnalysisEngine by lazy { AudioAnalysisEngine() }
    val trackAnalyzer by lazy { com.example.audio.TrackAnalyzer() }
    val entitlementManager by lazy { com.example.monetization.EntitlementManager.getInstance(this) }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .allowRgb565(false)
            .bitmapConfig(Bitmap.Config.ARGB_8888)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("aura_coil_disk_cache"))
                    .maxSizeBytes(250L * 1024L * 1024L)
                    .build()
            }
            .crossfade(true)
            .build()
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        com.example.data.model.UserTrackMetadataStore.init(this)
        entitlementManager.refreshRemoteConfig()

        // پیش‌بارگذاری فوری کاتالوگ آنلاین از کش یا کلادفلر به محض باز شدن برنامه
        applicationScope.launch(Dispatchers.IO) {
            try {
                val cached = OnlineMusicRepository.loadCachedAppTracksAsync()
                if (cached.isEmpty()) {
                    OnlineMusicRepository.fetchFromWorker()
                }
            } catch (_: Throwable) {}
        }

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
