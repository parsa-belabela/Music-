package com.example.data.model

enum class AppLanguage(val code: String, val titleEn: String, val titleFa: String) {
    ENGLISH("en", "English", "انگلیسی"),
    PERSIAN("fa", "Persian (فارسی)", "فارسی")
}

enum class AppTheme(val id: String, val titleEn: String, val titleFa: String, val descEn: String, val descFa: String) {
    CYBER_NIGHTS("cyber_nights", "Cyber Nights", "شب‌های سایبری", "Neon cyan, deep navy & electric magenta", "نئون سایان، سرمه‌ای عمیق و ماژنتا الکتریک"),
    Y2K_CHROME("chrome", "Y2K Chrome", "کروم متالیک Y2K", "Liquid mercury, silver sheen & industrial edge", "فلز مایع، جیوه نقره‌ای و بافت صنعتی"),
    VELVET_NOIR("velvet_noir", "Velvet Noir", "ولوت نوآر", "Deep obsidian, rich burgundy & warm gold", "ابسیدین عمیق، شرابی غنی و طلایی لوکس"),
    SUNSET_RAVE("sunset_rave", "Sunset Rave", "غروب پرانرژی", "Solar orange, dusk purple & vibrant heat", "نارنجی خورشیدی، بنفش غروب و انرژی تابناک"),
    DIGITAL_ACID("digital_acid", "Digital Acid", "دیجیتال اسید", "OLED pitch black & electric acid lime", "سیاه مطلق اولد و سبز نئونی اسید لایم"),
    MINIMAL_STUDIO("minimal_studio", "Minimal Studio", "مینیمال استودیو", "Crisp monochrome, graphite & surgical studio", "تک‌رنگ، گرافیت و طراحی استودیویی تمیز")
}

enum class VisualizerQuality(val titleEn: String, val titleFa: String, val fps: Int) {
    PERFORMANCE("Performance (30 FPS)", "اقتصادی (۳۰ فریم)", 30),
    BALANCED("Balanced (60 FPS)", "متعادل (۶۰ فریم)", 60),
    QUALITY("High Quality (120 FPS)", "بالاترین کیفیت (۱۲۰ فریم)", 120)
}

data class AppSettings(
    val language: AppLanguage = AppLanguage.ENGLISH,
    val theme: AppTheme = AppTheme.CYBER_NIGHTS,
    val customAccentColor: Long = 0xFF8B5CF6, // Purple
    val crossfadeDurationSeconds: Int = 2,
    val gaplessEnabled: Boolean = true,
    val bassBoostStrength: Int = 300, // 0 - 1000
    val equalizerEnabled: Boolean = true,
    val eqPreset: String = "Flat",
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
    val reducedMotion: Boolean = false,
    val playbackSpeed: Float = 1.0f
)
