package com.example.monetization

import android.content.Context
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object EntitlementManager {
    private const val PREFS_NAME = "aura_entitlements"
    private const val KEY_VIP_EXPIRY = "vip_expiry_millis"       // Long.MAX_VALUE = Permanent
    private const val KEY_VIP_SOURCE = "vip_source"              // "purchase" | "promo_code" | "gift" | "none"
    private const val KEY_TEMP_UNLOCKS = "temp_unlocks"          // "featureId:expiryMillis,featureId:expiryMillis"
    private const val KEY_DAILY_ADS_COUNT = "daily_ads_count"
    private const val KEY_DAILY_ADS_DAY = "daily_ads_day"
    const val MAX_DAILY_ADS = 3

    // Precomputed SHA-256 hash of "5758pp91"
    // Hashing prevents plain-text extraction from decompiled APK assets/strings.
    private val VALID_PROMO_HASHES = setOf(
        "a7a0772e06bc51397c4f1b91093fd1389ee778cfb57950f5830c654dc4582c69"
    )

    val ALL_PREMIUM_FEATURE_IDS = listOf(
        "theme_velvet_noir",
        "theme_sunset_rave",
        "theme_digital_acid",
        "theme_y2k_chrome",
        "theme_monochrome_noir",
        "now_playing_vinyl",
        "visualizer_circular_spectrum",
        "visualizer_waveform",
        "visualizer_radial_wave",
        "visualizer_pulse_ring",
        "visualizer_aurora",
        "visualizer_liquid",
        "visualizer_particle_field",
        "visualizer_dots",
        "visualizer_cinematic_fog",
        "personal_hearing_profile",
        "continuous_mix",
        "share_card_no_watermark"
    )

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isVip(context: Context): Boolean {
        val expiry = prefs(context).getLong(KEY_VIP_EXPIRY, 0L)
        return expiry > System.currentTimeMillis()
    }

    fun getVipSource(context: Context): String {
        return prefs(context).getString(KEY_VIP_SOURCE, "none") ?: "none"
    }

    fun getVipExpiryMillis(context: Context): Long {
        return prefs(context).getLong(KEY_VIP_EXPIRY, 0L)
    }

    fun grantVip(context: Context, days: Int, source: String) {
        val currentExpiry = prefs(context).getLong(KEY_VIP_EXPIRY, 0L)
        val baseTime = if (currentExpiry > System.currentTimeMillis()) currentExpiry else System.currentTimeMillis()
        val expiry = if (days <= 0) Long.MAX_VALUE
        else baseTime + (days * 24L * 60L * 60L * 1000L)

        prefs(context).edit()
            .putLong(KEY_VIP_EXPIRY, expiry)
            .putString(KEY_VIP_SOURCE, source)
            .commit()
    }

    fun revokeVip(context: Context) {
        prefs(context).edit()
            .remove(KEY_VIP_EXPIRY)
            .putString(KEY_VIP_SOURCE, "none")
            .remove(KEY_TEMP_UNLOCKS)
            .commit()
    }

    /**
     * Validates promo code using SHA-256 hash comparison.
     * Matches give 365 days of VIP status.
     */
    fun redeemPromoCode(context: Context, rawCode: String): Boolean {
        val normalized = rawCode.trim().lowercase(Locale.ROOT)
        val hash = sha256(normalized)
        return if (hash in VALID_PROMO_HASHES) {
            grantVip(context, days = 365, source = "promo_code")
            true
        } else {
            false
        }
    }

    fun unlockFeatureTemporarily(context: Context, featureId: String, hours: Int) {
        val current = readTempUnlocks(context).toMutableMap()
        val expiry = System.currentTimeMillis() + (hours * 60L * 60L * 1000L)
        current[featureId] = expiry
        writeTempUnlocks(context, current)
    }

    /**
     * Unlocks the entire VIP catalog for the given number of hours (default 24h).
     */
    fun unlockFullVipPreview(context: Context, hours: Int = 24) {
        val current = readTempUnlocks(context).toMutableMap()
        val expiry = System.currentTimeMillis() + (hours * 60L * 60L * 1000L)
        ALL_PREMIUM_FEATURE_IDS.forEach { id ->
            current[id] = expiry
        }
        writeTempUnlocks(context, current)
    }

    fun hasAccess(context: Context, featureId: String): Boolean {
        if (isVip(context)) return true
        val expiry = readTempUnlocks(context)[featureId] ?: return false
        return expiry > System.currentTimeMillis()
    }

    fun getFeatureRemainingTimeMillis(context: Context, featureId: String): Long {
        if (isVip(context)) return Long.MAX_VALUE
        val expiry = readTempUnlocks(context)[featureId] ?: return 0L
        return (expiry - System.currentTimeMillis()).coerceAtLeast(0L)
    }

    // Daily Rewarded Ad Cap (Max 3 per day)
    fun getRemainingDailyAds(context: Context): Int {
        val currentDay = getTodayDayOfYear()
        val savedDay = prefs(context).getInt(KEY_DAILY_ADS_DAY, -1)
        if (savedDay != currentDay) {
            return MAX_DAILY_ADS
        }
        val count = prefs(context).getInt(KEY_DAILY_ADS_COUNT, 0)
        return (MAX_DAILY_ADS - count).coerceIn(0, MAX_DAILY_ADS)
    }

    fun canWatchDailyAd(context: Context): Boolean {
        return getRemainingDailyAds(context) > 0
    }

    fun recordAdWatched(context: Context): Boolean {
        val currentDay = getTodayDayOfYear()
        val savedDay = prefs(context).getInt(KEY_DAILY_ADS_DAY, -1)
        var count = if (savedDay != currentDay) 0 else prefs(context).getInt(KEY_DAILY_ADS_COUNT, 0)
        if (count >= MAX_DAILY_ADS) return false

        count++
        prefs(context).edit()
            .putInt(KEY_DAILY_ADS_DAY, currentDay)
            .putInt(KEY_DAILY_ADS_COUNT, count)
            .commit()
        return true
    }

    private fun getTodayDayOfYear(): Int {
        val cal = Calendar.getInstance()
        return cal.get(Calendar.YEAR) * 1000 + cal.get(Calendar.DAY_OF_YEAR)
    }

    private fun readTempUnlocks(context: Context): Map<String, Long> {
        val raw = prefs(context).getString(KEY_TEMP_UNLOCKS, null) ?: return emptyMap()
        if (raw.isBlank()) return emptyMap()
        val now = System.currentTimeMillis()
        val result = mutableMapOf<String, Long>()

        raw.split(",").forEach { entry ->
            val parts = entry.split(":")
            if (parts.size == 2) {
                val id = parts[0].trim()
                val exp = parts[1].toLongOrNull() ?: 0L
                if (exp > now) {
                    result[id] = exp
                }
            }
        }
        return result
    }

    private fun writeTempUnlocks(context: Context, map: Map<String, Long>) {
        val now = System.currentTimeMillis()
        val serialized = map.filter { it.value > now }
            .map { "${it.key}:${it.value}" }
            .joinToString(",")
        prefs(context).edit().putString(KEY_TEMP_UNLOCKS, serialized).commit()
    }

    fun sha256(input: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(input.toByteArray())
            .joinToString("") { "%02x".format(it) }

    fun getFeatureTitle(featureId: String, isPersian: Boolean): String = when (featureId) {
        "theme_velvet_noir" -> if (isPersian) "تم ولوت نوآر سلطنتی" else "Royal Velvet Noir Theme"
        "theme_sunset_rave" -> if (isPersian) "تم غروب نئونی تابناک" else "Sunset Rave Theme"
        "theme_digital_acid" -> if (isPersian) "تم ماتریکس اسید فسفری" else "Digital Acid Matrix Theme"
        "theme_y2k_chrome" -> if (isPersian) "تم کروم مایع نقره‌ای" else "Liquid Chrome Theme"
        "theme_monochrome_noir" -> if (isPersian) "تم سیاه و سفید کریستالی" else "Monochrome Obsidian Theme"
        "now_playing_vinyl" -> if (isPersian) "استایل صفحه گرامافون (وینیل)" else "Vinyl Turntable Player"
        "personal_hearing_profile" -> if (isPersian) "پروفایل کالیبراسیون شنوایی" else "Personal Hearing Calibration"
        "continuous_mix" -> if (isPersian) "میکس پیوسته دی‌جی (Continuous Mix)" else "Continuous DJ Mix"
        "share_card_no_watermark" -> if (isPersian) "حذف واترمارک کارت اشتراک" else "No Watermark Share Cards"
        else -> if (featureId.startsWith("visualizer_")) {
            val name = featureId.removePrefix("visualizer_").replace("_", " ").capitalizeWords()
            if (isPersian) "ویژوالایزر پیشرفته $name" else "Advanced $name Visualizer"
        } else {
            if (isPersian) "ویژگی ویژه VIP" else "VIP Feature"
        }
    }

    fun formatRemainingTime(millis: Long, isPersian: Boolean): String {
        if (millis <= 0) return ""
        val hours = millis / (1000 * 3600)
        val minutes = (millis % (1000 * 3600)) / (1000 * 60)
        return if (hours > 0) {
            if (isPersian) "$hours ساعت و $minutes دقیقه" else "${hours}h ${minutes}m"
        } else {
            if (isPersian) "$minutes دقیقه" else "${minutes}m"
        }
    }

    private fun String.capitalizeWords(): String =
        split(" ").joinToString(" ") { word ->
            word.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
        }
}
