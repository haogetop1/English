package com.xunji.core.study

import com.xunji.core.model.*
import com.xunji.core.time.LocalDay
import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.*
import org.junit.Test

class NewCardQuotaTest {
    private val shanghai=ZoneId.of("Asia/Shanghai")
    private val at=Instant.parse("2026-10-09T15:59:59Z")
    private fun schedule(id:String, first:Long?)=CardSchedule(id,Phase.LEARNING,0,1.0,5.0,at.toEpochMilli(),first,first,1)
    @Test fun countsFirstLearningAcrossAllDecksOnlyOnce() {
        val all=listOf(schedule("one",at.toEpochMilli()),schedule("two",at.toEpochMilli()),schedule("old",at.minusSeconds(86400).toEpochMilli()))
        assertEquals(18,NewCardQuota.remaining(all,AppSettings(),at,shanghai))
    }
    @Test fun localMidnightResetsQuota() {
        val one=listOf(schedule("one",at.toEpochMilli()))
        assertEquals(19,NewCardQuota.remaining(one,AppSettings(),at,shanghai))
        assertEquals(20,NewCardQuota.remaining(one,AppSettings(),at.plusSeconds(1),shanghai))
    }
    @Test fun zeroAndExhaustedQuotaNeverBecomeNegative() {
        assertEquals(0,NewCardQuota.remaining(listOf(schedule("one",at.toEpochMilli())),AppSettings(0),at,shanghai))
    }
    @Test fun summerTimeDayIsTwentyThreeHours() {
        val bounds=LocalDay.bounds(Instant.parse("2026-03-08T12:00:00Z"),ZoneId.of("America/New_York"))
        assertEquals(23*60*60*1000L,bounds.endExclusive-bounds.start)
    }
}
