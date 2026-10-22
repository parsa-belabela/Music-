package com.example.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.model.*
import java.util.Calendar
import kotlin.math.max

object WrappedEngine {

    private const val PREFS_WRAPPED = "aura_wrapped_alerts"
    private const val CHANNEL_ID = "aura_wrapped_ready_channel"

    data class JalaliDate(val year: Int, val month: Int, val day: Int) {
        val monthNameFa: String get() = getPersianMonthName(month)
        val totalDaysInMonth: Int
            get() = when {
                month in 1..6 -> 31
                month in 7..11 -> 30
                isJalaliLeapYear(year) -> 30
                else -> 29
            }
    }

    private fun getPersianMonthName(month: Int): String = when (month) {
        1 -> "فروردین"
        2 -> "اردیبهشت"
        3 -> "خرداد"
        4 -> "تیر"
        5 -> "مرداد"
        6 -> "شهریور"
        7 -> "مهر"
        8 -> "آبان"
        9 -> "آذر"
        10 -> "دی"
        11 -> "بهمن"
        12 -> "اسفند"
        else -> "ماه"
    }

    private fun getEnglishMonthName(month: Int): String = when (month) {
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

    fun gregorianToJalali(gy: Int, gm: Int, gd: Int): JalaliDate {
        val gDaysInMonth = intArrayOf(0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334)
        val gy2 = if (gm > 2) gy + 1 else gy
        var days = 355666 + (365 * gy) + ((gy2 + 3) / 4) - ((gy2 + 99) / 100) + ((gy2 + 399) / 400) + gd + gDaysInMonth[gm - 1]
        var jy = -1595 + (33 * (days / 12053))
        days %= 12053
        jy += 4 * (days / 1461)
        days %= 1461
        if (days > 365) {
            jy += (days - 1) / 365
            days = (days - 1) % 365
        }
        val jm: Int
        val jd: Int
        if (days < 186) {
            jm = 1 + (days / 31)
            jd = 1 + (days % 31)
        } else {
            jm = 7 + ((days - 186) / 30)
            jd = 1 + ((days - 186) % 30)
        }
        return JalaliDate(jy, jm, jd)
    }

    private fun isJalaliLeapYear(jy: Int): Boolean {
        val rem = jy % 33
        return rem in intArrayOf(1, 5, 9, 13, 17, 22, 26, 30)
    }

    fun toPersianDigits(input: String): String {
        val en = charArrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9')
        val fa = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')
        var out = input
        for (i in en.indices) {
            out = out.replace(en[i], fa[i])
        }
        return out
    }

    /**
     * تولید ۳ دوره اصلی (هفتگی، ماهانه، سالانه) با قفل زمانی دقیق:
     * - هفتگی: قفل تا روز جمعه (برای هر دو زبان فارسی و انگلیسی)
     * - ماهانه: قفل تا پایان آخرین روز ماه
     * - سالانه: قفل تا پایان آخرین روز سال
     */
    fun getAvailablePeriods(firstTimestamp: Long): List<WrappedPeriod> {
        val periods = mutableListOf<WrappedPeriod>()
        val nowCal = Calendar.getInstance()
        val nowMs = nowCal.timeInMillis

        val gYear = nowCal.get(Calendar.YEAR)
        val gMonth = nowCal.get(Calendar.MONTH) + 1
        val gDay = nowCal.get(Calendar.DAY_OF_MONTH)
        val dayOfWeek = nowCal.get(Calendar.DAY_OF_WEEK) // Sunday=1 .. Friday=6, Saturday=7
        val jalaliNow = gregorianToJalali(gYear, gMonth, gDay)

        val dayMs = 24L * 60L * 60L * 1000L

        // ۱. آمار هفتگی: فقط در روز جمعه (Calendar.FRIDAY) باز می‌شود؛ در سایر روزها قفل است
        val isFridayToday = (dayOfWeek == Calendar.FRIDAY)
        val daysUntilFriday = when (dayOfWeek) {
            Calendar.SATURDAY -> 6
            Calendar.SUNDAY -> 5
            Calendar.MONDAY -> 4
            Calendar.TUESDAY -> 3
            Calendar.WEDNESDAY -> 2
            Calendar.THURSDAY -> 1
            else -> 0
        }

        val daysSinceSaturday = if (dayOfWeek == Calendar.SATURDAY) 0 else dayOfWeek
        val weekStartMs = nowMs - (daysSinceSaturday * dayMs)
        val weekEndMs = nowMs + (daysUntilFriday * dayMs)

        periods.add(
            WrappedPeriod(
                id = "WEEKLY_${jalaliNow.year}_${jalaliNow.month}_${jalaliNow.day / 7}",
                displayNameEn = "Weekly Wrapped",
                displayNameFa = "رپد هفتگی",
                isYearly = false,
                year = jalaliNow.year,
                month = jalaliNow.month,
                startTimestamp = weekStartMs,
                endTimestamp = weekEndMs,
                isLocked = !isFridayToday,
                daysRemainingUntilUnlock = daysUntilFriday,
                unlockTargetDateEn = "Unlocks this Friday",
                unlockTargetDateFa = toPersianDigits("بازگشایی در روز جمعه ($daysUntilFriday روز دیگر)"),
                timeframe = WrappedTimeframe.WEEKLY,
                subtitleEn = "7-Day Listening Report",
                subtitleFa = toPersianDigits("گزارش هفتگی • ${jalaliNow.monthNameFa} ${jalaliNow.year}")
            )
        )

        // ۲. آمار ماهانه: تا پایان آخرین روز ماه قفل است
        val daysLeftInJalaliMonth = (jalaliNow.totalDaysInMonth - jalaliNow.day).coerceAtLeast(0)
        val isLastDayOfMonth = daysLeftInJalaliMonth == 0
        val monthStartMs = nowMs - ((jalaliNow.day - 1) * dayMs)
        val monthEndMs = nowMs + (daysLeftInJalaliMonth * dayMs)
        val gMonthName = getEnglishMonthName(gMonth)

        periods.add(
            WrappedPeriod(
                id = "MONTHLY_${jalaliNow.year}_${jalaliNow.month}",
                displayNameEn = "Monthly Wrapped ($gMonthName)",
                displayNameFa = toPersianDigits("رپد ماهانه (${jalaliNow.monthNameFa} ${jalaliNow.year})"),
                isYearly = false,
                year = jalaliNow.year,
                month = jalaliNow.month,
                startTimestamp = monthStartMs,
                endTimestamp = monthEndMs,
                isLocked = !isLastDayOfMonth,
                daysRemainingUntilUnlock = daysLeftInJalaliMonth,
                unlockTargetDateEn = "End of $gMonthName ($daysLeftInJalaliMonth days left)",
                unlockTargetDateFa = toPersianDigits("پایان ${jalaliNow.totalDaysInMonth} ${jalaliNow.monthNameFa} ($daysLeftInJalaliMonth روز مانده)"),
                timeframe = WrappedTimeframe.MONTHLY,
                subtitleEn = "$gMonthName $gYear Recap",
                subtitleFa = toPersianDigits("آمار کامل ${jalaliNow.monthNameFa} ماه ${jalaliNow.year}")
            )
        )

        // ۳. آمار سالانه: تا پایان آخرین روز سال قفل است
        val totalDaysInYear = if (isJalaliLeapYear(jalaliNow.year)) 366 else 365
        val dayOfYearJalali = ((jalaliNow.month - 1) * 31).coerceAtMost(186) +
                ((jalaliNow.month - 7).coerceAtLeast(0) * 30) + jalaliNow.day
        val daysLeftInYear = (totalDaysInYear - dayOfYearJalali).coerceAtLeast(0)
        val isLastDayOfYear = (jalaliNow.month == 12 && jalaliNow.day == jalaliNow.totalDaysInMonth)

        periods.add(
            WrappedPeriod(
                id = "YEARLY_${jalaliNow.year}",
                displayNameEn = "Yearly Wrapped ($gYear)",
                displayNameFa = toPersianDigits("رپد سالانه (${jalaliNow.year})"),
                isYearly = true,
                year = jalaliNow.year,
                month = 0,
                startTimestamp = 0L,
                endTimestamp = nowMs + (daysLeftInYear * dayMs),
                isLocked = !isLastDayOfYear,
                daysRemainingUntilUnlock = daysLeftInYear,
                unlockTargetDateEn = "Last day of the Year ($daysLeftInYear days left)",
                unlockTargetDateFa = toPersianDigits("پایان آخرین روز سال ${jalaliNow.year} ($daysLeftInYear روز مانده)"),
                timeframe = WrappedTimeframe.YEARLY,
                subtitleEn = "Annual Music Recap $gYear",
                subtitleFa = toPersianDigits("کارنامه بزرگ سال ${jalaliNow.year}")
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
        val artistArtworkMap = mutableMapOf<String, String>()
        val albumPlayCount = mutableMapOf<String, Int>()
        val genrePlayCount = mutableMapOf<String, Int>()
        val hourDistribution = IntArray(24)

        val cal = Calendar.getInstance()

        if (periodEvents.isNotEmpty()) {
            for (event in periodEvents) {
                totalListeningTimeMs += event.durationListenedMs
                if (event.wasSkipped) totalSkips++

                trackPlayCount[event.trackId] = (trackPlayCount[event.trackId] ?: 0) + 1
                trackListeningTime[event.trackId] = (trackListeningTime[event.trackId] ?: 0L) + event.durationListenedMs

                val trk = trackMap[event.trackId]
                val cleanArtist = (trk?.artist ?: event.artist).trim()
                if (cleanArtist.isNotBlank() && !cleanArtist.equals("Unknown Artist", ignoreCase = true)) {
                    artistPlayCount[cleanArtist] = (artistPlayCount[cleanArtist] ?: 0) + 1
                    if (!trk?.artworkUri.isNullOrBlank() && !artistArtworkMap.containsKey(cleanArtist)) {
                        artistArtworkMap[cleanArtist] = trk!!.artworkUri!!
                    }
                }

                val cleanAlbum = (trk?.album ?: event.album).trim()
                if (cleanAlbum.isNotBlank() && !cleanAlbum.equals("Unknown Album", ignoreCase = true)) {
                    albumPlayCount[cleanAlbum] = (albumPlayCount[cleanAlbum] ?: 0) + 1
                }

                val resolvedGenre = resolveRealGenreForStats(trk, event.genre)
                genrePlayCount[resolvedGenre] = (genrePlayCount[resolvedGenre] ?: 0) + 1

                cal.timeInMillis = event.timestamp
                val hour = cal.get(Calendar.HOUR_OF_DAY)
                hourDistribution[hour]++
            }
        } else {
            val relevantTracks = allTracks.filter { it.playCount > 0 }.ifEmpty { allTracks.take(10) }
            for (trk in relevantTracks) {
                val plays = trk.playCount.coerceAtLeast(1)
                val dur = trk.durationMs * plays
                totalListeningTimeMs += dur
                trackPlayCount[trk.id] = plays
                trackListeningTime[trk.id] = dur

                if (trk.artist.isNotBlank() && !trk.artist.equals("Unknown Artist", ignoreCase = true)) {
                    artistPlayCount[trk.artist] = (artistPlayCount[trk.artist] ?: 0) + plays
                    if (!trk.artworkUri.isNullOrBlank() && !artistArtworkMap.containsKey(trk.artist)) {
                        artistArtworkMap[trk.artist] = trk.artworkUri
                    }
                }
                if (trk.album.isNotBlank() && !trk.album.equals("Unknown Album", ignoreCase = true)) {
                    albumPlayCount[trk.album] = (albumPlayCount[trk.album] ?: 0) + plays
                }
                val resolvedGenre = resolveRealGenreForStats(trk, trk.genre)
                genrePlayCount[resolvedGenre] = (genrePlayCount[resolvedGenre] ?: 0) + plays

                if (trk.lastPlayedTimestamp > 0L) {
                    cal.timeInMillis = trk.lastPlayedTimestamp
                    val hour = cal.get(Calendar.HOUR_OF_DAY)
                    hourDistribution[hour] += plays
                }
            }
        }

        // Top Songs
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
                    percentage = (entry.value.toFloat() / totalArtistPlays) * 100f,
                    artworkUri = artistArtworkMap[entry.key]
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
            completionRate = completionRate,
            hourlyDistribution = hourDistribution.toList(),
            peakHour = hourDistribution.indices.maxByOrNull { hourDistribution[it] } ?: 22,
            uniqueTracksCount = trackPlayCount.size
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

    fun resolveRealGenreForStats(track: Track?, rawGenre: String? = null): String {
        val genreToTest = rawGenre ?: track?.genre ?: ""
        val titleStr = track?.title ?: ""
        val artistStr = track?.artist ?: ""
        val albumStr = track?.album ?: ""
        val text = "$titleStr $artistStr $albumStr $genreToTest".lowercase()

        return when {
            text.contains("rap") || text.contains("hip hop") || text.contains("hiphop") || text.contains("808") || text.contains("trap") -> {
                if (text.contains("persian") || text.contains("farsi") || text.contains("فارسی") || text.contains("رپ") || text.contains("سورنا") || text.contains("یاس") || text.contains("هیچکس") || text.contains("پیشرو") || text.contains("حصین") || text.contains("شایع") || text.contains("فدائی") || text.contains("کابوس") || text.contains("بهرام") || text.contains("خلوت")) {
                    "Persian Rap / Hip-Hop"
                } else {
                    "Hip-Hop & Rap"
                }
            }
            text.contains("rock") || text.contains("metal") || text.contains("guitar") || text.contains("alternative") || text.contains("punk") -> "Rock & Alternative"
            text.contains("pop") || text.contains("dance") || text.contains("پاپ") -> "Pop & Melodic"
            text.contains("r&b") || text.contains("rnb") || text.contains("soul") || text.contains("funk") -> "R&B & Soul"
            text.contains("chill") || text.contains("ambient") || text.contains("lofi") || text.contains("lo-fi") || text.contains("relax") || text.contains("sleep") -> "Chill & Ambient"
            text.contains("soundtrack") || text.contains("score") || text.contains("ost") || text.contains("instrumental") || text.contains("cinema") -> "Soundtrack & Score"
            text.contains("electro") || text.contains("techno") || text.contains("house") || text.contains("edm") || text.contains("synth") -> "Electronic & Synth"
            else -> "Pop & Melodic"
        }
    }

    fun checkAndNotifyNewWrappedIfNeeded(context: Context, stats: WrappedStats, lang: AppLanguage): Boolean {
        val prefs = context.getSharedPreferences(PREFS_WRAPPED, Context.MODE_PRIVATE)
        val periodId = stats.period.id
        val seenKey = "seen_$periodId"
        val isSeen = prefs.getBoolean(seenKey, false)
        return !isSeen
    }

    fun markWrappedSeenInUi(context: Context, periodId: String) {
        val prefs = context.getSharedPreferences(PREFS_WRAPPED, Context.MODE_PRIVATE)
        prefs.edit().putBoolean("seen_$periodId", true).apply()
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
