package com.example.data.model

enum class VisualizerMode(val title: String, val titleFa: String) {
    AMBIENT_HALO("Ambient Halo", "هاله محیطی"),
    BASS_GLOW("Bass Glow", "درخشش بیس"),
    SPECTRUM("Spectrum", "طیف فرکانسی"),
    CIRCULAR_SPECTRUM("Circular Spectrum", "طیف دایره‌ای"),
    WAVEFORM("Waveform", "امواج صوتی"),
    RADIAL_WAVE("Radial Wave", "موج شعاعی"),
    PULSE_RING("Pulse Ring", "حلقه‌های پالس"),
    AURORA("Aurora", "شفق قطبی"),
    LIQUID("Liquid", "شیشه مایع"),
    PARTICLE_FIELD("Particle Field", "میدان ذرات"),
    DOTS("Dots", "ماتریس نقاط"),
    CINEMATIC_FOG("Cinematic Fog", "مه سینمایی");

    fun getTitle(lang: AppLanguage): String = if (lang == AppLanguage.PERSIAN) titleFa else title
}

data class VisualizerPreset(
    val id: String,
    val name: String,
    val nameFa: String = name,
    val mode: VisualizerMode = VisualizerMode.AMBIENT_HALO,
    val sensitivity: Float = 1.0f,
    val bassResponse: Float = 1.2f,
    val kickResponse: Float = 1.3f,
    val smoothness: Float = 0.75f,
    val decay: Float = 0.88f,
    val glow: Float = 0.85f,
    val blurRadius: Float = 24f,
    val scale: Float = 1.0f,
    val particleCount: Int = 40,
    val speed: Float = 1.0f,
    val autoColorFromArtwork: Boolean = true,
    val fps: Int = 60
) {
    fun getName(lang: AppLanguage): String = if (lang == AppLanguage.PERSIAN) nameFa else name

    companion object {
        val SOFT = VisualizerPreset(
            id = "soft",
            name = "Soft",
            nameFa = "ملایم (Soft)",
            mode = VisualizerMode.AMBIENT_HALO,
            sensitivity = 0.7f,
            bassResponse = 0.8f,
            kickResponse = 0.9f,
            smoothness = 0.9f,
            decay = 0.92f,
            glow = 0.6f
        )
        val DEEP_BASS = VisualizerPreset(
            id = "deep_bass",
            name = "Deep Bass",
            nameFa = "بیس عمیق",
            mode = VisualizerMode.BASS_GLOW,
            sensitivity = 1.2f,
            bassResponse = 1.8f,
            kickResponse = 1.6f,
            smoothness = 0.7f,
            decay = 0.82f,
            glow = 1.0f
        )
        val NIGHT_DRIVE = VisualizerPreset(
            id = "night_drive",
            name = "Night Drive",
            nameFa = "رانندگی شبانه",
            mode = VisualizerMode.SPECTRUM,
            sensitivity = 1.1f,
            bassResponse = 1.3f,
            kickResponse = 1.4f,
            smoothness = 0.75f,
            glow = 0.8f
        )
        val DREAM = VisualizerPreset(
            id = "dream",
            name = "Dream",
            nameFa = "رویایی (Dream)",
            mode = VisualizerMode.AURORA,
            sensitivity = 0.85f,
            bassResponse = 0.9f,
            kickResponse = 0.8f,
            smoothness = 0.88f,
            glow = 0.95f
        )
        val AGGRESSIVE = VisualizerPreset(
            id = "aggressive",
            name = "Aggressive",
            nameFa = "پرانرژی و تپنده",
            mode = VisualizerMode.PULSE_RING,
            sensitivity = 1.5f,
            bassResponse = 2.0f,
            kickResponse = 2.0f,
            smoothness = 0.5f,
            decay = 0.75f,
            glow = 1.0f
        )
        val MINIMAL = VisualizerPreset(
            id = "minimal",
            name = "Minimal",
            nameFa = "مینیمال",
            mode = VisualizerMode.WAVEFORM,
            sensitivity = 0.8f,
            bassResponse = 0.9f,
            kickResponse = 1.0f,
            smoothness = 0.85f,
            glow = 0.4f
        )
        val NEON = VisualizerPreset(
            id = "neon",
            name = "Neon",
            nameFa = "نئون درخشان",
            mode = VisualizerMode.CIRCULAR_SPECTRUM,
            sensitivity = 1.3f,
            bassResponse = 1.4f,
            kickResponse = 1.5f,
            smoothness = 0.7f,
            glow = 1.0f
        )
        val CINEMATIC = VisualizerPreset(
            id = "cinematic",
            name = "Cinematic",
            nameFa = "سینمایی (Cinematic)",
            mode = VisualizerMode.CINEMATIC_FOG,
            sensitivity = 1.0f,
            bassResponse = 1.2f,
            kickResponse = 1.2f,
            smoothness = 0.8f,
            glow = 0.85f
        )

        val DEFAULT_PRESETS = listOf(SOFT, DEEP_BASS, NIGHT_DRIVE, DREAM, AGGRESSIVE, MINIMAL, NEON, CINEMATIC)
    }
}
