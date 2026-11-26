package fi.vladi.valvontapoytakirja
import android.os.SystemClock
import com.google.firebase.auth.FirebaseAuth

internal object PremiumAccess {
    private data class Lease(val verified: VerifiedSubscription, val deadline: Long)
    @Volatile private var lease: Lease? = null
    fun clear() { lease = null }
    fun update(verified: VerifiedSubscription?, deadline: Long) {
        lease = if (verified == null || BuildConfig.OFFLINE_PREVIEW) null else Lease(verified, deadline)
    }
    fun requireCloud() {
        check(!BuildConfig.OFFLINE_PREVIEW) { "Premium required" }
        val current = lease
        check(current != null && SystemClock.elapsedRealtime() < current.deadline &&
            current.verified.permitsPremium(FirebaseAuth.getInstance().currentUser?.uid, System.currentTimeMillis())) { "Premium required" }
    }
}
