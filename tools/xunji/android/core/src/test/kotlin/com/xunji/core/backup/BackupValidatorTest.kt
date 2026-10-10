package com.xunji.core.backup

import com.xunji.core.model.*
import com.xunji.core.scheduling.FsrsScheduler
import java.time.Instant
import java.util.UUID
import org.junit.Test
import org.junit.Assert.*

class BackupValidatorTest {
    private fun id()=UUID.randomUUID().toString()
    private val at=Instant.parse("2026-10-09T08:00:00Z")
    private val deck=Deck(id(),"雅思",at.toEpochMilli(),at.toEpochMilli())
    private val card=Flashcard(id(),deck.id,"line one\nline two 😀","释义","句子",at.toEpochMilli(),at.toEpochMilli())
    private val before=CardSchedule(card.id,Phase.NEW,null,null,null,at.toEpochMilli(),null,null,0)
    private val after=FsrsScheduler().review(before,Rating.GOOD,at)
    private val log=ReviewLog(id(),card.id,deck.id,id(),Rating.GOOD,at.toEpochMilli(),1000,before,after,null)
    private val document=BackupDocument(exportedAt=at.toEpochMilli(),decks=listOf(deck),cards=listOf(card),schedules=listOf(after),reviewLogs=listOf(log),settings=AppSettings(8,ThemeMode.DARK))
    private fun rejected(doc:BackupDocument) { try { BackupValidator.validate(doc);fail("invalid backup accepted") } catch(_:IllegalArgumentException) {} }
    @Test fun roundTripPreservesUnicodeAndSnapshots() { BackupValidator.validate(document);assertEquals(document,BackupCodec.decode(BackupCodec.encode(document))) }
    @Test fun rejectsUnknownSchemaDuplicateAndMissingRelations() {
        rejected(document.copy(schemaVersion=2));rejected(document.copy(format="other"));rejected(document.copy(decks=listOf(deck,deck)))
        rejected(document.copy(cards=listOf(card.copy(deckId=id()))));rejected(document.copy(schedules=emptyList()))
        rejected(document.copy(schedules=listOf(after,after)));rejected(document.copy(cards=listOf(card.copy(id="bad-id"))))
    }
    @Test fun rejectsNonFiniteParametersAndIllegalState() {
        listOf(Double.NaN,Double.POSITIVE_INFINITY,0.0).forEach { rejected(document.copy(schedules=listOf(after.copy(stability=it)))) }
        rejected(document.copy(schedules=listOf(after.copy(difficulty=11.0))))
        rejected(document.copy(schedules=listOf(after.copy(stability=null))))
        rejected(document.copy(schedules=listOf(after.copy(lastReviewAt=null))))
        rejected(document.copy(schedules=listOf(before.copy(firstReviewedAt=at.toEpochMilli()))))
    }
    @Test fun rejectsInvalidTimestampsDurationsAndFields() {
        rejected(document.copy(exportedAt=-1));rejected(document.copy(cards=listOf(card.copy(createdAt=253402300800000))))
        rejected(document.copy(cards=listOf(card.copy(front=" "))));rejected(document.copy(cards=listOf(card.copy(back="x".repeat(10001)))))
        rejected(document.copy(reviewLogs=listOf(log.copy(durationMs=300001))));rejected(document.copy(settings=AppSettings(101)))
    }
    @Test fun rejectsMismatchedLogSnapshotsAndDeck() {
        rejected(document.copy(reviewLogs=listOf(log.copy(deckId=id()))))
        rejected(document.copy(reviewLogs=listOf(log.copy(beforeSchedule=before.copy(cardId=id())))))
        rejected(document.copy(reviewLogs=listOf(log.copy(afterSchedule=after.copy(revision=7)))))
    }
    @Test fun rejectsUnknownJsonKeysAndSpecialNumbers() {
        val encoded=BackupCodec.encode(document)
        try { BackupCodec.decode(encoded.dropLast(1)+",\"extra\":true}");fail() } catch(_:IllegalArgumentException) {}
        try { BackupCodec.decode(encoded.replace("\"stability\":", "\"stability\":NaN,\"bad\":"));fail() } catch(_:IllegalArgumentException) {}
    }
}
