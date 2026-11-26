package fi.vladi.valvontapoytakirja

import android.app.Activity
import android.os.SystemClock
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.android.billingclient.api.*
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay

internal class PremiumController {
    var active by mutableStateOf(false)
        private set
    var error by mutableStateOf(false)
        private set
    var simulatedState by mutableStateOf(SubscriptionState.FREE)
        private set
    fun simulate(state: SubscriptionState) {
        if (!BuildConfig.DEBUG || !BuildConfig.OFFLINE_PREVIEW) return
        simulatedState = state
        active = VerifiedSubscription("preview", state, Long.MAX_VALUE).permitsPremium("preview", 0)
    }
    var busy by mutableStateOf(false)
    var ready by mutableStateOf(false)
    var details by mutableStateOf<ProductDetails?>(null)
    var offer by mutableStateOf<ProductDetails.SubscriptionOfferDetails?>(null)
    var uid by mutableStateOf<String?>(null)
    var billing: PremiumBilling? = null
    private var generation = 0
    private var deadline = 0L
    fun invalidate() { generation++; active = false; busy = false; deadline = 0L }
    fun tick() { if (SystemClock.elapsedRealtime() >= deadline) active = false }
    fun refresh() {
        val client = billing ?: return
        if (!ready || busy || uid == null) return
        val epoch = ++generation
        val account = uid
        busy = true
        error = false
        client.queryPurchases { result, purchases ->
            if (epoch != generation) return@queryPurchases
            val purchase = purchases.firstOrNull { it.purchaseState == Purchase.PurchaseState.PURCHASED && it.accountIdentifiers?.obfuscatedAccountId == account?.let(::premiumAccountHash) }
            if (result.responseCode != BillingClient.BillingResponseCode.OK || purchase == null) {
                busy = false; active = false; error = result.responseCode != BillingClient.BillingResponseCode.OK; return@queryPurchases
            }
            val start = SystemClock.elapsedRealtime()
            verifyPremiumPurchase(purchase) { verified ->
                if (epoch == generation && account == uid) {
                    val duration = ((verified?.expiresAt ?: 0L) - System.currentTimeMillis()).coerceIn(0L, 300000L)
                    deadline = start + duration
                    active = verified != null && SystemClock.elapsedRealtime() < deadline
                    error = verified == null
                    busy = false
                }
            }
        }
    }
    fun buy(activity: Activity) {
        if (!BuildConfig.PREMIUM_CHECKOUT_ENABLED || active || busy || !ready) return
        val account = uid ?: return
        val d = details ?: return
        val o = offer ?: return
        error = billing?.launchPurchase(activity,d,o,premiumAccountHash(account))?.responseCode != BillingClient.BillingResponseCode.OK
    }
}

@Composable
internal fun rememberPremiumController(): PremiumController {
    val context = LocalContext.current
    val controller = remember { PremiumController() }
    if (!BuildConfig.OFFLINE_PREVIEW) {
        DisposableEffect(context) {
            val auth = FirebaseAuth.getInstance()
            val listener = FirebaseAuth.AuthStateListener {
                val uid = it.currentUser?.uid
                if (uid != controller.uid) { controller.invalidate(); controller.uid = uid; controller.refresh() }
            }
            auth.addAuthStateListener(listener)
            val billing = PremiumBilling(context) { _, _ -> controller.refresh() }
            controller.billing = billing
            billing.connect {
                controller.ready = it.responseCode == BillingClient.BillingResponseCode.OK
                if (controller.ready) {
                    billing.queryOffer { result, details, offer ->
                        if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                            controller.details = details; controller.offer = offer
                        }
                    }
                    controller.refresh()
                }
            }
            onDispose { auth.removeAuthStateListener(listener); controller.invalidate(); controller.ready = false; controller.billing = null; billing.close() }
        }
        LaunchedEffect(controller) {
            while (true) { controller.tick(); delay(1000) }
        }
        LaunchedEffect(controller) {
            while (true) { delay(60000); controller.refresh() }
        }
    }
    return controller
}
