package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.media.AudioManager
import android.media.MediaMetadata
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import kotlinx.coroutines.launch

class AudioPlaybackService : Service() {
    private val channelId = "aura_music_playback"
    private val notificationId = 1001

    private var mediaSession: MediaSession? = null
    private var becomingNoisyReceiver: BroadcastReceiver? = null
    private var wakeLock: android.os.PowerManager.WakeLock? = null
    private var wifiLock: android.net.wifi.WifiManager.WifiLock? = null
    private val serviceScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO + kotlinx.coroutines.SupervisorJob())

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        setupMediaSession()
        registerBecomingNoisyReceiver()
        initBackgroundLocks()
    }

    private fun initBackgroundLocks() {
        try {
            val pm = getSystemService(Context.POWER_SERVICE) as? android.os.PowerManager
            wakeLock = pm?.newWakeLock(android.os.PowerManager.PARTIAL_WAKE_LOCK, "AuraMusic:StreamWakeLock")?.apply {
                setReferenceCounted(false)
            }
            val wm = applicationContext.getSystemService(Context.WIFI_SERVICE) as? android.net.wifi.WifiManager
            @Suppress("DEPRECATION")
            val lockMode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                android.net.wifi.WifiManager.WIFI_MODE_FULL_LOW_LATENCY
            } else {
                android.net.wifi.WifiManager.WIFI_MODE_FULL_HIGH_PERF
            }
            wifiLock = wm?.createWifiLock(lockMode, "AuraMusic:StreamWifiLock")?.apply {
                setReferenceCounted(false)
            }
        } catch (_: Exception) {}
    }

    private fun updateBackgroundLocks(active: Boolean) {
        try {
            if (active) {
                if (wakeLock?.isHeld == false) {
                    wakeLock?.acquire(4 * 60 * 60 * 1000L)
                }
                if (wifiLock?.isHeld == false) {
                    wifiLock?.acquire()
                }
            } else {
                if (wakeLock?.isHeld == true) wakeLock?.release()
                if (wifiLock?.isHeld == true) wifiLock?.release()
            }
        } catch (_: Exception) {}
    }

    private fun setupMediaSession() {
        mediaSession = MediaSession(this, "AuraMusicSession").apply {
            setCallback(object : MediaSession.Callback() {
                override fun onPlay() {
                    actionHandler?.invoke("PLAY")
                }

                override fun onPause() {
                    actionHandler?.invoke("PAUSE")
                }

                override fun onSkipToNext() {
                    actionHandler?.invoke("NEXT")
                }

                override fun onSkipToPrevious() {
                    actionHandler?.invoke("PREVIOUS")
                }

                override fun onSeekTo(pos: Long) {
                    seekHandler?.invoke(pos)
                }

                override fun onStop() {
                    actionHandler?.invoke("STOP")
                }
            })
            isActive = true
        }
    }

    private fun registerBecomingNoisyReceiver() {
        becomingNoisyReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
                    actionHandler?.invoke("HEADPHONES_DISCONNECTED")
                }
            }
        }
        val filter = IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
        registerReceiver(becomingNoisyReceiver, filter)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            MediaNotificationReceiver.ACTION_PLAY_PAUSE -> {
                actionHandler?.invoke("PLAY_PAUSE")
            }
            MediaNotificationReceiver.ACTION_NEXT -> {
                actionHandler?.invoke("NEXT")
            }
            MediaNotificationReceiver.ACTION_PREVIOUS -> {
                actionHandler?.invoke("PREVIOUS")
            }
            MediaNotificationReceiver.ACTION_STOP -> {
                updateBackgroundLocks(false)
                actionHandler?.invoke("STOP")
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_UPDATE_NOTIFICATION -> {
                val title = intent.getStringExtra("EXTRA_TITLE") ?: "Aura Music"
                val artist = intent.getStringExtra("EXTRA_ARTIST") ?: "Ready to Play"
                val album = intent.getStringExtra("EXTRA_ALBUM") ?: "Aura Sound"
                val artworkUri = intent.getStringExtra("EXTRA_ARTWORK_URI")
                val isPlaying = intent.getBooleanExtra("EXTRA_IS_PLAYING", false)
                val durationMs = intent.getLongExtra("EXTRA_DURATION", 0L)
                val positionMs = intent.getLongExtra("EXTRA_POSITION", 0L)
                val hasActiveDownloads = com.example.OnlineCacheHolder.downloadingTrackIds.value.isNotEmpty()
                updateBackgroundLocks(isPlaying || hasActiveDownloads)
                updateNotification(title, artist, album, artworkUri, isPlaying, durationMs, positionMs)
            }
        }
        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Aura Music Playback"
            val descriptionText = "Media playback controls and information"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(channelId, name, importance).apply {
                description = descriptionText
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun updateNotification(
        title: String,
        artist: String,
        album: String,
        artworkUri: String?,
        isPlaying: Boolean,
        durationMs: Long,
        positionMs: Long
    ) {
        val session = mediaSession ?: return

        // 1. Update MediaSession State
        val state = if (isPlaying) PlaybackState.STATE_PLAYING else PlaybackState.STATE_PAUSED
        val actions = PlaybackState.ACTION_PLAY or
                PlaybackState.ACTION_PAUSE or
                PlaybackState.ACTION_PLAY_PAUSE or
                PlaybackState.ACTION_SKIP_TO_NEXT or
                PlaybackState.ACTION_SKIP_TO_PREVIOUS or
                PlaybackState.ACTION_SEEK_TO or
                PlaybackState.ACTION_STOP

        val playbackState = PlaybackState.Builder()
            .setActions(actions)
            .setState(state, positionMs, 1.0f)
            .build()
        session.setPlaybackState(playbackState)

        // 2. Resolve Artwork Bitmap (from local Uri, cached online bitmap, or fallback placeholder)
        val cachedArt = loadArtworkBitmap(artworkUri)
        val artBitmap = cachedArt ?: createArtworkPlaceholder(title, artist)

        // اگر کاور آنلاین https بود و هنوز در کش حافظه نبود، در پس‌زمینه با پشتیبانی از Cross-Protocol Redirect دانلود و نوتیفیکیشن را آپدیت کن
        if (cachedArt == null && !artworkUri.isNullOrBlank() &&
            (artworkUri.startsWith("http://", ignoreCase = true) || artworkUri.startsWith("https://", ignoreCase = true))
        ) {
            serviceScope.launch {
                val downloadedArt = loadRemoteBitmapWithRedirects(artworkUri)
                if (downloadedArt != null) {
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        updateNotificationWithResolvedArt(title, artist, album, downloadedArt, isPlaying, durationMs, positionMs)
                    }
                }
            }
        }

        updateNotificationWithResolvedArt(title, artist, album, artBitmap, isPlaying, durationMs, positionMs)
    }

    private fun updateNotificationWithResolvedArt(
        title: String,
        artist: String,
        album: String,
        artBitmap: Bitmap,
        isPlaying: Boolean,
        durationMs: Long,
        positionMs: Long
    ) {
        val session = mediaSession ?: return

        // 3. Update MediaMetadata
        val metadata = MediaMetadata.Builder()
            .putString(MediaMetadata.METADATA_KEY_TITLE, title)
            .putString(MediaMetadata.METADATA_KEY_ARTIST, artist)
            .putString(MediaMetadata.METADATA_KEY_ALBUM, album)
            .putLong(MediaMetadata.METADATA_KEY_DURATION, durationMs)
            .putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, artBitmap)
            .build()
        session.setMetadata(metadata)

        // 4. Pending Intents for system notifications
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("EXPAND_NOW_PLAYING", true)
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this, 0, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val playPauseIntent = Intent(this, MediaNotificationReceiver::class.java).apply {
            action = MediaNotificationReceiver.ACTION_PLAY_PAUSE
        }
        val playPausePendingIntent = PendingIntent.getBroadcast(
            this, 1, playPauseIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val prevIntent = Intent(this, MediaNotificationReceiver::class.java).apply {
            action = MediaNotificationReceiver.ACTION_PREVIOUS
        }
        val prevPendingIntent = PendingIntent.getBroadcast(
            this, 2, prevIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val nextIntent = Intent(this, MediaNotificationReceiver::class.java).apply {
            action = MediaNotificationReceiver.ACTION_NEXT
        }
        val nextPendingIntent = PendingIntent.getBroadcast(
            this, 3, nextIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = Notification.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(artist)
            .setSubText(album)
            .setLargeIcon(artBitmap)
            .setContentIntent(openAppPendingIntent)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setOngoing(isPlaying)
            .addAction(
                Notification.Action.Builder(
                    android.R.drawable.ic_media_previous,
                    "Previous",
                    prevPendingIntent
                ).build()
            )
            .addAction(
                Notification.Action.Builder(
                    if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
                    if (isPlaying) "Pause" else "Play",
                    playPausePendingIntent
                ).build()
            )
            .addAction(
                Notification.Action.Builder(
                    android.R.drawable.ic_media_next,
                    "Next",
                    nextPendingIntent
                ).build()
            )
            .setStyle(
                Notification.MediaStyle()
                    .setMediaSession(session.sessionToken)
                    .setShowActionsInCompactView(0, 1, 2)
            )

        val notification = builder.build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(notificationId, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        } else {
            startForeground(notificationId, notification)
        }
    }

    private fun loadRemoteBitmapWithRedirects(urlStr: String): Bitmap? {
        com.example.OnlineMusicRepository.getCachedCoverBitmap(urlStr)?.let { return it }
        var currentUrl = urlStr
        var redirects = 0
        while (redirects < 5) {
            var conn: java.net.HttpURLConnection? = null
            try {
                conn = (java.net.URL(currentUrl).openConnection() as java.net.HttpURLConnection).apply {
                    instanceFollowRedirects = false
                    connectTimeout = 20000
                    readTimeout = 20000
                    doInput = true
                }
                val code = conn.responseCode
                if (code in listOf(301, 302, 303, 307, 308)) {
                    val loc = conn.getHeaderField("Location")
                    conn.disconnect()
                    if (loc.isNullOrBlank()) return null
                    currentUrl = java.net.URL(java.net.URL(currentUrl), loc).toString()
                    redirects++
                    continue
                }
                if (code in 200..299) {
                    val bmp = conn.inputStream.use { android.graphics.BitmapFactory.decodeStream(it) }
                    conn.disconnect()
                    if (bmp != null) {
                        com.example.OnlineMusicRepository.cacheCoverBitmap(urlStr, bmp)
                    }
                    return bmp
                }
                conn.disconnect()
                return null
            } catch (_: Exception) {
                try { conn?.disconnect() } catch (_: Exception) {}
                return null
            }
        }
        return null
    }

    private fun loadArtworkBitmap(artworkUriString: String?): Bitmap? {
        if (artworkUriString.isNullOrBlank()) return null
        if (artworkUriString.startsWith("http://", ignoreCase = true) || artworkUriString.startsWith("https://", ignoreCase = true)) {
            return com.example.OnlineMusicRepository.getCachedCoverBitmap(artworkUriString)
        }
        return try {
            val uri = android.net.Uri.parse(artworkUriString)
            if (uri.scheme == "content" || uri.scheme == "file") {
                contentResolver.openInputStream(uri)?.use { stream ->
                    android.graphics.BitmapFactory.decodeStream(stream)
                }
            } else {
                val file = java.io.File(artworkUriString)
                if (file.exists()) {
                    android.graphics.BitmapFactory.decodeFile(file.absolutePath)
                } else null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun createArtworkPlaceholder(title: String, artist: String): Bitmap {
        val size = 256
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val hash = (title + artist).hashCode()
        val r = (hash and 0x7F) + 50
        val g = ((hash shr 8) and 0x7F) + 50
        val b = ((hash shr 16) and 0x7F) + 100

        canvas.drawColor(Color.rgb(r, g, b))

        val paint = Paint().apply {
            color = Color.WHITE
            textSize = 96f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        val initial = title.firstOrNull()?.uppercaseChar()?.toString() ?: "A"
        canvas.drawText(initial, size / 2f, size / 2f + 32f, paint)

        return bitmap
    }

    override fun onDestroy() {
        updateBackgroundLocks(false)
        becomingNoisyReceiver?.let {
            try { unregisterReceiver(it) } catch (_: Exception) {}
        }
        mediaSession?.release()
        mediaSession = null
        super.onDestroy()
    }

    companion object {
        const val ACTION_UPDATE_NOTIFICATION = "com.example.auramusic.UPDATE_NOTIFICATION"
        var actionHandler: ((String) -> Unit)? = null
        var seekHandler: ((Long) -> Unit)? = null

        fun update(
            context: Context,
            title: String,
            artist: String,
            album: String = "Aura Music",
            artworkUri: String? = null,
            isPlaying: Boolean,
            durationMs: Long = 0L,
            positionMs: Long = 0L
        ) {
            val intent = Intent(context, AudioPlaybackService::class.java).apply {
                action = ACTION_UPDATE_NOTIFICATION
                putExtra("EXTRA_TITLE", title)
                putExtra("EXTRA_ARTIST", artist)
                putExtra("EXTRA_ALBUM", album)
                putExtra("EXTRA_ARTWORK_URI", artworkUri)
                putExtra("EXTRA_IS_PLAYING", isPlaying)
                putExtra("EXTRA_DURATION", durationMs)
                putExtra("EXTRA_POSITION", positionMs)
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                android.util.Log.w("AudioPlaybackService", "Service start error: ${e.message}")
            }
        }
    }
}
