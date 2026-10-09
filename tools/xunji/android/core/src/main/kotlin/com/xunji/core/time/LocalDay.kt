package com.xunji.core.time
import java.time.Instant
import java.time.ZoneId
data class DayBounds(val start:Long,val endExclusive:Long)
object LocalDay {
    fun bounds(at:Instant,zone:ZoneId):DayBounds {
        val date=at.atZone(zone).toLocalDate()
        return DayBounds(date.atStartOfDay(zone).toInstant().toEpochMilli(),date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli())
    }
}
