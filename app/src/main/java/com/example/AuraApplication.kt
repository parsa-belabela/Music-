package com.example

import android.app.Application
import android.graphics.Bitmap
import android.os.StrictMode
import coil.Coil
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
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class AuraApplication : Application(), ImageLoaderFactory {
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val database by lazy { AppDatabase.getInstance(this) }
    val repository by lazy { MusicRepository(this, database.musicDao()) }
    val audioEngine by lazy { AudioEngine(this) }
    val audioAnalysisEngine by lazy { AudioAnalysisEngine() }
    val trackAnalyzer by lazy { com.example.audio.TrackAnalyzer() }
    val entitlementManager by lazy { com.example.monetization.EntitlementManager.getInstance(this) }

    val globalImageLoader: ImageLoader by lazy {
        val dispatcher = Dispatcher().apply {
            maxRequests = 8
            maxRequestsPerHost = 4
        }
        val okHttpClient = OkHttpClient.Builder()
            .dispatcher(dispatcher)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()

        ImageLoader.Builder(this)
            .okHttpClient(okHttpClient)
            .respectCacheHeaders(false)
            .allowRgb565(true)
            .bitmapConfig(Bitmap.Config.RGB_565)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .networkCachePolicy(CachePolicy.ENABLED)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(filesDir.resolve("aura_permanent_covers"))
                    .maxSizeBytes(500L * 1024L * 1024L)
                    .build()
            }
            .crossfade(true)
            .build()
    }

    override fun newImageLoader(): ImageLoader = globalImageLoader

    override fun onCreate() {
        super.onCreate()
        instance = this
        Coil.setImageLoader(globalImageLoader)
        com.example.data.model.UserTrackMetadataStore.init(this)
        entitlementManager.refreshRemoteConfig()

        // بارگذاری فوری کاتالوگ از catalog_cache.json در کسری از ثانیه و همگام‌سازی بی‌صدا فقط یک بار در هر نشست
        applicationScope.launch(Dispatchers.IO) {
            try {
                OnlineMusicRepository.loadCachedAppTracksAsync()
                OnlineMusicRepository.syncCatalogOncePerSession()
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
