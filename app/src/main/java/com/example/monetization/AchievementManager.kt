package com.example.monetization

import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.ui.graphics.vector.ImageVector

data class AchievementItem(
    val id: String,
    val titleEn: String,
    val titleFa: String,
    val descEn: String,
    val descFa: String,
    val target: Long,
    val current: Long,
    val isUnlocked: Boolean,
    val vipRewardHours: Int = 0,
    val isRewardClaimed: Boolean = false
)

object AchievementManager {
    private const val PREFS_NAME = "aura_achievements"
    private const val KEY_UNLOCKED = "unlocked_achievements"
    private const val KEY_CLAIMED_REWARDS = "claimed_rewards"

    fun getAchievements(
        context: Context,
        totalListenedMs: Long,
        uniqueTracksCount: Int,
        activeStreakDays: Int
    ): List<AchievementItem> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val unlockedSet = prefs.getStringSet(KEY_UNLOCKED, emptySet()) ?: emptySet()
        val claimedSet = prefs.getStringSet(KEY_CLAIMED_REWARDS, emptySet()) ?: emptySet()

        val totalHours = totalListenedMs / (3600 * 1000L)

        val list = mutableListOf<AchievementItem>()

        // 1. First Step (10 Hours) -> 24h VIP
        val ach1Unlocked = unlockedSet.contains("ach_10h") || totalHours >= 10
        list.add(
            AchievementItem(
                id = "ach_10h",
                titleEn = "Acoustic Journey",
                titleFa = "سفر آکوستیک (۱۰ ساعت)",
                descEn = "Listen to music for 10 hours in Aura",
                descFa = "۱۰ ساعت گوش دادن به موسیقی در پلیر آئورا",
                target = 10,
                current = totalHours.coerceAtMost(10),
                isUnlocked = ach1Unlocked,
                vipRewardHours = 24,
                isRewardClaimed = claimedSet.contains("ach_10h")
            )
        )

        // 2. 7 Days Streak -> 24h VIP
        val ach2Unlocked = unlockedSet.contains("ach_streak_7") || activeStreakDays >= 7
        list.add(
            AchievementItem(
                id = "ach_streak_7",
                titleEn = "Rhythm Habit",
                titleFa = "عادت ریتمیک (۷ روز پیاپی)",
                descEn = "Listen to music 7 consecutive days",
                descFa = "۷ روز متوالی غوطه‌ور شدن در موسیقی",
                target = 7,
                current = activeStreakDays.toLong().coerceAtMost(7),
                isUnlocked = ach2Unlocked,
                vipRewardHours = 24,
                isRewardClaimed = claimedSet.contains("ach_streak_7")
            )
        )

        // 3. 50 Unique Tracks Discovered -> 24h VIP
        val ach3Unlocked = unlockedSet.contains("ach_tracks_50") || uniqueTracksCount >= 50
        list.add(
            AchievementItem(
                id = "ach_tracks_50",
                titleEn = "Sound Explorer",
                titleFa = "کاشف اصوات (۵۰ قطعه)",
                descEn = "Explore and listen to 50 distinct tracks",
                descFa = "کشف و شنیدن ۵۰ قطعه موسیقی مجزا",
                target = 50,
                current = uniqueTracksCount.toLong().coerceAtMost(50),
                isUnlocked = ach3Unlocked,
                vipRewardHours = 24,
                isRewardClaimed = claimedSet.contains("ach_tracks_50")
            )
        )

        // 4. 100 Hours of Playback -> 48h VIP
        val ach4Unlocked = unlockedSet.contains("ach_100h") || totalHours >= 100
        list.add(
            AchievementItem(
                id = "ach_100h",
                titleEn = "Master Audiophile",
                titleFa = "استاد آدیوفیل (۱۰۰ ساعت)",
                descEn = "Listen to 100 hours of pure music",
                descFa = "۱۰۰ ساعت تجربه شنیداری ناب",
                target = 100,
                current = totalHours.coerceAtMost(100),
                isUnlocked = ach4Unlocked,
                vipRewardHours = 48,
                isRewardClaimed = claimedSet.contains("ach_100h")
            )
        )

        // Sync newly unlocked achievements to prefs
        val newlyUnlocked = list.filter { it.isUnlocked && !unlockedSet.contains(it.id) }.map { it.id }
        if (newlyUnlocked.isNotEmpty()) {
            val updated = unlockedSet.toMutableSet().apply { addAll(newlyUnlocked) }
            prefs.edit().putStringSet(KEY_UNLOCKED, updated).apply()
        }

        return list
    }

    /**
     * Claims VIP preview reward granted by a loyalty achievement.
     */
    fun claimReward(context: Context, achievementId: String): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val claimedSet = prefs.getStringSet(KEY_CLAIMED_REWARDS, emptySet()) ?: emptySet()
        if (claimedSet.contains(achievementId)) return 0

        val rewardHours = when (achievementId) {
            "ach_10h", "ach_streak_7", "ach_tracks_50" -> 24
            "ach_100h" -> 48
            else -> 0
        }

        if (rewardHours > 0) {
            EntitlementManager.unlockFullVipPreview(context, rewardHours)
            val updated = claimedSet.toMutableSet().apply { add(achievementId) }
            prefs.edit().putStringSet(KEY_CLAIMED_REWARDS, updated).apply()
        }
        return rewardHours
    }
}
