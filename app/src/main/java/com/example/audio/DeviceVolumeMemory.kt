package com.example.audio

import android.content.Context

object DeviceVolumeMemory {
    private const val PREFS_NAME = "aura_device_volume_memory"

    fun deviceKey(device: ConnectedAudioDevice?): String = when {
        device == null -> "speaker"
        device.isBluetooth -> "bt_${device.name.trim().lowercase().replace(Regex("[^a-z0-9_]"), "_")}"
        device.isHeadphones -> "wired_headphones"
        else -> "speaker"
    }

    fun get(context: Context, key: String): Float? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return if (prefs.contains(key)) {
            prefs.getFloat(key, 1.0f).coerceIn(0f, 1f)
        } else {
            null
        }
    }

    fun save(context: Context, key: String, volume: Float) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putFloat(key, volume.coerceIn(0f, 1f)).apply()
    }

    fun getSavedVolume(context: Context, deviceName: String, defaultVolume: Float = 0.85f): Float {
        return get(context, deviceName) ?: defaultVolume
    }

    fun saveVolume(context: Context, deviceName: String, volume: Float) {
        save(context, deviceName, volume)
    }
}
