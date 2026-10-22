package com.example.data.repository

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import java.util.concurrent.TimeUnit

data class UserBadge(
    val id: String,
    val titleFa: String,
    val titleEn: String,
    val descFa: String,
    val descEn: String,
    val emoji: String,
    val isUnlocked: Boolean,
    val isKindBadge: Boolean = false
)

data class UserQuest(
    val id: String,
    val titleFa: String,
    val titleEn: String,
    val descFa: String,
    val current: Int,
    val target: Int,
    val emoji: String,
    val isCompleted: Boolean
)

data class FunMusicInsight(
    val trackTitle: String,
    val artistName: String,
    val messageFa: String,
    val messageEn: String,
    val badgeEmoji: String,
    val statHighlight: String
)

data class UserProfileData(
    val name: String,
    val avatarEmoji: String,
    val joinDays: Int,
    val isSupporter: Boolean,
    val supporterTitle: String,
    val totalTracksPlayed: Int,
    val totalListeningHours: Float,
    val badges: List<UserBadge>,
    val quests: List<UserQuest>,
    val currentInsight: FunMusicInsight?
)

object UserProfileManager {
    private const val PREFS_NAME = "aura_user_profile"
    private const val KEY_USER_NAME = "user_name"
    private const val KEY_AVATAR = "user_avatar"
    private const val KEY_FIRST_LAUNCH = "first_launch_time"
    private const val KEY_IS_SUPPORTER = "is_supporter"
    private const val KEY_SUPPORTER_TIMESTAMP = "supporter_timestamp"
    private const val KEY_TOTAL_PLAYS = "total_plays_count"
    private const val KEY_TOTAL_DONATED = "total_donated_toman"

    private val _profileFlow = MutableStateFlow<UserProfileData?>(null)
    val profileFlow: StateFlow<UserProfileData?> = _profileFlow.asStateFlow()

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun init(context: Context) {
        val p = prefs(context)
        if (!p.contains(KEY_FIRST_LAUNCH)) {
            p.edit().putLong(KEY_FIRST_LAUNCH, System.currentTimeMillis()).apply()
        }
        refreshProfile(context)
    }

    fun getUserName(context: Context): String {
        return prefs(context).getString(KEY_USER_NAME, "شنونده موسیقی") ?: "شنونده موسیقی"
    }

    fun setUserName(context: Context, name: String) {
        prefs(context).edit().putString(KEY_USER_NAME, name.trim()).apply()
        refreshProfile(context)
    }

    fun getAvatarEmoji(context: Context): String {
        return prefs(context).getString(KEY_AVATAR, "🎧") ?: "🎧"
    }

    fun setAvatarEmoji(context: Context, emoji: String) {
        prefs(context).edit().putString(KEY_AVATAR, emoji).apply()
        refreshProfile(context)
    }

    fun isSupporter(context: Context): Boolean {
        return true
    }

    fun markAsSupporter(context: Context, viaAd: Boolean = false) {
        prefs(context).edit()
            .putBoolean(KEY_IS_SUPPORTER, true)
            .putLong(KEY_SUPPORTER_TIMESTAMP, System.currentTimeMillis())
            .apply()
        com.example.monetization.EntitlementManager.grantVip(
            context,
            3650,
            "full_access"
        )
        refreshProfile(context)
    }

    fun incrementTrackPlay(context: Context) {
        val count = prefs(context).getInt(KEY_TOTAL_PLAYS, 0) + 1
        prefs(context).edit().putInt(KEY_TOTAL_PLAYS, count).apply()
        refreshProfile(context)
    }

    fun getJoinDays(context: Context): Int {
        val firstLaunch = prefs(context).getLong(KEY_FIRST_LAUNCH, System.currentTimeMillis())
        val diffMs = System.currentTimeMillis() - firstLaunch
        val days = TimeUnit.MILLISECONDS.toDays(diffMs).toInt()
        return (days + 1).coerceAtLeast(1)
    }

