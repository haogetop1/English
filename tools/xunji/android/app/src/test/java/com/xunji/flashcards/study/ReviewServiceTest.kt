package com.xunji.flashcards.study

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.xunji.core.model.*
import com.xunji.core.scheduling.FsrsScheduler
import com.xunji.core.study.NewCardQuota
import com.xunji.flashcards.data.*
import java.time.*
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class) @Config(sdk=[28])
class ReviewServiceTest {
    private val clock=Clock.fixed(Instant.parse("2026-10-09T08:00:00Z"),ZoneOffset.UTC)
    private lateinit var db:XunjiDatabase
    private lateinit var repo:RoomStudyRepository
    private lateinit var service:ReviewService
    private lateinit var deck:Deck
    private lateinit var card:Flashcard
    private val session=UUID.randomUUID().toString()
    @Before fun setup()=runBlocking {
        db=Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(),XunjiDatabase::class.java).allowMainThreadQueries().build()
        repo=RoomStudyRepository(db,clock);service=ReviewService(db,FsrsScheduler(),clock) { ZoneOffset.UTC }
        deck=repo.createDeck("词汇");card=repo.saveCard(null,deck.id,"word","词","")
    }
    @After fun close() { db.close() }
    private fun command(id:String=UUID.randomUUID().toString(),cardId:String=card.id,revision:Long=0)=GradeCommand(id,session,cardId,revision,Rating.GOOD,1000)
    @Test fun recordsOnceAndRetriesIdempotently()=runBlocking {
        val command=command()
        val first=service.grade(command)
        assertTrue(first is GradeResult.Recorded)
        assertTrue(service.grade(command) is GradeResult.Duplicate)
        assertEquals(1,repo.observeLogs().first().size)
        val state=repo.candidates(deck.id).single().schedule
        assertEquals(1L,state.revision);assertEquals(clock.millis(),state.firstReviewedAt)
    }
    @Test fun conflictingIdempotencyKeyIsRejected()=runBlocking {
        val command=command();service.grade(command)
        assertEquals(GradeResult.InvalidCommand,service.grade(command.copy(rating=Rating.AGAIN)))
        assertEquals(1,repo.observeLogs().first().size)
    }
    @Test fun missingAndStaleCardsCannotBeScored()=runBlocking {
        assertEquals(GradeResult.StaleSchedule,service.grade(command(revision=7)))
        repo.deleteCard(card.id)
        assertEquals(GradeResult.MissingCard,service.grade(command()))
        assertTrue(repo.observeLogs().first().isEmpty())
    }
    @Test fun exhaustedQuotaDoesNotBlockExistingLearning()=runBlocking {
        repo.saveSettings(AppSettings(1))
        assertTrue(service.grade(command()) is GradeResult.Recorded)
        val second=repo.saveCard(null,deck.id,"two","二","")
        assertEquals(GradeResult.QuotaReached,service.grade(command(cardId=second.id)))
        val future=Clock.offset(clock,Duration.ofMinutes(10))
        val later=ReviewService(db,FsrsScheduler(),future) { ZoneOffset.UTC }
        assertTrue(later.grade(command(revision=1)) is GradeResult.Recorded)
        assertEquals(0,NewCardQuota.remaining(repo.observeSchedules().first(),AppSettings(1),future.instant(),ZoneOffset.UTC))
    }
    @Test fun failedLogInsertRollsBackSchedule()=runBlocking {
        val before=repo.candidates(deck.id).single().schedule
        db.openHelper.writableDatabase.execSQL("CREATE TRIGGER fail_log BEFORE INSERT ON review_logs BEGIN SELECT RAISE(ABORT, 'test insertion failure'); END")
        var failed=false
        try { service.grade(command()) } catch(_:Exception) { failed=true }
        assertTrue("real SQLite insert must fail",failed)
        assertEquals(before,repo.candidates(deck.id).single().schedule)
        assertTrue(repo.observeLogs().first().isEmpty())
    }
    @Test fun undoRestoresStateAndQuotaWithMonotonicRevision()=runBlocking {
        val before=repo.candidates(deck.id).single().schedule
        val command=command();assertTrue(service.grade(command) is GradeResult.Recorded)
        val result=service.undo(session,command.operationId)
        assertTrue(result is UndoResult.Restored)
        val restored=(result as UndoResult.Restored).schedule
        assertEquals(before.copy(revision=2),restored)
        assertEquals(20,NewCardQuota.remaining(repo.observeSchedules().first(),AppSettings(),clock.instant(),ZoneOffset.UTC))
        assertNotNull(repo.observeLogs().first().single().undoneAt)
        assertEquals(UndoResult.Unavailable,service.undo(session,command.operationId))
    }
    @Test fun wrongSessionAndOutdatedStateCannotUndo()=runBlocking {
        val command=command();assertTrue(service.grade(command) is GradeResult.Recorded)
        assertEquals(UndoResult.Unavailable,service.undo("different-session",command.operationId))
        val current=repo.candidates(deck.id).single().schedule
        db.dao().putSchedule(current.copy(revision=current.revision+1).toEntity())
        assertEquals(UndoResult.Unavailable,service.undo(session,command.operationId))
    }
    @Test fun cannotUndoEarlierLogAfterUndoingLatest()=runBlocking {
        val one=command();service.grade(one)
        val second=repo.saveCard(null,deck.id,"two","二","")
        val two=command(cardId=second.id);service.grade(two)
        assertTrue(service.undo(session,two.operationId) is UndoResult.Restored)
        assertEquals(UndoResult.Unavailable,service.undo(session,one.operationId))
    }
}
