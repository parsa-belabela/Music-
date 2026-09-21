package com.example.util

import com.example.data.model.*
import java.util.Calendar
import java.util.Locale
import kotlin.math.max

object WrappedEngine {

    fun getAvailablePeriods(firstTimestamp: Long): List<WrappedPeriod> {
        val periods = mutableListOf<WrappedPeriod>()
        val cal = Calendar.getInstance()
        val now = cal.timeInMillis
        val currentYear = cal.get(Calendar.YEAR)
        val currentMonth = cal.get(Calendar.MONTH) + 1 // 1-12
        val currentDay = cal.get(Calendar.DAY_OF_MONTH)

        // 1. Current Month Period
        val currentMonthCal = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val monthStart = currentMonthCal.timeInMillis
        val maxDaysInCurrentMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        currentMonthCal.set(Calendar.DAY_OF_MONTH, maxDaysInCurrentMonth)
        currentMonthCal.set(Calendar.HOUR_OF_DAY, 23)
        currentMonthCal.set(Calendar.MINUTE, 59)
        currentMonthCal.set(Calendar.SECOND, 59)
        currentMonthCal.set(Calendar.MILLISECOND, 999)
        val monthEnd = currentMonthCal.timeInMillis

        val daysLeftInMonth = max(1, maxDaysInCurrentMonth - currentDay)

        val monthNameEn = getMonthNameEn(currentMonth)
        val monthNameFa = getMonthNameFa(currentMonth)

        periods.add(
            WrappedPeriod(
                id = "MONTH_${currentYear}_${String.format(Locale.US, "%02d", currentMonth)}",
                displayNameEn = "$monthNameEn $currentYear",
                displayNameFa = "$monthNameFa $currentYear",
                isYearly = false,
                year = currentYear,
                month = currentMonth,
                startTimestamp = monthStart,
                endTimestamp = monthEnd,
                isLocked = true, // Current active month is locked until the month concludes!
                daysRemainingUntilUnlock = daysLeftInMonth,
                unlockTargetDateEn = "End of $monthNameEn $currentYear",
                unlockTargetDateFa = "پایان ماه $monthNameFa $currentYear"
            )
        )

        // 2. Previous Month (if any) - Unlocked because it is completed!
        val prevMonthCal = Calendar.getInstance().apply {
            add(Calendar.MONTH, -1)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val prevYear = prevMonthCal.get(Calendar.YEAR)
        val prevMonth = prevMonthCal.get(Calendar.MONTH) + 1
        val prevMonthStart = prevMonthCal.timeInMillis
        val maxDaysInPrevMonth = prevMonthCal.getActualMaximum(Calendar.DAY_OF_MONTH)
        prevMonthCal.set(Calendar.DAY_OF_MONTH, maxDaysInPrevMonth)
        prevMonthCal.set(Calendar.HOUR_OF_DAY, 23)
        prevMonthCal.set(Calendar.MINUTE, 59)
        prevMonthCal.set(Calendar.SECOND, 59)
        val prevMonthEnd = prevMonthCal.timeInMillis

        val prevMonthNameEn = getMonthNameEn(prevMonth)
        val prevMonthNameFa = getMonthNameFa(prevMonth)

        periods.add(
            WrappedPeriod(
                id = "MONTH_${prevYear}_${String.format(Locale.US, "%02d", prevMonth)}",
                displayNameEn = "$prevMonthNameEn $prevYear",
                displayNameFa = "$prevMonthNameFa $prevYear",
                isYearly = false,
                year = prevYear,
                month = prevMonth,
                startTimestamp = prevMonthStart,
                endTimestamp = prevMonthEnd,
                isLocked = false, // Past month is completed
                daysRemainingUntilUnlock = 0,
                unlockTargetDateEn = "Completed",
                unlockTargetDateFa = "تکمیل شده"
            )
        )

        // 3. Current Year Wrapped Period (Locked until December 1st or end of year)
        val yearStartCal = Calendar.getInstance().apply {
            set(Calendar.MONTH, Calendar.JANUARY)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val yearEndCal = Calendar.getInstance().apply {
            set(Calendar.MONTH, Calendar.DECEMBER)
            set(Calendar.DAY_OF_MONTH, 31)
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }

        // Yearly Wrapped unlocks around Dec 1
        val unlockYearCal = Calendar.getInstance().apply {
            set(Calendar.MONTH, Calendar.DECEMBER)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val isYearLocked = now < unlockYearCal.timeInMillis
        val daysUntilYearUnlock = if (isYearLocked) {
            max(1, ((unlockYearCal.timeInMillis - now) / (1000L * 60 * 60 * 24)).toInt())
        } else 0

        periods.add(
            WrappedPeriod(
                id = "YEAR_$currentYear",
                displayNameEn = "2026 Wrapped",
                displayNameFa = "رپد ۲۰۲۶ سالانه",
                isYearly = true,
                year = currentYear,
                month = 0,
                startTimestamp = yearStartCal.timeInMillis,
                endTimestamp = yearEndCal.timeInMillis,
                isLocked = isYearLocked,
                daysRemainingUntilUnlock = daysUntilYearUnlock,
                unlockTargetDateEn = "December 1, $currentYear",
                unlockTargetDateFa = "۱ دسامبر $currentYear"
            )
        )

        return periods
    }

    fun computeStats(
        period: WrappedPeriod,
        events: List<PlaybackEvent>,
        allTracks: List<Track>,
        firstTimestamp: Long
    ): WrappedStats {
        val periodEvents = events.filter { it.timestamp in period.startTimestamp..period.endTimestamp }
        val trackMap = allTracks.associateBy { it.id }

        var totalListeningTimeMs = 0L
        var totalSkips = 0
        val trackPlayCount = mutableMapOf<String, Int>()
        val trackListeningTime = mutableMapOf<String, Long>()
        val artistPlayCount = mutableMapOf<String, Int>()
        val albumPlayCount = mutableMapOf<String, Int>()
        val genrePlayCount = mutableMapOf<String, Int>()
        val hourDistribution = IntArray(24)
        val activeDays = mutableSetOf<String>()

        val cal = Calendar.getInstance()

        for (event in periodEvents) {
            totalListeningTimeMs += event.durationListenedMs
            if (event.wasSkipped) totalSkips++

            trackPlayCount[event.trackId] = (trackPlayCount[event.trackId] ?: 0) + 1
            trackListeningTime[event.trackId] = (trackListeningTime[event.trackId] ?: 0L) + event.durationListenedMs

            if (event.artist.isNotBlank() && event.artist != "Unknown Artist") {
                artistPlayCount[event.artist] = (artistPlayCount[event.artist] ?: 0) + 1
            }
            if (event.album.isNotBlank() && event.album != "Unknown Album") {
                albumPlayCount[event.album] = (albumPlayCount[event.album] ?: 0) + 1
            }
            if (event.genre.isNotBlank() && event.genre != "Unknown") {
                genrePlayCount[event.genre] = (genrePlayCount[event.genre] ?: 0) + 1
            }

            cal.timeInMillis = event.timestamp
            val hour = cal.get(Calendar.HOUR_OF_DAY)
            hourDistribution[hour]++

            val dayKey = "${cal.get(Calendar.YEAR)}_${cal.get(Calendar.DAY_OF_YEAR)}"
            activeDays.add(dayKey)
        }

        // Top 5 Songs
        val topSongs = trackPlayCount.entries
            .sortedByDescending { it.value }
            .take(5)
            .mapIndexed { index, entry ->
                val track = trackMap[entry.key]
                val matchingEvent = periodEvents.firstOrNull { it.trackId == entry.key }
                TopSongItem(
                    trackId = entry.key,
                    title = track?.title ?: matchingEvent?.trackTitle ?: "Track",
                    artist = track?.artist ?: matchingEvent?.artist ?: "Artist",
                    album = track?.album ?: matchingEvent?.album ?: "Album",
                    artworkUri = track?.artworkUri,
                    playCount = entry.value,
                    totalDurationMs = trackListeningTime[entry.key] ?: 0L,
                    rank = index + 1
                )
            }

        // Top Artists
        val totalArtistPlays = artistPlayCount.values.sum().coerceAtLeast(1)
        val topArtists = artistPlayCount.entries
            .sortedByDescending { it.value }
            .take(5)
            .map { entry ->
                TopStatItem(
                    name = entry.key,
                    count = entry.value,
                    formattedDuration = "${entry.value} plays",
                    percentage = (entry.value.toFloat() / totalArtistPlays) * 100f
                )
            }

        // Top Albums
        val topAlbums = albumPlayCount.entries
            .sortedByDescending { it.value }
            .take(5)
            .map { entry ->
                TopStatItem(
                    name = entry.key,
                    count = entry.value,
                    formattedDuration = "${entry.value} plays"
                )
            }

        // Top Genres
        val totalGenrePlays = genrePlayCount.values.sum().coerceAtLeast(1)
        val topGenres = genrePlayCount.entries
            .sortedByDescending { it.value }
            .take(5)
            .map { entry ->
                TopStatItem(
                    name = entry.key,
                    count = entry.value,
                    formattedDuration = "${entry.value} plays",
                    percentage = (entry.value.toFloat() / totalGenrePlays) * 100f
                )
            }

        // Prime Time Slot
        val morningScore = (5..11).sumOf { hourDistribution[it] }
        val afternoonScore = (12..16).sumOf { hourDistribution[it] }
        val eveningScore = (17..21).sumOf { hourDistribution[it] }
        val nightScore = (22..23).sumOf { hourDistribution[it] } + (0..4).sumOf { hourDistribution[it] }

        val primeTimeSlot = when {
            morningScore >= maxOf(afternoonScore, eveningScore, nightScore) -> ListeningTimeSlot.MORNING
            afternoonScore >= maxOf(morningScore, eveningScore, nightScore) -> ListeningTimeSlot.AFTERNOON
            eveningScore >= maxOf(morningScore, afternoonScore, nightScore) -> ListeningTimeSlot.EVENING
            else -> ListeningTimeSlot.NIGHT
        }

        // Audio Personality Archetype
        val (personalityEn, personalityFa, descEn, descFa) = when {
            totalListeningTimeMs > 10 * 3600 * 1000L -> Quadruple(
                "Soundscape Luminary",
                "کیهان‌شناس موسیقی",
                "You live with music in every waking moment, diving deep into acoustic dimensions.",
                "موسیقی جریان دائمی زندگی شماست؛ غرق در فرکانس‌ها و ابعاد بی‌پایان صوت."
            )
            topGenres.any { it.name.contains("ambient", ignoreCase = true) || it.name.contains("chill", ignoreCase = true) } -> Quadruple(
                "Atmospheric Dreamer",
                "خیال‌پرداز اتمسفریک",
                "Drawn to subtle echoes, ethereal pads, and deep ambient textures.",
                "شیفته پدهای ملکوتی، نوسانات آرامش‌بخش و فضای بی‌پایان امبینت."
            )
            topGenres.any { it.name.contains("synth", ignoreCase = true) || it.name.contains("cyber", ignoreCase = true) || it.name.contains("electronic", ignoreCase = true) } -> Quadruple(
                "Neon Cyber Voyager",
                "مسافر سایبری نئونی",
                "Fueled by high-energy retro synths, pulse drives, and reactive bass.",
                "انرژی‌گرفته از سینث‌های نئونی، بیت‌های پرقدرت و بیس‌های تپنده."
            )
            else -> Quadruple(
                "Harmonic Explorer",
                "کاوشگر هارمونیک",
                "An open-minded musical traveler discovering beauty across eclectic frequencies.",
                "جستجوگری کنجکاو در دنیای نواها که زیبایی را در تنوع سبک‌ها کشف می‌کند."
            )
        }

        val streak = calculateStreak(events)
        val daysWithAura = if (firstTimestamp > 0) {
            max(1, ((System.currentTimeMillis() - firstTimestamp) / (1000L * 60 * 60 * 24)).toInt())
        } else 1

        val completionRate = if (periodEvents.isNotEmpty()) {
            val unSkipped = periodEvents.count { !it.wasSkipped }
            (unSkipped.toFloat() / periodEvents.size.toFloat()) * 100f
        } else 100f

        return WrappedStats(
            period = period,
            totalListeningTimeMs = totalListeningTimeMs,
            totalTracksPlayed = periodEvents.size,
            totalSkips = totalSkips,
            listeningStreakDays = streak,
            topSongs = topSongs,
            topArtists = topArtists,
            topAlbums = topAlbums,
            topGenres = topGenres,
            favoriteTimeSlot = primeTimeSlot,
            daysWithAura = daysWithAura,
            audioPersonalityEn = personalityEn,
            audioPersonalityFa = personalityFa,
            audioPersonalityDescEn = descEn,
            audioPersonalityDescFa = descFa,
            completionRate = completionRate
        )
    }

    private fun calculateStreak(events: List<PlaybackEvent>): Int {
        if (events.isEmpty()) return 0
        val cal = Calendar.getInstance()
        val daysList = events.map {
            cal.timeInMillis = it.timestamp
            cal.get(Calendar.YEAR) * 366 + cal.get(Calendar.DAY_OF_YEAR)
        }.distinct().sortedDescending()

        var streak = 1
        for (i in 0 until daysList.size - 1) {
            if (daysList[i] - daysList[i + 1] == 1) {
                streak++
            } else {
                break
            }
        }
        return streak
    }

    private fun getMonthNameEn(month: Int): String = when (month) {
        1 -> "January"
        2 -> "February"
        3 -> "March"
        4 -> "April"
        5 -> "May"
        6 -> "June"
        7 -> "July"
        8 -> "August"
        9 -> "September"
        10 -> "October"
        11 -> "November"
        12 -> "December"
        else -> "Month"
    }

    private fun getMonthNameFa(month: Int): String = when (month) {
        1 -> "ژانویه"
        2 -> "فوریه"
        3 -> "مارس"
        4 -> "آوریل"
        5 -> "مه"
        6 -> "ژوئن"
        7 -> "ژوئیه"
        8 -> "اوت"
        9 -> "سپتامبر"
        10 -> "اکتبر"
        11 -> "نوامبر"
        12 -> "دسامبر"
        else -> "ماه"
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