    fun generateFunInsight(trackTitle: String, artistName: String, hoursListened: Float = 8f): FunMusicInsight {
        val insights = listOf(
            FunMusicInsight(
                trackTitle = trackTitle,
                artistName = artistName,
                messageFa = "💡 میدونستی این هفته بیش از ${hoursListened.toInt().coerceAtLeast(1)} ساعت غرق در این آهنگ بودی؟ انگار حسابی باهاش خاطره داری!",
                messageEn = "You spent over ${hoursListened.toInt().coerceAtLeast(1)} hours with this track this week! A true personal anthem.",
                badgeEmoji = "❤️",
                statHighlight = "آهنگ خاطره‌انگیز هفته"
            ),
            FunMusicInsight(
                trackTitle = trackTitle,
                artistName = artistName,
                messageFa = "🎧 آهنگ «$trackTitle» در چند روز گذشته بارها تکرار شده! به نظر میرسه دقیقاً حال‌وهوای این روزهاته.",
                messageEn = "You had \"$trackTitle\" on repeat! Perfectly matches your mood.",
                badgeEmoji = "🔥",
                statHighlight = "پرتکرارترین قطعه"
            ),
            FunMusicInsight(
                trackTitle = trackTitle,
                artistName = artistName,
                messageFa = "🌙 رکورددار شنیدن در نیمه‌شب: این آهنگ بیش از همه در سکوت ساعت ۲ تا ۴ صبح همراهت بوده!",
                messageEn = "Night Owl Favorite: Most played between 2 AM and 4 AM!",
                badgeEmoji = "🌌",
                statHighlight = "همدم شبانه"
            ),
            FunMusicInsight(
                trackTitle = trackTitle,
                artistName = artistName,
                messageFa = "✨ شما جزو شنوندگان خاص و بااحساس آثار $artistName در برنامه هستید!",
                messageEn = "Top Listener: Deep appreciation for $artistName's soundscapes.",
                badgeEmoji = "👑",
                statHighlight = "شنونده وفادار"
            )
        )
        return insights.random()
    }

