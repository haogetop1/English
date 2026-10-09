package com.xunji.core.scheduling

import com.xunji.core.model.*
import java.time.Instant
import org.junit.Assert.*
import org.junit.Test

class FsrsSchedulerTest {
    private val at = Instant.parse("2026-10-09T08:00:00Z")
    private val initial = CardSchedule("card", Phase.NEW, null, null, null, at.toEpochMilli(), null, null, 0)
    private val scheduler: Scheduler = FsrsScheduler()

    @Test fun againUsesOneMinute() {
        val result = scheduler.review(initial, Rating.AGAIN, at)
        assertEquals(at.toEpochMilli() + 60_000, result.dueAt)
        assertEquals(Phase.LEARNING, result.phase)
    }
    @Test fun goodUsesTenMinutesAndStartsHistory() {
        val result = scheduler.review(initial, Rating.GOOD, at)
        assertEquals(at.toEpochMilli() + 600_000, result.dueAt)
        assertEquals(at.toEpochMilli(), result.firstReviewedAt)
        assertEquals(at.toEpochMilli(), result.lastReviewAt)
        assertEquals(1, result.revision)
    }
    @Test fun easyGraduatesAndAgainRelearns() {
        val easy = scheduler.review(initial, Rating.EASY, at)
        assertEquals(Phase.REVIEW, easy.phase)
        val later = Instant.ofEpochMilli(easy.dueAt)
        val forgot = scheduler.review(easy, Rating.AGAIN, later)
        assertEquals(Phase.RELEARNING, forgot.phase)
        assertEquals(later.toEpochMilli() + 600_000, forgot.dueAt)
        assertEquals(at.toEpochMilli(), forgot.firstReviewedAt)
    }
    @Test fun previewsAreCompletePureAndDeterministic() {
        val before = initial.copy()
        val previews = scheduler.preview(initial, at)
        assertEquals(Rating.entries.toSet(), previews.keys)
        Rating.entries.forEach { assertEquals(scheduler.review(initial, it, at), previews.getValue(it)) }
        assertEquals(before, initial)
        assertEquals(previews, scheduler.preview(initial, at))
    }
    @Test fun backwardClockIsRejected() {
        val reviewed = scheduler.review(initial, Rating.GOOD, at)
        assertThrows(IllegalArgumentException::class.java) { scheduler.review(reviewed, Rating.GOOD, at.minusSeconds(1)) }
    }
}
