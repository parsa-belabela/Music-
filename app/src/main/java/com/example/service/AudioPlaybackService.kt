package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R

class AudioPlaybackService : Service() {
    private val channelId = "aura_music_playback"
    private val notificationId = 1001

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
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
                val isPlaying = intent.getBooleanExtra("EXTRA_IS_PLAYING", false)
                updateNotification(title, artist, isPlaying)
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

    private fun updateNotification(title: String, artist: String, isPlaying: Boolean) {
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

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(artist)
            .setContentIntent(openAppPendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(isPlaying)
            .addAction(android.R.drawable.ic_media_previous, "Previous", prevPendingIntent)
            .addAction(
                if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
                if (isPlaying) "Pause" else "Play",
                playPausePendingIntent
            )
            .addAction(android.R.drawable.ic_media_next, "Next", nextPendingIntent)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(notificationId, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        } else {
            startForeground(notificationId, notification)
        }
    }

    companion object {
        const val ACTION_UPDATE_NOTIFICATION = "com.example.auramusic.UPDATE_NOTIFICATION"
        var actionHandler: ((String) -> Unit)? = null

        fun update(context: Context, title: String, artist: String, isPlaying: Boolean) {
            val intent = Intent(context, AudioPlaybackService::class.java).apply {
                action = ACTION_UPDATE_NOTIFICATION
                putExtra("EXTRA_TITLE", title)
                putExtra("EXTRA_ARTIST", artist)
                putExtra("EXTRA_IS_PLAYING", isPlaying)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }
}
