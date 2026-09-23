package com.example.data.repository

import android.app.Activity
import android.content.Context
import com.example.billing.MyketBillingManager

/**
 * Repository responsible for handling Donation operations securely.
 * Enforces strict separation between Donation (Support) and Premium/VIP Entitlements.
 */
object DonationRepository {

    /**
     * Executes the donation process using the official billing provider.
     * Guaranteed NOT to touch VIP/Premium entitlement states.
     */
    fun processDonation(
        activity: Activity?,
        context: Context,
        sku: String,
        amountToman: Int,
        onSuccess: (unlockedAngelBadge: Boolean) -> Unit,
        onError: (message: String) -> Unit
    ) {
        if (activity == null) {
            onError("امکان برقراری ارتباط با محیط برنامه جهت پرداخت وجود ندارد.")
            return
        }

        MyketBillingManager.launchPurchase(
            activity = activity,
            sku = sku,
            onSuccess = {
                val unlockedAngel = UserProfileManager.recordDonation(context, amountToman)
                onSuccess(unlockedAngel)
            },
            onError = { errorMsg ->
                onError(errorMsg)
            }
        )
    }

    /**
     * Checks if the user is a financial supporter.
     */
    fun isSupporter(context: Context): Boolean {
        return UserProfileManager.isSupporter(context)
    }

    /**
     * Gets the total amount donated in Toman.
     */
    fun getTotalDonated(context: Context): Int {
        return UserProfileManager.getTotalDonatedToman(context)
    }
}
