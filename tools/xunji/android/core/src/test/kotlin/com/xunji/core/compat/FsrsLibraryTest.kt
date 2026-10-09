package com.xunji.core.compat

import io.github.openspacedrepetition.Card
import io.github.openspacedrepetition.Rating
import io.github.openspacedrepetition.Scheduler
import java.time.Instant
import org.junit.Assert.*
import org.junit.Test

class FsrsLibraryTest {
    private val at = Instant.parse("2026-10-09T08:00:00Z")
    private val scheduler = Scheduler.builder().enableFuzzing(false).build()
    private fun card() = Card.builder().cardId(0).due(at).build()

    @Test fun newCardAgainUsesOneMinute() {
        val original = card()
        val result = scheduler.reviewCard(original, Rating.AGAIN, at, 1000)
        assertEquals(at.plusSeconds(60), result.card().due)
        assertEquals(Rating.AGAIN, result.reviewLog().rating())
        assertEquals(at, original.due)
        assertNull(original.lastReview)
    }

    @Test fun newCardGoodUsesTenMinutes() {
        val result = scheduler.reviewCard(card(), Rating.GOOD, at, 1000)
        assertEquals(at.plusSeconds(600), result.card().due)
        assertEquals(Rating.GOOD, result.reviewLog().rating())
    }
}
