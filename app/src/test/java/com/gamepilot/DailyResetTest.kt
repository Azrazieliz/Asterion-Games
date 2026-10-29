package com.gamepilot

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.util.TimeZone

class DailyResetTest {
    private val utc = TimeZone.getTimeZone("UTC")
    @Test fun beforeConfiguredResetBelongsToPriorGameDay() {
        val moment = Instant.parse("2026-10-29T03:59:00Z").toEpochMilli()
        assertEquals("2026-10-28", DailyReset.key(moment, 4, utc))
        assertEquals("2026-10-29", DailyReset.key(moment, 0, utc))
    }
    @Test fun atResetBeginsNewDay() {
        val moment = Instant.parse("2026-10-29T04:00:00Z").toEpochMilli()
        assertEquals("2026-10-29", DailyReset.key(moment, 4, utc))
    }
    @Test fun midnightStillWorksAcrossMonthEnd() {
        val moment = Instant.parse("2026-11-01T00:01:00Z").toEpochMilli()
        assertEquals("2026-10-31", DailyReset.key(moment, 4, utc))
    }
}
