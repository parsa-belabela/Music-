package com.example.data.model

enum class WrappedTimeframe {
    WEEKLY,
    MONTHLY,
    YEARLY
}

data class WrappedPeriod(
    val id: String,
    val displayNameEn: String,
    val displayNameFa: String,
    val isYearly: Boolean,
    val year: Int,
    val month: Int,
    val startTimestamp: Long,
    val endTimestamp: Long,
    val isLocked: Boolean = false,
    val daysRemainingUntilUnlock: Int = 0,
    val unlockTargetDateEn: String = "",
    val unlockTargetDateFa: String = "",
    val timeframe: WrappedTimeframe = if (isYearly) WrappedTimeframe.YEARLY else WrappedTimeframe.MONTHLY,
    val subtitleEn: String = "",
    val subtitleFa: String = ""
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
    val percentage: Float = 0f,
    val artworkUri: String? = null
)

enum class ListeningTimeSlot(
    val labelEn: String,
    val labelFa: String,
    val personaEn: String,
    val personaFa: String
) {
    MORNING("Morning (05:00 - 12:00)", "صبح (۰۵:۰۰ تا ۱۲:۰۰)", "Early Bird Rhythmist", "شنونده سحرخیز"),
    AFTERNOON("Afternoon (12:00 - 17:00)", "عصر (۱۲:۰۰ تا ۱۷:۰۰)", "Daylight Voyager", "مسافر روز و انرژی"),
    EVENING("Evening (17:00 - 22:00)", "غروب (۱۷:۰۰ تا ۲۲:۰۰)", "Twilight Dreamer", "خیالپرداز غروب"),
    NIGHT("Night (22:00 - 05:00)", "نیمهشب (۲۲:۰۰ تا ۰۵:۰۰)", "Midnight Audiophile", "شبزندهدار نئونی")
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
    val completionRate: Float,
    val hourlyDistribution: List<Int> = List(24) { 0 },
    val peakHour: Int = 22,
    val uniqueTracksCount: Int = 0,
    val uniqueArtistsCount: Int = 0
) {
    val totalListeningMinutes: Long get() = (totalListeningTimeMs / (1000 * 60)).coerceAtLeast(if (totalTracksPlayed > 0) 1L else 0L)
    val totalListeningHours: Float get() = totalListeningTimeMs / (1000f * 60f * 60f)
}
