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
        return prefs(context).getString(KEY_USER_NAME, "کاربر مهربون و موزیک‌باز") ?: "کاربر مهربون و موزیک‌باز"
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
        return getTotalDonatedToman(context) > 0 || prefs(context).getBoolean(KEY_IS_SUPPORTER, false)
    }

    fun markAsSupporter(context: Context, viaAd: Boolean = false) {
        prefs(context).edit()
            .putBoolean(KEY_IS_SUPPORTER, true)
            .putLong(KEY_SUPPORTER_TIMESTAMP, System.currentTimeMillis())
            .apply()
        com.example.monetization.EntitlementManager.grantVip(
            context,
            3650,
            "donation_support"
        )
        refreshProfile(context)
    }

    fun getTotalDonatedToman(context: Context): Int {
        return prefs(context).getInt(KEY_TOTAL_DONATED, 0)
    }

    /**
     * Records a real financial donation. Anyone who donates >= 100,000 Toman receives the secret
     * "Developer's Guardian Angel 💙" achievement!
     */
    fun recordDonation(context: Context, amountToman: Int): Boolean {
        val p = prefs(context)
        val newTotal = p.getInt(KEY_TOTAL_DONATED, 0) + amountToman
        val unlockedAngel = newTotal >= 100_000
        p.edit()
            .putInt(KEY_TOTAL_DONATED, newTotal)
            .putBoolean(KEY_IS_SUPPORTER, true)
            .putBoolean("secret_dev_badge_unlocked", unlockedAngel)
            .putLong(KEY_SUPPORTER_TIMESTAMP, System.currentTimeMillis())
            .apply()

        refreshProfile(context)
        return unlockedAngel
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

    fun refreshProfile(context: Context) {
        val joinDays = getJoinDays(context)
        val supporter = isSupporter(context)
        val totalPlays = prefs(context).getInt(KEY_TOTAL_PLAYS, 0)
        val totalHours = (totalPlays * 3.5f / 60f)
        val totalDonated = getTotalDonatedToman(context)
        val hasDonated = totalDonated > 0
        val isGuardianAngelUnlocked = totalDonated >= 100_000

        val badges = listOf(
            UserBadge(
                id = "badge_kind_user",
                titleFa = "کاربر مهربون ❤️",
                titleEn = "Kind User ❤️",
                descFa = "حمایت ارزشمند از توسعه برنامه با دونیت و همراهی (آنلاک با دونیت)",
                descEn = "Supported the developer through donation",
                emoji = "❤️",
                isUnlocked = hasDonated,
                isKindBadge = true
            ),
            UserBadge(
                id = "badge_secret_dev_gratitude",
                titleFa = "فرشته نجات سازنده 💙",
                titleEn = "Developer's Guardian Angel 💙",
                descFa = "از طرف برنامه‌نویس: واقعاً ازت ممنونم. خیلی بهم کمک کردی. اینو از ته قلبم میگم. درسته نمیشناسمت اما این کمکت خیلی کمک بزرگی بود به من و آینده و زندگیم:)💙 (آنلاک‌شده با حمایت بالای ۱۰۰ هزار تومان)",
                descEn = "From developer: Thank you so much! Your support means the world to me and my future:)💙 (Unlocked via 100k+ Toman donation)",
                emoji = "💙",
                isUnlocked = isGuardianAngelUnlocked,
                isKindBadge = true
            ),
            UserBadge(
                id = "badge_lovely_user",
                titleFa = "کاربر دوست‌داشتنی ✨",
                titleEn = "Lovely User ✨",
                descFa = "شنیدن بیش از ۲۰ قطعه موسیقی",
                descEn = "Listened to 20+ tracks",
                emoji = "✨",
                isUnlocked = totalPlays >= 20
            ),
            UserBadge(
                id = "badge_loyal_user",
                titleFa = "کاربر وفادار 🌟",
                titleEn = "Loyal User 🌟",
                descFa = "همراهی با برنامه به مدت بیش از ۱ هفته",
                descEn = "Continuous presence in the app for 1+ week",
                emoji = "🌟",
                isUnlocked = joinDays >= 7
            ),
            UserBadge(
                id = "badge_music_addict",
                titleFa = "غرق در ملودی 🎧",
                titleEn = "Melody Devotee 🎧",
                descFa = "بیش از ۱۰ ساعت پخش موسیقی",
                descEn = "Over 10 hours of music streaming",
                emoji = "🎧",
                isUnlocked = totalHours >= 10f || totalPlays >= 100
            ),
            UserBadge(
                id = "badge_night_owl",
                titleFa = "شب‌زنده‌دار موسیقی 🌙",
                titleEn = "Night Owl 🌙",
                descFa = "شنیدن موسیقی در ساعات آرامش‌بخش نیمه‌شب (۱۲ شب تا ۶ صبح)",
                descEn = "Enjoying music during midnight hours",
                emoji = "🌙",
                isUnlocked = false
            )
        )

        val quests = listOf(
            UserQuest(
                id = "quest_weekly_listening",
                titleFa = "شنیدن ۵ روز موسیقی در هفته",
                titleEn = "Listen 5 days this week",
                descFa = "حضور فعال و شنیدن موسیقی در روزهای هفته",
                current = joinDays.coerceAtMost(5),
                target = 5,
                emoji = "🎵",
                isCompleted = joinDays >= 5
            ),
            UserQuest(
                id = "quest_support_dev",
                titleFa = "حمایت معنوی یا مالی از سازنده",
                titleEn = "Support the Developer",
                descFa = "یک حمایت شیرین برای دلگرمی و ارتقای برنامه",
                current = if (supporter) 1 else 0,
                target = 1,
                emoji = "🎁",
                isCompleted = supporter
            ),
            UserQuest(
                id = "quest_favorite_songs",
                titleFa = "افزودن ۵ آهنگ به علاقه‌مندی‌ها",
                titleEn = "Favorite 5 tracks",
                descFa = "قلب زدن روی آهنگ‌های خاطره‌انگیز",
                current = 0,
                target = 5,
                emoji = "💖",
                isCompleted = false
            )
        )

        val supporterTitle = if (supporter) "حامی ویژه و کاربر مهربون ✨" else "شنونده مشتاق موسیقی"

        _profileFlow.value = UserProfileData(
            name = getUserName(context),
            avatarEmoji = getAvatarEmoji(context),
            joinDays = joinDays,
            isSupporter = supporter,
            supporterTitle = supporterTitle,
            totalTracksPlayed = totalPlays,
            totalListeningHours = totalHours,
            badges = badges,
            quests = quests,
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
        val totalDonated = getTotalDonatedToman(context)

        val nightPlaysCount = allEvents.count { ev ->
            val cal = java.util.Calendar.getInstance().apply { timeInMillis = ev.timestamp }
            cal.get(java.util.Calendar.HOUR_OF_DAY) in 0..5
        }

        val nowMs = System.currentTimeMillis()
        val activeDaysThisWeek = allEvents
            .filter { (nowMs - it.timestamp) <= 7 * 24 * 3600 * 1000L }
            .map { ev ->
                java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.US).format(java.util.Date(ev.timestamp))
            }
            .distinct()
            .size

        val favoritesCount = favorites.size
        val hasDonated = totalDonated > 0
        val isGuardianAngelUnlocked = totalDonated >= 100_000

        val badges = listOf(
            UserBadge(
                id = "badge_kind_user",
                titleFa = "کاربر مهربون ❤️",
                titleEn = "Kind User ❤️",
                descFa = "حمایت ارزشمند از توسعه برنامه با دونیت و همراهی (آنلاک با دونیت)",
                descEn = "Supported the developer through donation",
                emoji = "❤️",
                isUnlocked = hasDonated,
                isKindBadge = true
            ),
            UserBadge(
                id = "badge_secret_dev_gratitude",
                titleFa = "فرشته نجات سازنده 💙",
                titleEn = "Developer's Guardian Angel 💙",
                descFa = "از طرف برنامه‌نویس: واقعاً ازت ممنونم. خیلی بهم کمک کردی. اینو از ته قلبم میگم. درسته نمیشناسمت اما این کمکت خیلی کمک بزرگی بود به من و آینده و زندگیم:)💙 (آنلاک‌شده با حمایت بالای ۱۰۰ هزار تومان)",
                descEn = "From developer: Thank you so much! Your support means the world to me and my future:)💙 (Unlocked via 100k+ Toman donation)",
                emoji = "💙",
                isUnlocked = isGuardianAngelUnlocked,
                isKindBadge = true
            ),
            UserBadge(
                id = "badge_lovely_user",
                titleFa = "کاربر دوست‌داشتنی ✨",
                titleEn = "Lovely User ✨",
                descFa = "شنیدن بیش از ۲۰ قطعه موسیقی",
                descEn = "Listened to 20+ tracks",
                emoji = "✨",
                isUnlocked = totalPlays >= 20
            ),
            UserBadge(
                id = "badge_loyal_user",
                titleFa = "کاربر وفادار 🌟",
                titleEn = "Loyal User 🌟",
                descFa = "همراهی با برنامه به مدت بیش از ۱ هفته",
                descEn = "Continuous presence in the app for 1+ week",
                emoji = "🌟",
                isUnlocked = joinDays >= 7
            ),
            UserBadge(
                id = "badge_music_addict",
                titleFa = "غرق در ملودی 🎧",
                titleEn = "Melody Devotee 🎧",
                descFa = "بیش از ۱۰ ساعت پخش موسیقی",
                descEn = "Over 10 hours of music streaming",
                emoji = "🎧",
                isUnlocked = totalHours >= 10f || totalPlays >= 100
            ),
            UserBadge(
                id = "badge_night_owl",
                titleFa = "شب‌زنده‌دار موسیقی 🌙",
                titleEn = "Night Owl 🌙",
                descFa = "شنیدن موسیقی در ساعات آرامش‌بخش نیمه‌شب (۱۲ شب تا ۶ صبح)",
                descEn = "Enjoying nocturnal listening sessions",
                emoji = "🌙",
                isUnlocked = nightPlaysCount > 0
            )
        )

        val quests = listOf(
            UserQuest(
                id = "quest_weekly_listening",
                titleFa = "شنیدن ۵ روز موسیقی در هفته",
                titleEn = "Listen 5 days this week",
                descFa = "حضور فعال و شنیدن موسیقی در روزهای هفته",
                current = activeDaysThisWeek.coerceAtMost(5),
                target = 5,
                emoji = "🎵",
                isCompleted = activeDaysThisWeek >= 5
            ),
            UserQuest(
                id = "quest_support_dev",
                titleFa = "حمایت معنوی یا مالی از سازنده",
                titleEn = "Support the Developer",
                descFa = "یک حمایت شیرین برای دلگرمی و ارتقای برنامه",
                current = if (supporter) 1 else 0,
                target = 1,
                emoji = "🎁",
                isCompleted = supporter
            ),
            UserQuest(
                id = "quest_favorite_songs",
                titleFa = "افزودن ۵ آهنگ به علاقه‌مندی‌ها",
                titleEn = "Favorite 5 tracks",
                descFa = "قلب زدن روی آهنگ‌های خاطره‌انگیز",
                current = favoritesCount.coerceAtMost(5),
                target = 5,
                emoji = "💖",
                isCompleted = favoritesCount >= 5
            )
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

        val supporterTitle = if (supporter) "حامی ویژه و کاربر مهربون ✨" else "شنونده مشتاق موسیقی"

        _profileFlow.value = UserProfileData(
            name = getUserName(context),
            avatarEmoji = getAvatarEmoji(context),
            joinDays = joinDays,
            isSupporter = supporter,
            supporterTitle = supporterTitle,
            totalTracksPlayed = totalPlays,
            totalListeningHours = totalHours,
            badges = badges,
            quests = quests,
            currentInsight = realInsight
        )
    }
}
