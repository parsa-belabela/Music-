package com.example.data.model

enum class AppLanguage(val code: String, val titleEn: String, val titleFa: String) {
    ENGLISH("en", "English", "انگلیسی"),
    PERSIAN("fa", "Persian (فارسی)", "فارسی")
}

enum class AppTheme(val titleEn: String, val titleFa: String) {
    MIDNIGHT("Midnight", "نیمه‌شب"),
    PURE_BLACK("Pure Black", "مشکی خالص"),
    AMOLED("AMOLED", "امولد"),
    GRAPHITE("Graphite", "گرافیت"),
    GLASS("Liquid Glass", "شیشه مایع"),
    NEON("Neon Cyber", "نئون سایبر"),
    CINEMA("Cinema Glow", "سینمایی"),
    MINIMAL("Minimal Dark", "مینیمال تاریک")
}

enum class VisualizerQuality(val titleEn: String, val titleFa: String, val fps: Int) {
    PERFORMANCE("Performance (30 FPS)", "اقتصادی (۳۰ فریم)", 30),
    BALANCED("Balanced (60 FPS)", "متعادل (۶۰ فریم)", 60),
    QUALITY("High Quality (120 FPS)", "بالاترین کیفیت (۱۲۰ فریم)", 120)
}

data class AppSettings(
    val language: AppLanguage = AppLanguage.ENGLISH,
    val theme: AppTheme = AppTheme.MIDNIGHT,
    val customAccentColor: Long = 0xFF8B5CF6, // Purple
    val crossfadeDurationSeconds: Int = 2,
    val gaplessEnabled: Boolean = true,
    val bassBoostStrength: Int = 300, // 0 - 1000
    val equalizerEnabled: Boolean = true,
    val eqBands: List<Float> = listOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f), // 10 bands dB (-12 to +12)
    val pauseOnInterruption: Boolean = true,
    val duckVolumeOnInterruption: Boolean = true,
    val pauseOnHeadphoneDisconnect: Boolean = true,
    val playOnHeadphoneConnect: Boolean = false,
    val lyricsFontSize: Float = 20f,
    val lyricsDisplayMode: LyricsDisplayMode = LyricsDisplayMode.CLASSIC,
    val lyricsKaraokeWordHighlight: Boolean = true,
    val visualizerMode: VisualizerMode = VisualizerMode.AMBIENT_HALO,
    val visualizerQuality: VisualizerQuality = VisualizerQuality.QUALITY,
    val visualizerSensitivity: Float = 1.0f,
    val visualizerBassResponse: Float = 1.2f,
    val visualizerGlow: Float = 0.85f,
    val visualizerFps: Int = 120,
    val autoColorFromArtwork: Boolean = true,
    val developerModeEnabled: Boolean = false,
    val gesturesEnabled: Boolean = true,
    val hapticFeedbackEnabled: Boolean = true,
    val sleepTimerMinutes: Int = 0, // 0 = disabled
    val sleepTimerFadeOut: Boolean = true,
    val preloadNextTrack: Boolean = true,
    val batterySaver: Boolean = false,
    val reducedMotion: Boolean = false
)
