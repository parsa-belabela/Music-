package com.example.monetization

import android.app.Activity
import android.content.Context

/**
 * Interface abstraction for rewarded video advertisements (e.g. Tapsell, AdMob, Unity Ads).
 * Allows plugging in real SDK implementations without touching the UI or business logic.
 */
interface RewardedAdProvider {
    fun preload(context: Context)
    fun isReady(): Boolean
    fun show(activity: Activity, onEarned: () -> Unit, onFailed: (String) -> Unit)
}

/**
 * Default fallback / stub provider for environments before integrating an actual ad network.
 * In debug / simulated mode, it can simulate viewing a rewarded video to preview the full UX flow.
 */
class NoOpRewardedAdProvider : RewardedAdProvider {
    var simulationMode: Boolean = true

    override fun preload(context: Context) {
        // Ready for real SDK preloading
    }

    override fun isReady(): Boolean = true

    override fun show(activity: Activity, onEarned: () -> Unit, onFailed: (String) -> Unit) {
        if (simulationMode) {
            // Emulate rewarded ad completion for testing
            onEarned()
        } else {
            onFailed("سرویس تبلیغات هنوز پیکربندی نشده است")
        }
    }
}

/**
 * Interface abstraction for in-app billing / subscription providers (e.g. Bazaar IAB, Google Play Billing).
 */
interface BillingProvider {
    suspend fun purchaseAnnualVip(activity: Activity): Boolean
    fun getFormattedPrice(): String
    fun getDailyBreakdownPrice(): String
}

/**
 * Default fallback / stub billing provider.
 */
class NoOpBillingProvider : BillingProvider {
    var simulationSuccess: Boolean = false

    override suspend fun purchaseAnnualVip(activity: Activity): Boolean {
        return simulationSuccess
    }

    override fun getFormattedPrice(): String = "۳۵٬۰۰۰ تومان / سال"

    override fun getDailyBreakdownPrice(): String = "کمتر از ۱۰۰ تومان در روز"
}

/**
 * Central registry holding the active Monetization providers.
 */
object MonetizationService {
    var rewardedAdProvider: RewardedAdProvider = NoOpRewardedAdProvider()
    var billingProvider: BillingProvider = NoOpBillingProvider()
}
