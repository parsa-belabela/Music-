package com.example.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.example.BuildConfig
import com.example.data.repository.UserProfileManager
import ir.myket.billingclient.IabHelper
import ir.myket.billingclient.util.IabResult
import ir.myket.billingclient.util.Inventory
import ir.myket.billingclient.util.Purchase

/**
 * Official Myket In-App Billing Integration Manager for Aura Music.
 * Handles setup, querying inventory, purchases, and consumption for support packages.
 */
object MyketBillingManager {
    private const val TAG = "MyketBilling"

    // Supported SKUs defined in Myket Developer Panel
    const val SKU_SUPPORT_GUM = "support_gum"             // 25,000 Toman (Consumable)
    const val SKU_SUPPORT_COFFEE = "support_coffee"       // 50,000 Toman (Consumable)
    const val SKU_SUPPORT_PIZZA = "support_pizza"         // 150,000 Toman (Consumable)
    const val SKU_SUPPORT_GOLD = "support_golden_patron"  // 300,000 Toman (Consumable)

    val ALL_SKUS: ArrayList<String> = arrayListOf(
        SKU_SUPPORT_GUM,
        SKU_SUPPORT_COFFEE,
        SKU_SUPPORT_PIZZA,
        SKU_SUPPORT_GOLD
    )

    private var helper: IabHelper? = null
    var isSetupDone = false
        private set

    /**
     * Initialize IabHelper with public RSA key from BuildConfig.
     */
    fun init(context: Context, onSetupComplete: (Boolean) -> Unit = {}) {
        if (helper != null && isSetupDone) {
            onSetupComplete(true)
            return
        }

        val publicKey = try {
            BuildConfig.IAB_PUBLIC_KEY
        } catch (_: Exception) {
            ""
        }

        try {
            helper = IabHelper(context.applicationContext, publicKey).apply {
                enableDebugLogging(BuildConfig.DEBUG)
                startSetup { result: IabResult? ->
                    if (helper == null) return@startSetup
                    if (result != null && result.isSuccess) {
                        isSetupDone = true
                        Log.d(TAG, "Myket billing setup successful.")
                        queryPurchases(context)
                        onSetupComplete(true)
                    } else {
                        isSetupDone = false
                        val msg = result?.message ?: "Unknown setup error"
                        Log.w(TAG, "Myket billing setup failed: $msg")
                        onSetupComplete(false)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize IabHelper", e)
            onSetupComplete(false)
        }
    }

    /**
     * Query inventory to consume any pending purchases and sync status on launch.
     */
    fun queryPurchases(context: Context) {
        val curHelper = helper ?: return
        if (!isSetupDone) return

        try {
            curHelper.queryInventoryAsync(true, ALL_SKUS, IabHelper.QueryInventoryFinishedListener { result: IabResult?, inventory: Inventory? ->
                if (helper == null) return@QueryInventoryFinishedListener
                if (result != null && result.isSuccess && inventory != null) {
                    handleInventoryResult(context, inventory)
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "Error querying inventory", e)
        }
    }

    private fun handleInventoryResult(context: Context, inventory: Inventory) {
        for (sku in ALL_SKUS) {
            val purchase = inventory.getPurchase(sku)
            if (purchase != null) {
                UserProfileManager.markAsSupporter(context, viaAd = false)
                // Consume consumable support donations so user can support again anytime
                consumePurchase(context, purchase)
            }
        }
    }

    /**
     * Launch official Myket purchase flow for a support tier.
     */
    fun launchPurchase(
        activity: Activity,
        sku: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val curHelper = helper
        if (curHelper == null || !isSetupDone) {
            // If setup hasn't finished or market is not available, init & inform
            init(activity) { success ->
                if (success) {
                    launchPurchase(activity, sku, onSuccess, onError)
                } else {
                    onError("اتصال به سرویس پرداخت مایکت برقرار نشد. لطفاً از نصب و ورود به برنامه مایکت اطمینان حاصل کنید.")
                }
            }
            return
        }

        try {
            val payload = "aura_payload_${System.currentTimeMillis()}"
            curHelper.launchPurchaseFlow(activity, sku, { result: IabResult?, purchase: Purchase? ->
                if (helper == null) return@launchPurchaseFlow
                if (result != null && result.isSuccess && purchase != null) {
                    Log.d(TAG, "Purchase succeeded for $sku")
                    UserProfileManager.markAsSupporter(activity, viaAd = false)
                    consumePurchase(activity, purchase)
                    onSuccess()
                } else {
                    val msg = result?.message ?: "فرآیند خرید لغو شد یا با خطا مواجه شد."
                    Log.w(TAG, "Purchase failed: $msg")
                    onError(msg)
                }
            }, payload)
        } catch (e: Exception) {
            Log.e(TAG, "Error launching purchase flow", e)
            onError(e.localizedMessage ?: "خطای ناشناخته در خرید")
        }
    }

    private fun consumePurchase(context: Context, purchase: Purchase) {
        val curHelper = helper ?: return
        try {
            curHelper.consumeAsync(purchase) { _: Purchase?, result: IabResult? ->
                if (result != null && result.isSuccess) {
                    Log.d(TAG, "Purchase consumed successfully: ${purchase.sku}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error consuming purchase", e)
        }
    }

    /**
     * Clean up helper in onDestroy.
     */
    fun dispose() {
        try {
            helper?.dispose()
        } catch (_: Exception) {}
        helper = null
        isSetupDone = false
    }
}
