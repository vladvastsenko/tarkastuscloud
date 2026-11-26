package fi.vladi.valvontapoytakirja

import android.content.Context
import com.android.billingclient.api.*

/** Queries Play only. Purchase callbacks never grant an entitlement locally. */
internal class PremiumBilling(context: Context, onPurchases: (BillingResult, List<Purchase>) -> Unit) {
    private val client = BillingClient.newBuilder(context.applicationContext)
        .setListener { result, purchases -> onPurchases(result, purchases.orEmpty()) }
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .build()

    fun connect(onResult: (BillingResult) -> Unit) {
        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) = onResult(result)
            override fun onBillingServiceDisconnected() = Unit
        })
    }

    fun queryPurchases(onResult: (BillingResult, List<Purchase>) -> Unit) {
        client.queryPurchasesAsync(QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS).build()) { result, purchases ->
            onResult(result, purchases.filter { PREMIUM_PRODUCT in it.products })
        }
    }

    fun close() = client.endConnection()
}
