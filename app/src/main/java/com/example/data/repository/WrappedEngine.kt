package com.example.data.repository

import com.example.data.model.*
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

object WrappedEngine {

    private val MONTH_NAMES_EN = listOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )

    private val MONTH_NAMES_FA = listOf(
        "ژانویه (دی / بهمن)", "فوریه (بهمن / اسفند)", "مارس (اسفند / فروردین)", "آوریل (فروردین / اردیبهشت)",
        "مه (اردیبهشت / خرداد)", "ژوئن (خرداد / تیر)", "ژوئیه (تیر / مرداد)", "اوت (مرداد / شهریور)",
        "سپتامبر (شهریور / مهر)", "اکتبر (مهر / آبان)", "نوامبر (آبان / آذر)", "دسامبر (آذر / دی)"
    )

    fun getAvailablePeriods(firstEventTime: Long, currentTime: Long = System.currentTimeMillis()): List<WrappedPeriod> {
        val periods = mutableListOf<WrappedPeriod>()
        val cal = Calendar.getInstance(TimeZone.getDefault(), Locale.ENGLISH)
        cal.timeInMillis = currentTime

        val currentYear = cal.get(Calendar.YEAR)
        val currentMonth = cal.get(Calendar.MONTH) + 1 // 1-12

        // 1. Current Year Wrapped
        val calYearStart = Calendar.getInstance().apply {
            set(currentYear, Calendar.JANUARY, 1, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val calYearEnd = Calendar.getInstance().apply {
            set(currentYear, Calendar.DECEMBER, 31, 23, 59, 59)
            set(Calendar.MILLISECOND, 999)
        }
        periods.add(
            WrappedPeriod(
                id = "YEAR_$currentYear",
                displayNameEn = "$currentYear Wrapped",
                displayNameFa = "خلاصه سال $currentYear",
                isYearly = true,
                year = currentYear,
                month = 0,
                startTimestamp = calYearStart.timeInMillis,
                endTimestamp = calYearEnd.timeInMillis
            )
        )

        // 2. Monthly Wrapped periods for past months down to firstEventTime or at least last 6 months
        val earliestCal = Calendar.getInstance().apply {
            timeInMillis = if (firstEventTime > 0) firstEventTime else (currentTime - 180L * 86400000L)
        }
        val earliestYear = earliestCal.get(Calendar.YEAR).coerceAtLeast(currentYear - 2)

        for (y in currentYear downTo earliestYear) {
            val maxMonth = if (y == currentYear) currentMonth else 12
            val minMonth = if (y == earliestYear) (earliestCal.get(Calendar.MONTH) + 1) else 1

            for (m in maxMonth downTo minMonth) {
                val mCalStart = Calendar.getInstance().apply {
                    set(y, m - 1, 1, 0, 0, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val lastDay = mCalStart.getActualMaximum(Calendar.DAY_OF_MONTH)
                val mCalEnd = Calendar.getInstance().apply {
                    set(y, m - 1, lastDay, 23, 59, 59)
                    set(Calendar.MILLISECOND, 999)
                }

                val monthNameEn = MONTH_NAMES_EN.getOrElse(m - 1) { "Month $m" }
                val monthNameFa = MONTH_NAMES_FA.getOrElse(m - 1) { "ماه $m" }

                periods.add(
                    WrappedPeriod(
                        id = "MONTH_${y}_${"%02d".format(m)}",
                        displayNameEn = "$monthNameEn $y",
                        displayNameFa = "$monthNameFa $y",
                        isYearly = false,
                        year = y,
                        month = m,
                        startTimestamp = mCalStart.timeInMillis,
                        endTimestamp = mCalEnd.timeInMillis
                    )
                )
            }
        }

        return periods
    }

    fun computeStats(
        period: WrappedPeriod,
        events: List<PlaybackEvent>,
        allTracks: List<Track>,
        firstAppTimestamp: Long
    ): WrappedStats {
        val filteredEvents = events.filter { it.timestamp in period.startTimestamp..period.endTimestamp }
        val trackMap = allTracks.associateBy { it.id }

        if (filteredEvents.isNotEmpty()) {
            val totalListeningTime = filteredEvents.sumOf { it.durationListenedMs }
            val totalTracksPlayed = filteredEvents.size
            val totalSkips = filteredEvents.count { it.wasSkipped }

            // Group by track
            val trackGroups = filteredEvents.groupBy { it.trackId }
            val topSongs = trackGroups.map { (tId, evList) ->
                val track = trackMap[tId]
                val title = track?.title ?: evList.firstOrNull()?.trackTitle ?: "Unknown Track"
                val artist = track?.artist ?: evList.firstOrNull()?.artist ?: "Unknown Artist"
                val album = track?.album ?: evList.firstOrNull()?.album ?: "Unknown Album"
                val art = track?.artworkUri
                val durationSum = evList.sumOf { it.durationListenedMs }
                TopSongItem(
                    trackId = tId,
                    title = title,
                    artist = artist,
                    album = album,
                    artworkUri = art,
                    playCount = evList.size,
                    totalDurationMs = durationSum,
                    rank = 1
                )
            }.sortedWith(compareByDescending<TopSongItem> { it.playCount }.thenByDescending { it.totalDurationMs })
                .take(5)
                .mapIndexed { idx, item -> item.copy(rank = idx + 1) }

            // Group by artist
            val artistGroups = filteredEvents.groupBy { it.artist.ifBlank { "Unknown Artist" } }
            val topArtists = artistGroups.map { (artist, evList) ->
                val dur = evList.sumOf { it.durationListenedMs }
                val minutes = dur / (1000 * 60)
                TopStatItem(
                    name = artist,
                    count = evList.size,
                    formattedDuration = "$minutes min",
                    percentage = (evList.size.toFloat() / totalTracksPlayed).coerceIn(0f, 1f)
                )
            }.sortedByDescending { it.count }.take(5)

            // Group by album
            val albumGroups = filteredEvents.groupBy { it.album.ifBlank { "Unknown Album" } }
            val topAlbums = albumGroups.map { (album, evList) ->
                TopStatItem(
                    name = album,
                    count = evList.size,
                    formattedDuration = "${evList.size} plays"
                )
            }.sortedByDescending { it.count }.take(5)

            // Group by genre
            val genreGroups = filteredEvents.groupBy { it.genre.ifBlank { "Electronic" } }
            val topGenres = genreGroups.map { (genre, evList) ->
                TopStatItem(
                    name = genre,
                    count = evList.size,
                    formattedDuration = "${evList.size} tracks",
                    percentage = (evList.size.toFloat() / totalTracksPlayed).coerceIn(0f, 1f)
                )
            }.sortedByDescending { it.count }.take(5)

            // Favorite time of day
            val timeSlotCounts = mutableMapOf(
                ListeningTimeSlot.MORNING to 0,
                ListeningTimeSlot.AFTERNOON to 0,
                ListeningTimeSlot.EVENING to 0,
                ListeningTimeSlot.NIGHT to 0
            )

            val cal = Calendar.getInstance()
            for (ev in filteredEvents) {
                cal.timeInMillis = ev.timestamp
                val hour = cal.get(Calendar.HOUR_OF_DAY)
                val slot = when (hour) {
                    in 5..11 -> ListeningTimeSlot.MORNING
                    in 12..16 -> ListeningTimeSlot.AFTERNOON
                    in 17..21 -> ListeningTimeSlot.EVENING
                    else -> ListeningTimeSlot.NIGHT
                }
                timeSlotCounts[slot] = (timeSlotCounts[slot] ?: 0) + 1
            }

            val favoriteSlot = timeSlotCounts.maxByOrNull { it.value }?.key ?: ListeningTimeSlot.NIGHT

            // Listening streak (consecutive calendar days)
            val distinctDays = filteredEvents.map { ev ->
                ev.timestamp / (86400000L)
            }.distinct().sorted()

            var maxStreak = if (distinctDays.isNotEmpty()) 1 else 0
            var currentStreak = 1
            for (i in 1 until distinctDays.size) {
                if (distinctDays[i] == distinctDays[i - 1] + 1) {
                    currentStreak++
                    if (currentStreak > maxStreak) maxStreak = currentStreak
                } else {
                    currentStreak = 1
                }
            }

            // Days with Aura
            val earliest = if (firstAppTimestamp > 0) firstAppTimestamp else (filteredEvents.minOfOrNull { it.timestamp } ?: System.currentTimeMillis())
            val daysWithAura = ((System.currentTimeMillis() - earliest) / (86400000L)).toInt().coerceAtLeast(1)

            // Personality determination
            val dominantGenre = topGenres.firstOrNull()?.name ?: "Synthwave"
            val (personaEn, personaFa, descEn, descFa) = determinePersonality(dominantGenre, favoriteSlot, totalSkips, totalTracksPlayed)

            val completionRate = if (totalTracksPlayed > 0) {
                ((totalTracksPlayed - totalSkips).toFloat() / totalTracksPlayed).coerceIn(0f, 1f)
            } else 1.0f

            return WrappedStats(
                period = period,
                totalListeningTimeMs = totalListeningTime,
                totalTracksPlayed = totalTracksPlayed,
                totalSkips = totalSkips,
                listeningStreakDays = maxStreak,
                topSongs = topSongs,
                topArtists = topArtists,
                topAlbums = topAlbums,
                topGenres = topGenres,
                favoriteTimeSlot = favoriteSlot,
                daysWithAura = daysWithAura,
                audioPersonalityEn = personaEn,
                audioPersonalityFa = personaFa,
                audioPersonalityDescEn = descEn,
                audioPersonalityDescFa = descFa,
                completionRate = completionRate
            )
        } else {
            // Authentic calculation from track library stats if events table is new
            val playedTracks = allTracks.filter { it.playCount > 0 }
            val totalPlayCount = playedTracks.sumOf { it.playCount }
            val totalDurationMs = playedTracks.sumOf { it.durationMs * it.playCount }

            val topSongs = playedTracks
                .sortedByDescending { it.playCount }
                .take(5)
                .mapIndexed { idx, t ->
                    TopSongItem(
                        trackId = t.id,
                        title = t.title,
                        artist = t.artist,
                        album = t.album,
                        artworkUri = t.artworkUri,
                        playCount = t.playCount,
                        totalDurationMs = t.durationMs * t.playCount,
                        rank = idx + 1
                    )
                }

            val topArtists = playedTracks.groupBy { it.artist }
                .map { (artist, tList) ->
                    val count = tList.sumOf { it.playCount }
                    val dur = tList.sumOf { it.durationMs * it.playCount } / (1000 * 60)
                    TopStatItem(name = artist, count = count, formattedDuration = "$dur min")
                }.sortedByDescending { it.count }.take(5)

            val topAlbums = playedTracks.groupBy { it.album }
                .map { (album, tList) ->
                    val count = tList.sumOf { it.playCount }
                    TopStatItem(name = album, count = count, formattedDuration = "$count plays")
                }.sortedByDescending { it.count }.take(5)

            val topGenres = playedTracks.groupBy { it.genre }
                .map { (genre, tList) ->
                    val count = tList.sumOf { it.playCount }
                    TopStatItem(name = genre, count = count, formattedDuration = "$count plays")
                }.sortedByDescending { it.count }.take(5)

            val earliest = if (firstAppTimestamp > 0) firstAppTimestamp else (allTracks.minOfOrNull { it.dateAdded } ?: System.currentTimeMillis())
            val daysWithAura = ((System.currentTimeMillis() - earliest) / (86400000L)).toInt().coerceAtLeast(1)

            val dominantGenre = topGenres.firstOrNull()?.name ?: "Ambient Electronic"
            val favoriteSlot = ListeningTimeSlot.NIGHT
            val (personaEn, personaFa, descEn, descFa) = determinePersonality(dominantGenre, favoriteSlot, 0, totalPlayCount.coerceAtLeast(1))

            return WrappedStats(
                period = period,
                totalListeningTimeMs = totalDurationMs,
                totalTracksPlayed = totalPlayCount,
                totalSkips = 0,
                listeningStreakDays = if (totalPlayCount > 0) 1 else 0,
                topSongs = topSongs,
                topArtists = topArtists,
                topAlbums = topAlbums,
                topGenres = topGenres,
                favoriteTimeSlot = favoriteSlot,
                daysWithAura = daysWithAura,
                audioPersonalityEn = personaEn,
                audioPersonalityFa = personaFa,
                audioPersonalityDescEn = descEn,
                audioPersonalityDescFa = descFa,
                completionRate = 1.0f
            )
        }
    }

    private fun determinePersonality(
        dominantGenre: String,
        timeSlot: ListeningTimeSlot,
        skips: Int,
        total: Int
    ): Tuple4<String, String, String, String> {
        val genreLower = dominantGenre.lowercase()
        return when {
            genreLower.contains("cyber") || genreLower.contains("synth") -> {
                Tuple4(
                    "Neon Cyber Drifter",
                    "مسافر سایبر نئون",
                    "You dive deep into synthetic arpeggios and high-octane electronic waves that fuel your focus.",
                    "شما در دنیای آرپژهای سینت‌سایزری و امواج پرانرژی الکترونیک غوطه‌ور می‌شوید که تمرکز و خلاقیت شما را تقویت می‌کند."
                )
            }
            genreLower.contains("ambient") || genreLower.contains("chill") -> {
                Tuple4(
                    "Cosmic Deep Dreamer",
                    "خیال‌پرداز کیهانی",
                    "You use soundscapes as meditation, letting harmonic frequencies shape vast atmospheric worlds.",
                    "شما موسیقی را ابزاری برای آرامش عمیق و پرواز در اتمسفرهای بی‌پایان صوتی قرار داده‌اید."
                )
            }
            timeSlot == ListeningTimeSlot.NIGHT -> {
                Tuple4(
                    "Midnight Sound Architect",
                    "معمار شبانه اصوات",
                    "Your deepest musical connections happen when the world sleeps and the stars take over.",
                    "بیشترین و خالص‌ترین پیوند شما با موسیقی زمانی شکل می‌گیرد که جهان به خواب رفته است."
                )
            }
            skips.toFloat() / total.coerceAtLeast(1) < 0.1f -> {
                Tuple4(
                    "Immersive Sonic Purist",
                    "شنونده عمیق و اصیل",
                    "You listen with absolute patience, letting every track unfold naturally from start to finish.",
                    "شما با صبر و اشتیاق واقعی به موسیقی گوش می‌دهید و اجازه می‌دهید هر اثر از ابتدا تا انتها شکوفا شود."
                )
            }
            else -> {
                Tuple4(
                    "Eclectic Frequency Explorer",
                    "کاوشگر فرکانس‌های ناب",
                    "Your ears wander freely across diverse rhythms, seeking raw emotion and sonic depth.",
                    "سلیقه شما مرزی نمی‌شناسد و آزادانه در میان انواع ریتم‌ها به دنبال حس ناب و اصالت صدا حرکت می‌کند."
                )
            }
        }
    }
}

data class Tuple4<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
