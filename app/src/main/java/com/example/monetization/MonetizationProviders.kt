package com.example.monetization

import android.app.Activity
import android.content.Context

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
    var billingProvider: BillingProvider = NoOpBillingProvider()
}