    private fun createMilestoneBadges(
        totalPlays: Int,
        totalHours: Float,
        nightPlaysCount: Int,
        favoritesCount: Int,
        joinDays: Int
    ): List<UserBadge> {
        return listOf(
            UserBadge(
                id = "badge_track_1",
                titleFa = "نخستین نوا 🎵",
                titleEn = "First Melody 🎵",
                descFa = "شنیدن اولین قطعه موسیقی در آئورا",
                descEn = "Listened to your first track in Aura",
                emoji = "🎵",
                isUnlocked = totalPlays >= 1
            ),
            UserBadge(
                id = "badge_track_50",
                titleFa = "همراه آهنگ‌ها 🎶",
                titleEn = "Melody Explorer 🎶",
                descFa = "شنیدن بیش از ۵۰ قطعه موسیقی",
                descEn = "Listened to over 50 tracks",
                emoji = "🎶",
                isUnlocked = totalPlays >= 50
            ),
            UserBadge(
                id = "badge_track_200",
                titleFa = "شیفته موسیقی 🎼",
                titleEn = "Music Devotee 🎼",
                descFa = "شنیدن بیش از ۲۰۰ قطعه موسیقی",
                descEn = "Listened to over 200 tracks",
                emoji = "🎼",
                isUnlocked = totalPlays >= 200
            ),
            UserBadge(
                id = "badge_track_500",
                titleFa = "استاد ریتم 🎻",
                titleEn = "Rhythm Maestro 🎻",
                descFa = "شنیدن بیش از ۵۰۰ قطعه موسیقی",
                descEn = "Listened to over 500 tracks",
                emoji = "🎻",
                isUnlocked = totalPlays >= 500
            ),
            UserBadge(
                id = "badge_track_1000",
                titleFa = "افسانه ۱۰۰۰ نوا 👑",
                titleEn = "1,000 Melodies Legend 👑",
                descFa = "ثبت رکورد شگفت‌انگیز شنیدن ۱۰۰۰ قطعه موسیقی",
                descEn = "Reached an astounding milestone of 1,000 tracks",
                emoji = "👑",
                isUnlocked = totalPlays >= 1000
            ),
            UserBadge(
                id = "badge_hour_1",
                titleFa = "ساعت نخست ⏱️",
                titleEn = "First Hour ⏱️",
                descFa = "۱ ساعت غرق در دنیای موسیقی",
                descEn = "1 full hour spent enjoying music",
                emoji = "⏱️",
                isUnlocked = totalHours >= 1f
            ),
            UserBadge(
                id = "badge_hour_10",
                titleFa = "۱۰ ساعت هم‌نوایی ⏳",
                titleEn = "10 Hours Symphony ⏳",
                descFa = "سپری کردن بیش از ۱۰ ساعت با قطعات دلخواه",
                descEn = "Over 10 hours of music journey",
                emoji = "⏳",
                isUnlocked = totalHours >= 10f
            ),
            UserBadge(
                id = "badge_hour_50",
                titleFa = "۵۰ ساعت زندگی با صدا 🌌",
                titleEn = "50 Hours Milestone 🌌",
                descFa = "بیش از ۵۰ ساعت پخش مداوم صدا و موسیقی",
                descEn = "Over 50 hours of audio immersion",
                emoji = "🌌",
                isUnlocked = totalHours >= 50f
            ),
            UserBadge(
                id = "badge_hour_100",
                titleFa = "۱۰۰ ساعت سمفونی بی‌پایان 🪐",
                titleEn = "100 Hours Symphony 🪐",
                descFa = "دستیابی به رکورد کم‌نظیر ۱۰۰ ساعت شنیدن موسیقی",
                descEn = "Achieved the legendary 100-hour listening milestone",
                emoji = "🪐",
                isUnlocked = totalHours >= 100f
            ),
            UserBadge(
                id = "badge_night_owl",
                titleFa = "همدم نیمه‌شب 🌙",
                titleEn = "Night Owl 🌙",
                descFa = "شنیدن موسیقی در سکوت و آرامش نیمه‌شب (۱۲ شب تا ۶ صبح)",
                descEn = "Listened to music during peaceful midnight hours",
                emoji = "🌙",
                isUnlocked = nightPlaysCount > 0
            ),
            UserBadge(
                id = "badge_fav_10",
                titleFa = "گزینش‌گر ملودی 💖",
                titleEn = "Melody Curator 💖",
                descFa = "افزودن ۱۰ قطعه به لیست علاقه‌مندی‌ها",
                descEn = "Added 10 tracks to Favorites",
                emoji = "💖",
                isUnlocked = favoritesCount >= 10
            ),
            UserBadge(
                id = "badge_fav_25",
                titleFa = "گنجینه طلایی 💎",
                titleEn = "Golden Treasury 💎",
                descFa = "افزودن ۲۵ قطعه به گنجینه علاقه‌مندی‌ها",
                descEn = "Curated a treasury of 25 favorite tracks",
                emoji = "💎",
                isUnlocked = favoritesCount >= 25
            ),
            UserBadge(
                id = "badge_active_7d",
                titleFa = "همراهی هفتگی 🌟",
                titleEn = "Weekly Companion 🌟",
                descFa = "بیش از ۱ هفته همراهی مداوم با برنامه",
                descEn = "Over 1 week of companionship with Aura",
                emoji = "🌟",
                isUnlocked = joinDays >= 7
            ),
            UserBadge(
                id = "badge_active_30d",
                titleFa = "همسفر صمیمی 🏆",
                titleEn = "Monthly Loyal 🏆",
                descFa = "بیش از ۳۰ روز حضور و همراهی با موسیقی",
                descEn = "Over 30 days of active music listening",
                emoji = "🏆",
                isUnlocked = joinDays >= 30
            )
        )
    }

    fun refreshProfile(context: Context) {
        val joinDays = getJoinDays(context)
        val supporter = true
        val totalPlays = prefs(context).getInt(KEY_TOTAL_PLAYS, 0)
        val totalHours = (totalPlays * 3.5f / 60f)

        val badges = createMilestoneBadges(
            totalPlays = totalPlays,
            totalHours = totalHours,
            nightPlaysCount = 0,
            favoritesCount = 0,
            joinDays = joinDays
        )

        val supporterTitle = if (supporter) "شنونده ویژه ✨" else "شنونده موسیقی"

        _profileFlow.value = UserProfileData(
            name = getUserName(context),
            avatarEmoji = getAvatarEmoji(context),
            joinDays = joinDays,
            isSupporter = supporter,
            supporterTitle = supporterTitle,
            totalTracksPlayed = totalPlays,
            totalListeningHours = totalHours,
            badges = badges,
            quests = emptyList(),
            currentInsight = null
        )
    }

