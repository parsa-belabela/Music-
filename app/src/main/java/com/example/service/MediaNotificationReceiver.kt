package com.example.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class MediaNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        when (action) {
            ACTION_PLAY_PAUSE -> {
                AudioPlaybackService.actionHandler?.invoke("PLAY_PAUSE")
            }
            ACTION_NEXT -> {
                AudioPlaybackService.actionHandler?.invoke("NEXT")
            }
            ACTION_PREVIOUS -> {
                AudioPlaybackService.actionHandler?.invoke("PREVIOUS")
            }
            ACTION_STOP -> {
                AudioPlaybackService.actionHandler?.invoke("STOP")
            }
        }
    }

    companion object {
        const val ACTION_PLAY_PAUSE = "com.example.auramusic.ACTION_PLAY_PAUSE"
        const val ACTION_NEXT = "com.example.auramusic.ACTION_NEXT"
        const val ACTION_PREVIOUS = "com.example.auramusic.ACTION_PREVIOUS"
        const val ACTION_STOP = "com.example.auramusic.ACTION_STOP"
    }
}
