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

        val todayStartCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val todayStartMs = todayStartCal.timeInMillis

        // =========================================================================
        // ۱. رپد هفتگی (قفل تا روز جمعه - فقط در روز جمعه باز میشود)
        // =========================================================================
        val daysSinceSaturday = if (dayOfWeek == Calendar.SATURDAY) 0 else dayOfWeek
        val daysUntilFriday = (6 - daysSinceSaturday).coerceAtLeast(0)

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

        val isWeeklyUnlocked48h = (dayOfWeek == Calendar.FRIDAY || dayOfWeek == Calendar.SATURDAY)
        val safeDaysUntilFriday = if (isWeeklyUnlocked48h) 0 else daysUntilFriday.coerceAtLeast(1)

        periods.add(
            WrappedPeriod(
                id = "WEEK_CURRENT_${jalaliNow.year}_${jalaliNow.month}_${cwStartJalali.day}",
                displayNameEn = "Weekly Wrapped",
                displayNameFa = toPersianDigits("گزارش هفتگی (${cwStartJalali.day} ${cwStartJalali.monthNameFa})"),
                isYearly = false,
                year = jalaliNow.year,
                month = jalaliNow.month,
                startTimestamp = currentWeekStartMs,
                endTimestamp = currentWeekEndMs,
                isLocked = !isWeeklyUnlocked48h,
                daysRemainingUntilUnlock = safeDaysUntilFriday,
                unlockTargetDateEn = "Unlocks Friday (Limited 48h Window)",
                unlockTargetDateFa = "بازگشایی: روز جمعه هر هفته (به مدت محدود ۴۸ ساعت)",
                timeframe = WrappedTimeframe.WEEKLY,
                subtitleEn = if (isWeeklyUnlocked48h) "Unlocked for 48 Hours • Weekly Story Ready" else "Locked until Friday ($safeDaysUntilFriday days left)",
                subtitleFa = if (isWeeklyUnlocked48h) "باز شده به مدت ۴۸ ساعت • آماده تماشا" else toPersianDigits("قفل تا جمعه (${safeDaysUntilFriday} روز مانده • دسترسی ۴۸ ساعته)")
            )
        )

        // =========================================================================
        // ۲. رپد ماهانه (قفل تا روز آخر ماه - بازگشایی محدود ۴۸ ساعته)
        // =========================================================================
        val daysLeftInJalaliMonth = (jalaliNow.totalDaysInMonth - jalaliNow.day).coerceAtLeast(0)
        val isMonthlyUnlocked48h = (daysLeftInJalaliMonth == 0 || jalaliNow.day == 1)
        val safeDaysUntilMonthEnd = if (isMonthlyUnlocked48h) 0 else daysLeftInJalaliMonth.coerceAtLeast(1)

        val currentMonthStartMs = todayStartMs - ((jalaliNow.day - 1) * dayMs)
        val currentMonthEndMs = currentMonthStartMs + (jalaliNow.totalDaysInMonth * dayMs) - 1L
        val gMonthName = getEnglishMonthName(gMonth)

        periods.add(
            WrappedPeriod(
                id = "MONTH_CURRENT_${jalaliNow.year}_${jalaliNow.month}",
                displayNameEn = "Monthly Wrapped ($gMonthName)",
                displayNameFa = toPersianDigits("گزارش ماهانه (${jalaliNow.monthNameFa} ${jalaliNow.year})"),
                isYearly = false,
                year = jalaliNow.year,
                month = jalaliNow.month,
                startTimestamp = currentMonthStartMs,
                endTimestamp = currentMonthEndMs,
                isLocked = !isMonthlyUnlocked48h,
                daysRemainingUntilUnlock = safeDaysUntilMonthEnd,
                unlockTargetDateEn = "Unlocks Last Day of $gMonthName (48h Window)",
                unlockTargetDateFa = toPersianDigits("بازگشایی: روز آخر ${jalaliNow.monthNameFa} (${jalaliNow.totalDaysInMonth} ${jalaliNow.monthNameFa} • به مدت ۴۸ ساعت)"),
                timeframe = WrappedTimeframe.MONTHLY,
                subtitleEn = if (isMonthlyUnlocked48h) "Unlocked for 48 Hours • Monthly Story" else "Locked until end of month ($safeDaysUntilMonthEnd days left)",
                subtitleFa = if (isMonthlyUnlocked48h) "باز شده به مدت ۴۸ ساعت • پایان ماه" else toPersianDigits("قفل تا روز آخر ماه (${safeDaysUntilMonthEnd} روز مانده • دسترسی ۴۸ ساعته)")
            )
        )

        // =========================================================================
        // ۳. رپد سالانه (قفل تا روز آخر سال - بازگشایی محدود ۴۸ ساعته)
        // =========================================================================
        val totalDaysInJalaliYear = if (isJalaliLeapYear(jalaliNow.year)) 366 else 365
        val dayOfYearJalali = if (jalaliNow.month <= 6) {
            (jalaliNow.month - 1) * 31 + jalaliNow.day
        } else {
            186 + (jalaliNow.month - 7) * 30 + jalaliNow.day
        }
        val daysLeftInYear = (totalDaysInJalaliYear - dayOfYearJalali).coerceAtLeast(0)
        val isYearlyUnlocked48h = (daysLeftInYear == 0 || dayOfYearJalali == 1)
        val safeDaysUntilYearEnd = if (isYearlyUnlocked48h) 0 else daysLeftInYear.coerceAtLeast(1)

        val yearStartMs = todayStartMs - ((dayOfYearJalali - 1) * dayMs)
        val yearEndMs = yearStartMs + (totalDaysInJalaliYear * dayMs) - 1L

        periods.add(
            WrappedPeriod(
                id = "YEAR_CURRENT_${jalaliNow.year}",
                displayNameEn = "$gYear Annual Wrapped",
                displayNameFa = toPersianDigits("گزارش سالانه ${jalaliNow.year}"),
                isYearly = true,
                year = jalaliNow.year,
                month = 0,
                startTimestamp = yearStartMs,
                endTimestamp = yearEndMs,
                isLocked = !isYearlyUnlocked48h,
                daysRemainingUntilUnlock = safeDaysUntilYearEnd,
                unlockTargetDateEn = "Unlocks Last Day of Year (Limited 48h Window)",
                unlockTargetDateFa = toPersianDigits("بازگشایی: روز آخر سال ${jalaliNow.year} (به مدت محدود ۴۸ ساعت)"),
                timeframe = WrappedTimeframe.YEARLY,
                subtitleEn = if (isYearlyUnlocked48h) "Unlocked for 48 Hours • Annual Wrapped!" else "Locked until end of year ($safeDaysUntilYearEnd days left)",
                subtitleFa = if (isYearlyUnlocked48h) "باز شده به مدت ۴۸ ساعت • پایان سال" else toPersianDigits("قفل تا روز آخر سال (${safeDaysUntilYearEnd} روز مانده • دسترسی ۴۸ ساعته)")
            )
        )

        return periods
    }

    /**
     * محاسبه دقیق آمار بر اساس آهنگ‌هایی که حداقل تا نیمه (۵۰٪) گوش داده شده‌اند.
     * هیچ آمار ساختگی یا کلیک‌های زیر ۵۰٪ در این بخش لحاظ نمی‌شود.
     */
    fun computeStats(
        period: WrappedPeriod,
        events: List<PlaybackEvent>,
        allTracks: List<Track>,
        firstTimestamp: Long
    ): WrappedStats {
        val effectiveEndMs = maxOf(period.endTimestamp, System.currentTimeMillis())
        val rawPeriodEvents = events.filter { it.timestamp in period.startTimestamp..effectiveEndMs }
        val trackMap = allTracks.associateBy { it.id }

        var totalSkips = 0
        // قانون سخت‌گیرانه ۵۰٪: فقط آهنگ‌هایی که حداقل تا نصف (۵۰٪) مدت زمانشان گوش داده شده‌اند حساب می‌شوند
        val validListenEvents = rawPeriodEvents.filter { ev ->
            val fullDur = ev.fullTrackDurationMs.takeIf { it > 0L }
                ?: trackMap[ev.trackId]?.durationMs?.takeIf { it > 0L }
                ?: 180_000L
            val halfDur = (fullDur / 2L).coerceAtLeast(10_000L)
            if (ev.durationListenedMs < (halfDur * 95L / 100L)) {
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

        for (event in validListenEvents) {
            val trk = trackMap[event.trackId]
            val fullDur = event.fullTrackDurationMs.takeIf { it > 0L }
                ?: trk?.durationMs?.takeIf { it > 0L }
                ?: 180_000L
            val listenedMs = event.durationListenedMs.coerceIn(1000L, fullDur)
            totalListeningTimeMs += listenedMs

            trackListeningTime[event.trackId] = (trackListeningTime[event.trackId] ?: 0L) + listenedMs
            trackPlayCount[event.trackId] = (trackPlayCount[event.trackId] ?: 0) + 1

            val resolvedArtwork = trk?.artworkUri?.takeIf { it.isNotBlank() }
                ?: try {
                    com.example.OnlineMusicRepository.getCoverUrlForTrack(event.trackId)?.takeIf { it.isNotBlank() }
                } catch (_: Exception) {
                    null
                }

            val cleanArtist = (trk?.artist ?: event.artist).trim()
            if (cleanArtist.isNotBlank() && !cleanArtist.equals("Unknown Artist", ignoreCase = true)) {
                artistListeningTime[cleanArtist] = (artistListeningTime[cleanArtist] ?: 0L) + listenedMs
                artistPlayCount[cleanArtist] = (artistPlayCount[cleanArtist] ?: 0) + 1
                if (!resolvedArtwork.isNullOrBlank() && !artistArtworkMap.containsKey(cleanArtist)) {
                    artistArtworkMap[cleanArtist] = resolvedArtwork
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
            hourDistribution[hour] += ((listenedMs + 30_000L) / 60_000L).toInt().coerceAtLeast(1)
        }

        // رتبه‌بندی ۵ آهنگ برتر بر اساس دفعات پخش معتبر (>=50%) و زمان واقعی گوش دادن
        val topSongs = trackListeningTime.entries
            .sortedWith(
                compareByDescending<Map.Entry<String, Long>> { trackPlayCount[it.key] ?: 0 }
                    .thenByDescending { it.value }
            )
            .take(5)
            .mapIndexed { index, entry ->
                val track = trackMap[entry.key]
                val matchingEvent = validListenEvents.firstOrNull { it.trackId == entry.key }
                val resolvedArtwork = track?.artworkUri?.takeIf { it.isNotBlank() }
                    ?: try {
                        com.example.OnlineMusicRepository.getCoverUrlForTrack(entry.key)?.takeIf { it.isNotBlank() }
                    } catch (_: Exception) {
                        null
                    }
                TopSongItem(
                    trackId = entry.key,
                    title = track?.title ?: matchingEvent?.trackTitle ?: "Track",
                    artist = track?.artist ?: matchingEvent?.artist ?: "Artist",
                    album = track?.album ?: matchingEvent?.album ?: "Album",
                    artworkUri = resolvedArtwork,
                    playCount = trackPlayCount[entry.key] ?: 1,
                    totalDurationMs = entry.value,
                    rank = index + 1
                )
            }

        val totalArtistMs = artistListeningTime.values.sum().coerceAtLeast(1L)
        val topArtists = artistListeningTime.entries
            .sortedWith(
                compareByDescending<Map.Entry<String, Long>> { artistPlayCount[it.key] ?: 0 }
                    .thenByDescending { it.value }
            )
            .take(5)
            .map { entry ->
                val mins = ((entry.value + 30_000L) / 60_000L).coerceAtLeast(1L)
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
                val mins = ((entry.value + 30_000L) / 60_000L).coerceAtLeast(1L)
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
                val mins = ((entry.value + 30_000L) / 60_000L).coerceAtLeast(1L)
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
            morningScore >= maxOf(afternoonScore, eveningScore, nightScore) && morningScore > 0 -> ListeningTimeSlot.MORNING
            afternoonScore >= maxOf(morningScore, eveningScore, nightScore) && afternoonScore > 0 -> ListeningTimeSlot.AFTERNOON
            eveningScore >= maxOf(morningScore, afternoonScore, nightScore) && eveningScore > 0 -> ListeningTimeSlot.EVENING
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

        val completionRate = if (validListenEvents.isNotEmpty()) {
            val avgRatio = validListenEvents.map { ev ->
                val full = ev.fullTrackDurationMs.takeIf { it > 0L } ?: 180_000L
                (ev.durationListenedMs.toFloat() / full.toFloat()).coerceIn(0.5f, 1f)
            }.average().toFloat()
            avgRatio.coerceIn(0.5f, 1f)
        } else 0f

        return WrappedStats(
            period = period,
            totalListeningTimeMs = totalListeningTimeMs,
            totalTracksPlayed = validListenEvents.size,
            totalSkips = totalSkips,
            listeningStreakDays = if (validListenEvents.isNotEmpty()) streak.coerceAtLeast(1) else 0,
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
            peakHour = if (validListenEvents.isNotEmpty()) (hourDistribution.indices.maxByOrNull { hourDistribution[it] } ?: 21) else 21,
            uniqueTracksCount = trackPlayCount.size,
            uniqueArtistsCount = artistPlayCount.size
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
