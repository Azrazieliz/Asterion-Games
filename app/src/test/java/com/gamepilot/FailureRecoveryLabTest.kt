package com.gamepilot

import org.junit.Assert.*
import org.junit.Test

class FailureRecoveryLabTest {
    private val lab=FailureRecoveryLab
    @Test fun holdsAfterThreeIdenticalFailures() {
        val a=lab.next(null,26,"No safe action","scene-a",1000L)
        val b=lab.next(a,26,"No safe action","scene-a",2000L)
        val c=lab.next(b,26,"No safe action","scene-a",3000L)
        assertFalse(a.requiresReview)
        assertFalse(b.requiresReview)
        assertTrue(c.requiresReview)
    }
    @Test fun unlikeFailuresOrOldSessionsDoNotInflateSameFailure() {
        val first=lab.next(null,26,"Wrong app","x",1000)
        assertEquals(1,lab.next(first,26,"No vision","y",2000).occurrences)
        val outside=1000L+24L*60*60*1000+1
        assertEquals(1,lab.next(first,26,"Wrong app","x",outside).occurrences)
    }
    @Test fun distinctAccountsAreDistinctByGameProfileId() {
        val global=lab.next(null,26,"Low confidence","scene",1000)
        assertEquals(1,lab.next(global,22,"Low confidence","scene",2000).occurrences)
    }
}
