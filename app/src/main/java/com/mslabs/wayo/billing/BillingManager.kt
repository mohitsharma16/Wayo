package com.mslabs.wayo.billing

import android.app.Activity
import android.content.Context
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Wraps Google Play Billing for the single one-time "full unlock" product.
 *
 * IMPORTANT: create a managed (one-time) product in Play Console with this
 * exact product ID before the purchase flow will work.
 */
class BillingManager(private val context: Context) {

    companion object {
        const val PRODUCT_ID = "unlock_full_access"
    }

    private val _isPro = MutableStateFlow(false)
    val isPro: StateFlow<Boolean> = _isPro

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

    fun launchPurchase(activity: Activity) {
        val details = productDetails ?: return

        val productDetailsParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(details)
            .build()

        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productDetailsParams))
            .build()

        billingClient.launchBillingFlow(activity, flowParams)
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
            _isPro.value = unlocked
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
                _isPro.value = true
            }
        }
    }
}
