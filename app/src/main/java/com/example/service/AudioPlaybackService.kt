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

class AudioPlaybackService : Service() {
    private val channelId = "aura_music_playback"
    private val notificationId = 1001

    private var mediaSession: MediaSession? = null
    private var becomingNoisyReceiver: BroadcastReceiver? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        setupMediaSession()
        registerBecomingNoisyReceiver()
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

        // 2. Resolve Artwork Bitmap (from Uri or fallback placeholder)
        val artBitmap = loadArtworkBitmap(artworkUri) ?: createArtworkPlaceholder(title, artist)

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
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
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

    private fun loadArtworkBitmap(artworkUriString: String?): Bitmap? {
        if (artworkUriString.isNullOrBlank()) return null
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
