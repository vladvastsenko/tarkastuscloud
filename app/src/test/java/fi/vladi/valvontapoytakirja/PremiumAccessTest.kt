package fi.vladi.valvontapoytakirja
import org.junit.Assert.*
import org.junit.Test
class PremiumAccessTest {
    @Test fun leaseRequiresCorrectAccountAndBothClocks() {
        val v = VerifiedSubscription("a", SubscriptionState.ACTIVE, 500)
        assertTrue(permitsCloudLease(false,v,"a",100,100,200))
        assertFalse(permitsCloudLease(true,v,"a",100,100,200))
        assertFalse(permitsCloudLease(false,v,"b",100,100,200))
        assertFalse(permitsCloudLease(false,v,null,100,100,200))
        assertFalse(permitsCloudLease(false,v,"a",100,200,200))
        assertFalse(permitsCloudLease(false,v,"a",500,100,200))
        assertFalse(permitsCloudLease(false,null,"a",100,100,200))
        for (state in SubscriptionState.values()) {
            assertEquals(state in setOf(SubscriptionState.ACTIVE,SubscriptionState.CANCELLED,SubscriptionState.GRACE_PERIOD),
                permitsCloudLease(false,v.copy(state=state),"a",100,100,200))
        }
    }
    @Test fun everyLanguageHasEveryState() {
        for (lang in listOf("en","fi","ru","sv","nb","da","et","lv","lt","fr","es","it","de","pl")) {
            assertEquals(9, SubscriptionState.values().map { subscriptionLabel(lang,it) }.toSet().size)
        }
    }
}
