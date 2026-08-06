package com.mslabs.wayo.billing

import android.app.Activity
import android.content.Context
import com.mslabs.wayo.BuildConfig
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Wraps Google Play Billing for the single one-time "full unlock" product.
 *
 * IMPORTANT: create a managed (one-time) product in Play Console with this
 * exact product ID before the purchase flow will work.
 *
 * Caching strategy: the unlock state is cached in SharedPreferences so the
 * UI can show the correct state instantly on launch instead of defaulting
 * to "locked" for the brief moment before Play Billing responds. This is
 * a cache for speed, not a replacement for the real check -- Play Billing
 * is still queried on every launch and the cache is corrected if it's
 * wrong. Trusting a local flag forever would mean a refunded or charged-
 * back purchase could never be re-locked, and a purchase restored on a
 * new device would never be picked up.
 */
class BillingManager(private val context: Context) {

    companion object {
        const val PRODUCT_ID = "unlock_full_access"
        private const val PREFS_NAME = "wayo_billing"
        private const val KEY_IS_PRO = "is_pro"
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // Seeded from the cache immediately so the UI never has to show a
    // false "locked" flash while the real Play Billing query is in flight.
    private val _isPro = MutableStateFlow(prefs.getBoolean(KEY_IS_PRO, false))
    val isPro: StateFlow<Boolean> = _isPro

    // Fires only for a real-time purchase completion (handlePurchase, below)
    // -- never for the ownership sync that runs on every app launch -- so
    // the UI can show a one-time "unlocked!" moment without re-showing it
    // every time an already-Pro user reopens the app.
    private val _purchaseCompleted = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val purchaseCompleted: SharedFlow<Unit> = _purchaseCompleted.asSharedFlow()

    private var productDetails: ProductDetails? = null

    private val purchasesListener = PurchasesUpdatedListener { billingResult, purchases ->
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            purchases?.forEach { handlePurchase(it) }
        }
    }

    private val billingClient = BillingClient.newBuilder(context)
        .setListener(purchasesListener)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()
        )
        .build()

    fun startConnection(onReady: () -> Unit = {}) {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    queryProductDetails()
                    queryExistingPurchases()
                    onReady()
                }
            }

            override fun onBillingServiceDisconnected() {
                // Play Billing recommends retrying with backoff; add if needed.
                // Note: the cached flag from SharedPreferences still holds
                // during any disconnected period, so the user isn't
                // incorrectly locked out just because this connection blipped.
            }
        })
    }

    private fun queryProductDetails() {
        val product = QueryProductDetailsParams.Product.newBuilder()
            .setProductId(PRODUCT_ID)
            .setProductType(BillingClient.ProductType.INAPP)
            .build()

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(listOf(product))
            .build()

        billingClient.queryProductDetailsAsync(params) { _, result ->
            productDetails = result.productDetailsList.firstOrNull()
        }
    }

    /**
     * Returns false (and launches nothing) if the product details haven't
     * loaded yet -- e.g. the billing connection is still in flight, there's
     * no network right now, or (during development) the product ID isn't
     * set up in Play Console yet. Without this, tapping "Unlock full
     * access" in that state did nothing at all, with no feedback that
     * anything had gone wrong.
     */
    fun launchPurchase(activity: Activity): Boolean {
        val details = productDetails ?: return false

        val productDetailsParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(details)
            .build()

        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productDetailsParams))
            .build()

        billingClient.launchBillingFlow(activity, flowParams)
        return true
    }

    fun queryExistingPurchases() {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()

        billingClient.queryPurchasesAsync(params) { _, purchases ->
            val unlocked = purchases.any {
                it.products.contains(PRODUCT_ID) &&
                    it.purchaseState == Purchase.PurchaseState.PURCHASED
            }
            setIsPro(unlocked)
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            if (!purchase.isAcknowledged) {
                val ackParams = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()
                billingClient.acknowledgePurchase(ackParams) {}
            }
            if (purchase.products.contains(PRODUCT_ID)) {
                val wasAlreadyPro = _isPro.value
                setIsPro(true)
                if (!wasAlreadyPro) {
                    _purchaseCompleted.tryEmit(Unit)
                }
            }
        }
    }

    /** Updates in-memory state and persists it, in one place, so the two never drift apart. */
    private fun setIsPro(value: Boolean) {
        _isPro.value = value
        prefs.edit().putBoolean(KEY_IS_PRO, value).apply()
    }

    /**
     * Debug-only escape hatch for exercising the paywall/unlock UI without a
     * real Play purchase. No-ops in release builds (BuildConfig.DEBUG is a
     * compile-time constant there, so this whole branch is dead code and
     * gets stripped -- it can't be reached in a release build regardless of
     * how it's called).
     */
    fun debugToggleIsPro() {
        if (BuildConfig.DEBUG) {
            setIsPro(!_isPro.value)
        }
    }
}
