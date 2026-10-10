package com.xunji.core.stats
import com.xunji.core.model.*
import org.junit.Test
import org.junit.Assert.*
import java.time.*
class StudyStatisticsTest {
 private val at=Instant.parse("2026-10-09T08:00:00Z")
 private fun log(id:String,card:String,rating:Rating,undone:Long?=null,time:Long=at.toEpochMilli()):ReviewLog {
  val s=CardSchedule(card,Phase.NEW,null,null,null,time,null,null,0)
  return ReviewLog(id,card,"deck","session",rating,time,1000,s,s,undone)
 }
 @Test fun zeroRecordsHaveNoRatioAndSevenDaysIncludingToday() { val s=StudyStatistics.summarize(emptyList(),at,ZoneOffset.UTC);assertNull(s.today.againRatio);assertEquals(7,s.lastSevenDays.size);assertEquals(s.today,s.lastSevenDays.last()) }
 @Test fun countsDistinctCardsAndExcludesUndone() {
  val logs=listOf(log("1","a",Rating.AGAIN),log("2","a",Rating.GOOD),log("3","b",Rating.HARD),log("4","b",Rating.EASY),log("5","c",Rating.AGAIN,at.toEpochMilli()))
  val s=StudyStatistics.summarize(logs,at,ZoneOffset.UTC).today
  assertEquals(4,s.scoreCount);assertEquals(2,s.distinctCards);assertEquals(0.25,s.againRatio!!,0.0);assertEquals(4000L,s.durationMs)
 }
 @Test fun usesLocalMidnightAndExcludesFutureDays() {
  val boundary=Instant.parse("2026-10-08T16:00:00Z").toEpochMilli()
  val s=StudyStatistics.summarize(listOf(log("1","a",Rating.GOOD,time=boundary-1),log("2","b",Rating.GOOD,time=boundary)),at,ZoneId.of("Asia/Shanghai"))
  assertEquals(1,s.today.scoreCount);assertEquals(1,s.lastSevenDays[5].scoreCount)
 }
}