    suspend fun refreshProfileWithDatabase(
        context: Context,
        repository: MusicRepository
    ) {
        val joinDays = getJoinDays(context)
        val supporter = isSupporter(context)
        val allEvents = repository.getAllPlaybackEvents()
        val favorites = repository.favoriteTracks.firstOrNull() ?: emptyList()

        val totalPlays = allEvents.size.coerceAtLeast(prefs(context).getInt(KEY_TOTAL_PLAYS, 0))
        val totalMs = allEvents.sumOf { it.durationListenedMs }
        val totalHours = (totalMs / 3600000f)

        val nightPlaysCount = allEvents.count { ev ->
            val cal = java.util.Calendar.getInstance().apply { timeInMillis = ev.timestamp }
            cal.get(java.util.Calendar.HOUR_OF_DAY) in 0..5
        }

        val favoritesCount = favorites.size

        val badges = createMilestoneBadges(
            totalPlays = totalPlays,
            totalHours = totalHours,
            nightPlaysCount = nightPlaysCount,
            favoritesCount = favoritesCount,
            joinDays = joinDays
        )

        val realInsight: FunMusicInsight = if (allEvents.isEmpty()) {
            FunMusicInsight(
                trackTitle = "",
                artistName = "",
                messageFa = "هنوز آمار پخشی در دیتابیس ثبت نشده است. با پخش آهنگ‌های محبوبتان، خاطرات و فکت‌های موسیقی واقعی شما در اینجا شکل می‌گیرند ✨",
                messageEn = "No play history recorded in database yet. Play tracks to create real music memories!",
                badgeEmoji = "🎧",
                statHighlight = "دفترچه خاطرات موسیقی شما"
            )
        } else {
            val topGroup = allEvents.groupBy { it.trackId }.maxByOrNull { it.value.size }
            val topEv = topGroup?.value?.firstOrNull()
            val trackTitle = topEv?.trackTitle ?: "آهنگ منتخب"
            val artistName = topEv?.artist ?: "هنرمند"
            val playCount = topGroup?.value?.size ?: 1
            val listenedMs = topGroup?.value?.sumOf { it.durationListenedMs } ?: 0L
            val listenedMins = (listenedMs / 60000L).toInt()
            val trackNightCount = topGroup?.value?.count { ev ->
                val cal = java.util.Calendar.getInstance().apply { timeInMillis = ev.timestamp }
                cal.get(java.util.Calendar.HOUR_OF_DAY) in 0..5
            } ?: 0

            if (trackNightCount > 0) {
                FunMusicInsight(
                    trackTitle = trackTitle,
                    artistName = artistName,
                    messageFa = "🌙 همدم شبانه: قطعه «$trackTitle» اثر $artistName تاکنون $trackNightCount بار در ساعات سکوت و آرامش نیمه‌شب (۱۲ شب تا ۶ صبح) همراه شما بوده است.",
                    messageEn = "Night Owl Favorite: \"$trackTitle\" by $artistName was played $trackNightCount times during late hours!",
                    badgeEmoji = "🌌",
                    statHighlight = "همدم شبانه شما"
                )
            } else if (listenedMins >= 1) {
                FunMusicInsight(
                    trackTitle = trackTitle,
                    artistName = artistName,
                    messageFa = "💡 شما مجموعاً $listenedMins دقیقه ($playCount بار) غرق در شنیدن قطعه «$trackTitle» از $artistName بوده‌اید!",
                    messageEn = "You have spent $listenedMins minutes ($playCount plays) listening to \"$trackTitle\" by $artistName!",
                    badgeEmoji = "❤️",
                    statHighlight = "آهنگ خاطره‌انگیز شما"
                )
            } else {
                FunMusicInsight(
                    trackTitle = trackTitle,
                    artistName = artistName,
                    messageFa = "🎧 قطعه «$trackTitle» اثر $artistName با $playCount بار پخش ثبت‌شده در دیتابیس، بیشترین تکرار را در کارنامه شما داشته است.",
                    messageEn = "Your top track is \"$trackTitle\" by $artistName with $playCount plays recorded!",
                    badgeEmoji = "🔥",
                    statHighlight = "پرتکرارترین قطعه"
                )
            }
        }

        val supporterTitle = if (supporter) "شنونده ویژه ✨" else "شنونده موسیقی"

        _profileFlow.value = UserProfileData(
            name = getUserName(context),
            avatarEmoji = getAvatarEmoji(context),
            joinDays = joinDays,
            isSupporter = supporter,
            supporterTitle = supporterTitle,
            totalTracksPlayed = totalPlays,
            totalListeningHours = totalHours,
            badges = badges,
            quests = emptyList(),
            currentInsight = realInsight
        )
    }
}
