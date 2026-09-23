package com.example.data.repository

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    private const val KEY_CUSTOM_STATUS = "custom_status"

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
        // Supporter status either from donation or ad reward
        val isExplicit = prefs(context).getBoolean(KEY_IS_SUPPORTER, false)
        val hasVip = com.example.monetization.EntitlementManager.isVip(context)
        return isExplicit || hasVip
    }

    fun markAsSupporter(context: Context, viaAd: Boolean = false) {
        prefs(context).edit()
            .putBoolean(KEY_IS_SUPPORTER, true)
            .putLong(KEY_SUPPORTER_TIMESTAMP, System.currentTimeMillis())
            .apply()
        // Also grant VIP support status for 48h or permanent
        com.example.monetization.EntitlementManager.grantVip(
            context,
            if (viaAd) 2 else 3650,
            if (viaAd) "ad_reward_support" else "donation_support"
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
                messageFa = "🎧 آهنگ «$trackTitle» در چند روز گذشته بیش از ۱۲ بار تکرار شده! به نظر میرسه دقیقاً حال‌وهوای این روزهاته.",
                messageEn = "You had \"$trackTitle\" on repeat 12+ times! Perfectly matches your mood.",
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
        val totalPlays = prefs(context).getInt(KEY_TOTAL_PLAYS, 18)
        val totalHours = (totalPlays * 3.5f / 60f).coerceAtLeast(0.5f)

        val badges = listOf(
            UserBadge(
                id = "badge_kind_user",
                titleFa = "کاربر مهربون ❤️",
                titleEn = "Kind User ❤️",
                descFa = "حمایت ارزشمند از توسعه برنامه با تماشای تبلیغ یا دونیت",
                descEn = "Supported the developer through donation or reward ad",
                emoji = "❤️",
                isUnlocked = supporter,
                isKindBadge = true
            ),
            UserBadge(
                id = "badge_lovely_user",
                titleFa = "کاربر دوست‌داشتنی ✨",
                titleEn = "Lovely User ✨",
                descFa = "شنیدن بیش از ۲۰ قطعه موسیقی و ساخت پلی‌لیست‌های دلخواه",
                descEn = "Listened to 20+ tracks and created personal playlists",
                emoji = "✨",
                isUnlocked = totalPlays >= 20 || supporter
            ),
            UserBadge(
                id = "badge_loyal_user",
                titleFa = "کاربر وفادار 🌟",
                titleEn = "Loyal User 🌟",
                descFa = "همراهی با برنامه به مدت بیش از ۱ هفته یا ۱ ماه",
                descEn = "Continuous presence in the app for 1+ week",
                emoji = "🌟",
                isUnlocked = joinDays >= 7 || supporter
            ),
            UserBadge(
                id = "badge_music_addict",
                titleFa = "غرق در ملودی 🎧",
                titleEn = "Melody Devotee 🎧",
                descFa = "بیش از ۱۰ ساعت پخش موسیقی بدون وقفه",
                descEn = "Over 10 hours of immersive music streaming",
                emoji = "🎧",
                isUnlocked = totalHours >= 10 || totalPlays >= 30
            ),
            UserBadge(
                id = "badge_night_owl",
                titleFa = "شب‌زنده‌دار موسیقی 🌙",
                titleEn = "Night Owl 🌙",
                descFa = "شنیدن موسیقی در ساعات آرامش‌بخش نیمه‌شب",
                descEn = "Enjoying nocturnal listening sessions",
                emoji = "🌙",
                isUnlocked = true
            )
        )

        val quests = listOf(
            UserQuest(
                id = "quest_weekly_listening",
                titleFa = "شنیدن ۵ روز موسیقی در هفته",
                titleEn = "Listen 5 days this week",
                descFa = "حضور فعال و شنیدن موسیقی در روزهای هفته",
                current = (joinDays % 7).coerceAtLeast(2),
                target = 5,
                emoji = "🎵",
                isCompleted = (joinDays % 7) >= 5
            ),
            UserQuest(
                id = "quest_support_ad",
                titleFa = "حمایت معنوی با تماشای یک تبلیغ",
                titleEn = "Support with 1 short ad",
                descFa = "یک حمایت رایگان ولی بسیار بزرگ برای سازنده",
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
                current = (totalPlays / 3).coerceAtMost(5).coerceAtLeast(3),
                target = 5,
                emoji = "💖",
                isCompleted = totalPlays >= 15
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
}
