package com.xunji.core.study
import com.xunji.core.model.*
import java.time.Instant
import java.time.ZoneId
import com.xunji.core.time.LocalDay
object NewCardQuota {
    fun remaining(allSchedules:List<CardSchedule>,settings:AppSettings,at:Instant,zone:ZoneId):Int {
        val day=LocalDay.bounds(at,zone)
        val started=allSchedules.count { it.firstReviewedAt?.let { first -> first>=day.start && first<day.endExclusive } == true }
        return (settings.newCardsPerDay-started).coerceAtLeast(0)
    }
}
