package com.example.data.model

enum class AppLanguage(val code: String, val titleEn: String, val titleFa: String) {
    ENGLISH("en", "English", "انگلیسی"),
    PERSIAN("fa", "Persian (فارسی)", "فارسی")
}

enum class AppTheme(val id: String, val titleEn: String, val titleFa: String, val descEn: String, val descFa: String) {
    PURE_LIQUID_GLASS("pure_liquid_glass", "Pure Liquid Glass", "شیشه مایع کریستالی", "Ultra-translucent frosted crystal glass, specular gloss & chromatic glare", "شیشه کریستالی مات فوق شفاف، درخشش براق منشوری و لبه‌های بلورین"),
    CYBER_NIGHTS("cyber_nights", "Cyber Nights", "شب‌های سایبری", "Neon electric cyan, deep navy & hot magenta", "نئون سایان الکتریک، سرمه‌ای عمیق و ماژنتا لیزری"),
    VELVET_NOIR("velvet_noir", "Royal Obsidian", "ولوت نوآر سلطنتی", "Deep obsidian plum, brushed 24k gold & royal amethyst", "ابسیدین شرابی عمیق، طلای ۲۴ عیار و آمتیست سلطنتی"),
    SUNSET_RAVE("sunset_rave", "Sunset Rave", "غروب نئونی تابناک", "Solar amber, dusk violet & radiant pink heat", "نارنجی خورشیدی، بنفش غروب و صورتی درخشان نئونی"),
    DIGITAL_ACID("digital_acid", "Toxic Acid Matrix", "ماتریکس اسید فسفری", "OLED pure black & radioactive neon acid lime", "سیاه مطلق اولد و سبز فسفری نئونی پرتوزا"),
    Y2K_CHROME("chrome", "Liquid Chrome Y2K", "کروم مایع نقره‌ای", "Liquid mercury, titanium silver sheen & futuristic reflections", "فلز مایع و جیوه نقره‌ای، صیقل تیتانیومی و انعکاس‌های متالیک")
}

enum class VisualizerQuality(val titleEn: String, val titleFa: String, val fps: Int) {
    PERFORMANCE("Performance (30 FPS)", "اقتصادی (۳۰ فریم)", 30),
    BALANCED("Balanced (60 FPS)", "متعادل (۶۰ فریم)", 60),
    QUALITY("High Quality (120 FPS)", "بالاترین کیفیت (۱۲۰ فریم)", 120)
}

data class AppSettings(
    val language: AppLanguage = AppLanguage.ENGLISH,
    val theme: AppTheme = AppTheme.PURE_LIQUID_GLASS,
    val customAccentColor: Long = 0xFF00E5FF, // Luminous Cyan
    val crossfadeDurationSeconds: Int = 2,
    val gaplessEnabled: Boolean = true,
    val bassBoostStrength: Int = 0, // 0 - 1000 (0 = Flat, pure uncolored sound)
    val equalizerEnabled: Boolean = true,
    val eqPreset: String = "Flat (Studio)",
    val eqBands: List<Float> = listOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f), // 10 bands dB (-12 to +12) Flat by default
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
    val playbackSpeed: Float = 1.0f,
    val continuousMixEnabled: Boolean = false,
    val focusModeEnabled: Boolean = false,
    val appIconTheme: String = "pure_liquid_glass",
    val unlockedNowPlayingStyles: String = "default",
    val selectedNowPlayingStyle: String = "default",
    val personalEqBands: List<Float> = listOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f)
)
