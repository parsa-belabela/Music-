package com.example.billing

import android.app.Activity
import android.content.Context

enum class DonationTier(
    val sku: String,
    val amountToman: Int,
    val titleFa: String,
    val titleEn: String,
    val emoji: String
) {
    GUM(MyketBillingManager.SKU_SUPPORT_GUM, 25_000, "یک آدامس خرسی", "Buy a Gummy Bear", "🐻"),
    COFFEE(MyketBillingManager.SKU_SUPPORT_COFFEE, 50_000, "یک فنجان قهوه گرم", "Buy a Warm Coffee", "☕"),
    PIZZA(MyketBillingManager.SKU_SUPPORT_PIZZA, 150_000, "یک پیتزای دورهمی", "Buy a Pizza", "🍕"),
    GOLD(MyketBillingManager.SKU_SUPPORT_GOLD, 300_000, "حامی طلایی و ویژه آئورا", "Golden Patron", "🌟")
}

sealed class PurchaseResult {
    data class Success(val sku: String, val amountToman: Int, val purchaseToken: String?) : PurchaseResult()
    data class Error(val message: String) : PurchaseResult()
    object Cancelled : PurchaseResult()
    object ServiceUnavailable : PurchaseResult()
}

/**
 * Clean PaymentProvider abstraction for In-App Billing integrations (e.g. Myket).
 * Ensures decoupling between UI/Data layer and billing SDK implementations.
 */
interface PaymentProvider {
    val isConfigured: Boolean
    val isSetupDone: Boolean
    val isPurchaseInProgress: Boolean

    fun init(context: Context, onComplete: (Boolean) -> Unit = {})
    fun launchPurchase(
        activity: Activity,
        tier: DonationTier,
        onResult: (PurchaseResult) -> Unit
    )
    fun queryPurchases(context: Context)
    fun dispose()
}
