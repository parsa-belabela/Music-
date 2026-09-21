package com.example.data.model

data class WrappedPeriod(
    val id: String, // e.g., "MONTH_2026_09", "YEAR_2026"
    val displayNameEn: String,
    val displayNameFa: String,
    val isYearly: Boolean,
    val year: Int,
    val month: Int, // 1-12 for monthly, 0 for yearly
    val startTimestamp: Long,
    val endTimestamp: Long
)

data class TopSongItem(
    val trackId: String,
    val title: String,
    val artist: String,
    val album: String,
    val artworkUri: String?,
    val playCount: Int,
    val totalDurationMs: Long,
    val rank: Int
)

data class TopStatItem(
    val name: String,
    val count: Int,
    val formattedDuration: String,
    val percentage: Float = 0f
)

enum class ListeningTimeSlot(val labelEn: String, val labelFa: String, val personaEn: String, val personaFa: String) {
    MORNING("Morning (05:00 - 12:00)", "صبح (۰۵:۰۰ - ۱۲:۰۰)", "Early Bird Rhythmist", "شنونده سحرخیز"),
    AFTERNOON("Afternoon (12:00 - 17:00)", "عصر (۱۲:۰۰ - ۱۷:۰۰)", "Daylight Voyager", "مسافر روز و نور"),
    EVENING("Evening (17:00 - 22:00)", "غروب (۱۷:۰۰ - ۲۲:۰۰)", "Twilight Dreamer", "خیال‌پرداز غروب"),
    NIGHT("Night (22:00 - 05:00)", "نیمه‌شب (۲۲:۰۰ - ۰۵:۰۰)", "Midnight Audiophile", "شب‌زنده‌دار نئونی")
}

data class WrappedStats(
    val period: WrappedPeriod,
    val totalListeningTimeMs: Long,
    val totalTracksPlayed: Int,
    val totalSkips: Int,
    val listeningStreakDays: Int,
    val topSongs: List<TopSongItem>,
    val topArtists: List<TopStatItem>,
    val topAlbums: List<TopStatItem>,
    val topGenres: List<TopStatItem>,
    val favoriteTimeSlot: ListeningTimeSlot,
    val daysWithAura: Int,
    val audioPersonalityEn: String,
    val audioPersonalityFa: String,
    val audioPersonalityDescEn: String,
    val audioPersonalityDescFa: String,
    val completionRate: Float
) {
    val totalListeningMinutes: Long get() = totalListeningTimeMs / (1000 * 60)
    val totalListeningHours: Float get() = totalListeningMinutes / 60f
}
