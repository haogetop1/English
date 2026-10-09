package com.xunji.flashcards.compat

import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.openspacedrepetition.Card
import io.github.openspacedrepetition.Rating
import io.github.openspacedrepetition.Scheduler
import java.time.Instant
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FsrsAndroidCompatibilityTest {
    @Test fun schedulingAndRecordsWorkOnAndroid() {
        val at = Instant.parse("2026-10-09T08:00:00Z")
        val original = Card.builder().cardId(0).due(at).build()
        val scheduler = Scheduler.builder().enableFuzzing(false).build()
        assertEquals(at.plusSeconds(60), scheduler.reviewCard(original, Rating.AGAIN, at, 1000).card().due)
        val good = scheduler.reviewCard(original, Rating.GOOD, at, 1000)
        assertEquals(at.plusSeconds(600), good.card().due)
        assertEquals(Rating.GOOD, good.reviewLog().rating())
        assertNull(original.lastReview)
    }
}
