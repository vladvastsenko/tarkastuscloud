package fi.vladi.valvontapoytakirja
import android.os.SystemClock
import com.google.firebase.auth.FirebaseAuth

internal fun permitsCloudLease(preview: Boolean, verified: VerifiedSubscription?, uid: String?, wallNow: Long, elapsedNow: Long, deadline: Long): Boolean =
    !preview && elapsedNow < deadline && verified?.permitsPremium(uid, wallNow) == true

// Temporary authenticated test access; never enabled in release or checkout builds.
internal fun permitsDeveloperTest(debug: Boolean, preview: Boolean, checkout: Boolean, verified: Boolean, email: String?): Boolean =
    debug && !preview && !checkout && verified && email.equals("vlad.vastsenko@gmail.com", ignoreCase = true)

internal fun isDeveloperTestAccount(): Boolean {
    if (!BuildConfig.DEBUG || BuildConfig.OFFLINE_PREVIEW || BuildConfig.PREMIUM_CHECKOUT_ENABLED) return false
    val user = FirebaseAuth.getInstance().currentUser ?: return false
    return permitsDeveloperTest(BuildConfig.DEBUG, BuildConfig.OFFLINE_PREVIEW, BuildConfig.PREMIUM_CHECKOUT_ENABLED, user.isEmailVerified, user.email)
}

internal object PremiumAccess {
    private data class Lease(val verified: VerifiedSubscription, val deadline: Long)
    @Volatile private var lease: Lease? = null
    @Volatile var developerFree = false
    fun clear() { lease = null }
    fun update(verified: VerifiedSubscription?, deadline: Long) {
        lease = if (verified == null || BuildConfig.OFFLINE_PREVIEW) null else Lease(verified, deadline)
    }
    fun requireCloud() {
        check(!BuildConfig.OFFLINE_PREVIEW) { "Premium required" }
        if (isDeveloperTestAccount()) { check(!developerFree) { "Premium required" }; return }
        val current = lease
        check(current != null && permitsCloudLease(BuildConfig.OFFLINE_PREVIEW, current.verified,
            FirebaseAuth.getInstance().currentUser?.uid, System.currentTimeMillis(), SystemClock.elapsedRealtime(), current.deadline)) { "Premium required" }
    }
}
