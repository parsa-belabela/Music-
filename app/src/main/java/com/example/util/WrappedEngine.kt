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

    fun isJalaliLeapYear(jy: Int): Boolean {
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

        // =========================================================================
        // ۱. رپد هفتگی (قفل تا روز جمعه - فقط در روز جمعه باز میشود)
        // =========================================================================
        val daysSinceSaturday = if (dayOfWeek == Calendar.SATURDAY) 0 else dayOfWeek
        val daysUntilFriday = (6 - daysSinceSaturday).coerceAtLeast(0)
        val isTodayFriday = (dayOfWeek == Calendar.FRIDAY)

        val currentWeekStartCal = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -daysSinceSaturday)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val currentWeekStartMs = currentWeekStartCal.timeInMillis
        val currentWeekEndMs = currentWeekStartMs + (7 * dayMs) - 1L

        val cwStartJalali = gregorianToJalali(
            currentWeekStartCal.get(Calendar.YEAR),
            currentWeekStartCal.get(Calendar.MONTH) + 1,
            currentWeekStartCal.get(Calendar.DAY_OF_MONTH)
        )

        periods.add(
            WrappedPeriod(
                id = "WEEK_CURRENT_${jalaliNow.year}_${jalaliNow.month}_${cwStartJalali.day}",
                displayNameEn = "This Week's Wrapped",
                displayNameFa = toPersianDigits("رپد این هفته (${cwStartJalali.day} ${cwStartJalali.monthNameFa})"),
                isYearly = false,
                year = jalaliNow.year,
                month = jalaliNow.month,
                startTimestamp = currentWeekStartMs,
                endTimestamp = currentWeekEndMs,
                isLocked = !isTodayFriday, // قفل تا فرا رسیدن روز جمعه!
                daysRemainingUntilUnlock = daysUntilFriday.coerceAtLeast(if (isTodayFriday) 0 else 1),
                unlockTargetDateEn = "Unlocks this Friday",
                unlockTargetDateFa = "باز شدن قفل: روز جمعه پایان هفته",
                timeframe = WrappedTimeframe.WEEKLY,
                subtitleEn = if (isTodayFriday) "Unlocked • Weekly Story Ready" else "Locked until Friday",
                subtitleFa = if (isTodayFriday) "باز شده • آماده تماشا" else toPersianDigits("${daysUntilFriday} روز مانده تا جمعه")
            )
        )

        // هفته گذشته (چون جمعهاش گذشته، باز و قابل مشاهده است)
        val prevWeekStartMs = currentWeekStartMs - (7 * dayMs)
        val prevWeekEndMs = currentWeekStartMs - 1L
        val pwCal = Calendar.getInstance().apply { timeInMillis = prevWeekStartMs }
        val pwJalali = gregorianToJalali(
            pwCal.get(Calendar.YEAR),
            pwCal.get(Calendar.MONTH) + 1,
            pwCal.get(Calendar.DAY_OF_MONTH)
        )

        periods.add(
            WrappedPeriod(
                id = "WEEK_PREV_${pwJalali.year}_${pwJalali.month}_${pwJalali.day}",
                displayNameEn = "Last Week Wrapped",
                displayNameFa = toPersianDigits("هفته گذشته (${pwJalali.day} ${pwJalali.monthNameFa})"),
                isYearly = false,
                year = pwJalali.year,
                month = pwJalali.month,
                startTimestamp = prevWeekStartMs,
                endTimestamp = prevWeekEndMs,
                isLocked = false,
                daysRemainingUntilUnlock = 0,
                unlockTargetDateEn = "Completed Friday",
                unlockTargetDateFa = "تکمیلشده در جمعه گذشته",
                timeframe = WrappedTimeframe.WEEKLY,
                subtitleEn = "Completed 7-Day Story",
                subtitleFa = toPersianDigits("آمار کامل هفته گذشته • ${pwJalali.monthNameFa}")
            )
        )

        // =========================================================================
        // ۲. رپد ماهانه (قفل تا روز آخر ماه)
        // =========================================================================
        val daysLeftInJalaliMonth = (jalaliNow.totalDaysInMonth - jalaliNow.day).coerceAtLeast(0)
        val isLastDayOfMonth = (daysLeftInJalaliMonth == 0)

        val currentMonthStartMs = nowMs - ((jalaliNow.day - 1) * dayMs)
        val currentMonthEndMs = currentMonthStartMs + (jalaliNow.totalDaysInMonth * dayMs) - 1L
        val gMonthName = getEnglishMonthName(gMonth)

        periods.add(
            WrappedPeriod(
                id = "MONTH_CURRENT_${jalaliNow.year}_${jalaliNow.month}",
                displayNameEn = "$gMonthName $gYear",
                displayNameFa = toPersianDigits("${jalaliNow.monthNameFa} ${jalaliNow.year}"),
                isYearly = false,
                year = jalaliNow.year,
                month = jalaliNow.month,
                startTimestamp = currentMonthStartMs,
                endTimestamp = currentMonthEndMs,
                isLocked = !isLastDayOfMonth, // قفل تا روز آخر ماه!
                daysRemainingUntilUnlock = daysLeftInJalaliMonth.coerceAtLeast(if (isLastDayOfMonth) 0 else 1),
                unlockTargetDateEn = "Last day of $gMonthName",
                unlockTargetDateFa = toPersianDigits("روز آخر ${jalaliNow.monthNameFa} (${jalaliNow.totalDaysInMonth} ${jalaliNow.monthNameFa})"),
                timeframe = WrappedTimeframe.MONTHLY,
                subtitleEn = if (isLastDayOfMonth) "Unlocked Monthly Story" else "Locked until end of month",
                subtitleFa = if (isLastDayOfMonth) "باز شده • پایان ماه" else toPersianDigits("${daysLeftInJalaliMonth} روز تا پایان ${jalaliNow.monthNameFa}")
            )
        )

        // ماه گذشته (تکمیلشده و باز)
        val prevJMonth = if (jalaliNow.month > 1) jalaliNow.month - 1 else 12
        val prevJYear = if (jalaliNow.month > 1) jalaliNow.year else jalaliNow.year - 1
        val prevJMonthDays = if (prevJMonth <= 6) 31 else if (prevJMonth <= 11) 30 else 29
        val prevMonthEndMs = currentMonthStartMs - 1L
        val prevMonthStartMs = prevMonthEndMs - (prevJMonthDays * dayMs) + 1L
        val prevGMonth = if (gMonth > 1) gMonth - 1 else 12
        val prevGYear = if (gMonth > 1) gYear else gYear - 1

        periods.add(
            WrappedPeriod(
                id = "MONTH_PREV_${prevJYear}_${prevJMonth}",
                displayNameEn = "${getEnglishMonthName(prevGMonth)} $prevGYear",
                displayNameFa = toPersianDigits("${getPersianMonthName(prevJMonth)} ${prevJYear}"),
                isYearly = false,
                year = prevJYear,
                month = prevJMonth,
                startTimestamp = prevMonthStartMs,
                endTimestamp = prevMonthEndMs,
                isLocked = false,
                daysRemainingUntilUnlock = 0,
                unlockTargetDateEn = "Completed Month",
                unlockTargetDateFa = "ماه تکمیلشده",
                timeframe = WrappedTimeframe.MONTHLY,
                subtitleEn = "Full Monthly Wrapped",
                subtitleFa = toPersianDigits("گزارش کامل ${getPersianMonthName(prevJMonth)} ${prevJYear}")
            )
        )

        // =========================================================================
        // ۳. رپد سالانه (قفل تا روز آخر سال)
        // =========================================================================
        val totalDaysInJalaliYear = if (isJalaliLeapYear(jalaliNow.year)) 366 else 365
        val dayOfYearJalali = if (jalaliNow.month <= 6) {
            (jalaliNow.month - 1) * 31 + jalaliNow.day
        } else {
            186 + (jalaliNow.month - 7) * 30 + jalaliNow.day
        }
        val daysLeftInYear = (totalDaysInJalaliYear - dayOfYearJalali).coerceAtLeast(0)
        val isLastDayOfYear = (daysLeftInYear == 0)

        val yearStartMs = nowMs - ((dayOfYearJalali - 1) * dayMs)
        val yearEndMs = yearStartMs + (totalDaysInJalaliYear * dayMs) - 1L

        periods.add(
            WrappedPeriod(
                id = "YEAR_CURRENT_${jalaliNow.year}",
                displayNameEn = "$gYear Annual Wrapped",
                displayNameFa = toPersianDigits("رپد سالانه ${jalaliNow.year}"),
                isYearly = true,
                year = jalaliNow.year,
                month = 0,
                startTimestamp = yearStartMs,
                endTimestamp = yearEndMs,
                isLocked = !isLastDayOfYear, // قفل تا روز آخر سال!
                daysRemainingUntilUnlock = daysLeftInYear.coerceAtLeast(if (isLastDayOfYear) 0 else 1),
                unlockTargetDateEn = "Last day of the year (Dec 31)",
                unlockTargetDateFa = toPersianDigits("روز آخر سال (اسفند ${jalaliNow.year})"),
                timeframe = WrappedTimeframe.YEARLY,
                subtitleEn = if (isLastDayOfYear) "Annual Wrapped Unlocked!" else "$daysLeftInYear days until Year-End Unlock",
                subtitleFa = if (isLastDayOfYear) "باز شده • پایان سال" else toPersianDigits("${daysLeftInYear} روز مانده تا پایان سال ${jalaliNow.year}")
            )
        )

        return periods
    }

    /**
     * محاسبه آمار بر اساس «دقیقه واقعی گوش دادن» (Minutes Listened) و نادیده گرفتن آهنگهای اسکیپشده کوتاه
     */
    fun computeStats(
        period: WrappedPeriod,
        events: List<PlaybackEvent>,
        allTracks: List<Track>,
        firstTimestamp: Long
    ): WrappedStats {
        val rawPeriodEvents = events.filter { it.timestamp in period.startTimestamp..period.endTimestamp }
        val trackMap = allTracks.associateBy { it.id }

        var totalSkips = 0
        // فیلتر کردن آهنگهایی که کاربر سریع رد (Skip) کرده تا در آمار آهنگهای گوشدادهشده حساب نشوند
        val validListenEvents = rawPeriodEvents.filter { ev ->
            val minThreshold = minOf(25_000L, (ev.fullTrackDurationMs * 0.35f).toLong().coerceAtLeast(12_000L))
            if (ev.wasSkipped && ev.durationListenedMs < minThreshold) {
                totalSkips++
                false
            } else {
                true
            }
        }

        var totalListeningTimeMs = 0L
        val trackListeningTime = mutableMapOf<String, Long>()
        val trackPlayCount = mutableMapOf<String, Int>()
        val artistListeningTime = mutableMapOf<String, Long>()
        val artistPlayCount = mutableMapOf<String, Int>()
        val artistArtworkMap = mutableMapOf<String, String>()
        val albumListeningTime = mutableMapOf<String, Long>()
        val albumPlayCount = mutableMapOf<String, Int>()
        val genreListeningTime = mutableMapOf<String, Long>()
        val genrePlayCount = mutableMapOf<String, Int>()
        val hourDistribution = IntArray(24)

        val cal = Calendar.getInstance()

        if (validListenEvents.isNotEmpty()) {
            for (event in validListenEvents) {
                val listenedMs = event.durationListenedMs.coerceAtLeast(1000L)
                totalListeningTimeMs += listenedMs

                trackListeningTime[event.trackId] = (trackListeningTime[event.trackId] ?: 0L) + listenedMs
                trackPlayCount[event.trackId] = (trackPlayCount[event.trackId] ?: 0) + 1

                val trk = trackMap[event.trackId]
                val cleanArtist = (trk?.artist ?: event.artist).trim()
                if (cleanArtist.isNotBlank() && !cleanArtist.equals("Unknown Artist", ignoreCase = true)) {
                    artistListeningTime[cleanArtist] = (artistListeningTime[cleanArtist] ?: 0L) + listenedMs
                    artistPlayCount[cleanArtist] = (artistPlayCount[cleanArtist] ?: 0) + 1
                    if (!trk?.artworkUri.isNullOrBlank() && !artistArtworkMap.containsKey(cleanArtist)) {
                        artistArtworkMap[cleanArtist] = trk!!.artworkUri!!
                    }
                }

                val cleanAlbum = (trk?.album ?: event.album).trim()
                if (cleanAlbum.isNotBlank() && !cleanAlbum.equals("Unknown Album", ignoreCase = true)) {
                    albumListeningTime[cleanAlbum] = (albumListeningTime[cleanAlbum] ?: 0L) + listenedMs
                    albumPlayCount[cleanAlbum] = (albumPlayCount[cleanAlbum] ?: 0) + 1
                }

                val resolvedGenre = resolveRealGenreForStats(trk, event.genre)
                genreListeningTime[resolvedGenre] = (genreListeningTime[resolvedGenre] ?: 0L) + listenedMs
                genrePlayCount[resolvedGenre] = (genrePlayCount[resolvedGenre] ?: 0) + 1

                cal.timeInMillis = event.timestamp
                val hour = cal.get(Calendar.HOUR_OF_DAY)
                hourDistribution[hour] += (listenedMs / 60_000L).toInt().coerceAtLeast(1)
            }
        } else {
            val playedTracks = allTracks.filter { it.playCount > 0 }
            for (trk in playedTracks) {
                val plays = trk.playCount
                val listenedMs = trk.durationMs * plays
                totalListeningTimeMs += listenedMs
                trackListeningTime[trk.id] = listenedMs
                trackPlayCount[trk.id] = plays

                if (trk.artist.isNotBlank() && !trk.artist.equals("Unknown Artist", ignoreCase = true)) {
                    artistListeningTime[trk.artist] = (artistListeningTime[trk.artist] ?: 0L) + listenedMs
                    artistPlayCount[trk.artist] = (artistPlayCount[trk.artist] ?: 0) + plays
                    if (!trk.artworkUri.isNullOrBlank() && !artistArtworkMap.containsKey(trk.artist)) {
                        artistArtworkMap[trk.artist] = trk.artworkUri
                    }
                }
                if (trk.album.isNotBlank() && !trk.album.equals("Unknown Album", ignoreCase = true)) {
                    albumListeningTime[trk.album] = (albumListeningTime[trk.album] ?: 0L) + listenedMs
                    albumPlayCount[trk.album] = (albumPlayCount[trk.album] ?: 0) + plays
                }
                val resolvedGenre = resolveRealGenreForStats(trk, trk.genre)
                genreListeningTime[resolvedGenre] = (genreListeningTime[resolvedGenre] ?: 0L) + listenedMs
                genrePlayCount[resolvedGenre] = (genrePlayCount[resolvedGenre] ?: 0) + plays
                hourDistribution[21] += (listenedMs / 60_000L).toInt().coerceAtLeast(1)
            }
        }

        // رتبهبندی ۵ آهنگ برتر دقیقاً بر اساس «دقیقه گوش دادن» (totalListeningTimeMs)
        val topSongs = trackListeningTime.entries
            .sortedByDescending { it.value }
            .take(5)
            .mapIndexed { index, entry ->
                val track = trackMap[entry.key]
                val matchingEvent = validListenEvents.firstOrNull { it.trackId == entry.key }
                TopSongItem(
                    trackId = entry.key,
                    title = track?.title ?: matchingEvent?.trackTitle ?: "Track",
                    artist = track?.artist ?: matchingEvent?.artist ?: "Artist",
                    album = track?.album ?: matchingEvent?.album ?: "Album",
                    artworkUri = track?.artworkUri,
                    playCount = trackPlayCount[entry.key] ?: 1,
                    totalDurationMs = entry.value,
                    rank = index + 1
                )
            }

        // رتبهبندی هنرمندان برتر بر اساس مجموع دقایق گوش دادن
        val totalArtistMs = artistListeningTime.values.sum().coerceAtLeast(1L)
        val topArtists = artistListeningTime.entries
            .sortedByDescending { it.value }
            .take(5)
            .map { entry ->
                val mins = (entry.value / 60_000L).coerceAtLeast(1L)
                TopStatItem(
                    name = entry.key,
                    count = artistPlayCount[entry.key] ?: 1,
                    formattedDuration = "$mins min",
                    percentage = ((entry.value.toFloat() / totalArtistMs.toFloat()) * 100f).coerceIn(5f, 100f),
                    artworkUri = artistArtworkMap[entry.key]
                )
            }

        val topAlbums = albumListeningTime.entries
            .sortedByDescending { it.value }
            .take(5)
            .map { entry ->
                val mins = (entry.value / 60_000L).coerceAtLeast(1L)
                TopStatItem(
                    name = entry.key,
                    count = albumPlayCount[entry.key] ?: 1,
                    formattedDuration = "$mins min"
                )
            }

        val totalGenreMs = genreListeningTime.values.sum().coerceAtLeast(1L)
        val topGenres = genreListeningTime.entries
            .sortedByDescending { it.value }
            .take(5)
            .map { entry ->
                val mins = (entry.value / 60_000L).coerceAtLeast(1L)
                TopStatItem(
                    name = entry.key,
                    count = genrePlayCount[entry.key] ?: 1,
                    formattedDuration = "$mins min",
                    percentage = ((entry.value.toFloat() / totalGenreMs.toFloat()) * 100f).coerceIn(5f, 100f)
                )
            }

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

        val streak = calculateStreak(validListenEvents)
        val daysWithAura = if (firstTimestamp > 0) {
            max(1, ((System.currentTimeMillis() - firstTimestamp) / (1000L * 60 * 60 * 24)).toInt())
        } else 1

        val completionRate = if (rawPeriodEvents.isNotEmpty()) {
            val unSkipped = rawPeriodEvents.count { !it.wasSkipped }
            (unSkipped.toFloat() / rawPeriodEvents.size.toFloat()) * 100f
        } else 100f

        return WrappedStats(
            period = period,
            totalListeningTimeMs = totalListeningTimeMs,
            totalTracksPlayed = validListenEvents.size,
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
            peakHour = hourDistribution.indices.maxByOrNull { hourDistribution[it] } ?: 21,
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
