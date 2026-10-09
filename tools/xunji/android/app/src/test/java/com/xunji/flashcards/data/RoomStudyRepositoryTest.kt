package com.xunji.flashcards.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.xunji.core.model.*
import com.xunji.core.scheduling.FsrsScheduler
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28])
class RoomStudyRepositoryTest {
    private val context=ApplicationProvider.getApplicationContext<Context>()
    private val clock=Clock.fixed(Instant.parse("2026-10-09T08:00:00Z"),ZoneOffset.UTC)
    private lateinit var db:XunjiDatabase
    private lateinit var repository:RoomStudyRepository
    @Before fun setup() { db=Room.inMemoryDatabaseBuilder(context,XunjiDatabase::class.java).allowMainThreadQueries().build(); repository=RoomStudyRepository(db,clock) }
    @After fun teardown() { db.close() }

    @Test fun createsCardAndScheduleTogether()=runBlocking {
        val deck=repository.createDeck(" 英语 ")
        val card=repository.saveCard(null,deck.id,"line one\nline two 😀","答案\n解释", "Example.")
        assertEquals(1,repository.candidates(deck.id).size)
        val stored=repository.candidates(deck.id).single()
        assertEquals("英语",stored.deckName)
        assertEquals(card,stored.card)
        assertEquals(Phase.NEW,stored.schedule.phase)
        assertNull(stored.schedule.firstReviewedAt)
        assertEquals(0L,stored.schedule.revision)
    }
    @Test fun editingPreservesScheduleAndLogs()=runBlocking {
        val deck=repository.createDeck("英语")
        val card=repository.saveCard(null,deck.id,"before","answer","")
        assertEquals(1,repository.candidates(deck.id).size)
        val before=repository.candidates(deck.id).single().schedule
        val reviewed=FsrsScheduler().review(before,Rating.GOOD,clock.instant())
        db.dao().putSchedule(reviewed.toEntity())
        val log=ReviewLog("log",card.id,deck.id,"session",Rating.GOOD,clock.millis(),1000,before,reviewed,null)
        db.dao().insertLog(log.toEntity())
        repository.saveCard(card.id,deck.id,"after","new answer","")
        assertEquals(reviewed,repository.candidates(deck.id).single().schedule)
        assertEquals(listOf(log),repository.observeLogs().first())
    }
    @Test fun deletionCascadesAndDuplicateNamesAreAllowed()=runBlocking {
        val one=repository.createDeck("英语");val two=repository.createDeck("英语")
        assertNotEquals(one.id,two.id)
        val card=repository.saveCard(null,one.id,"word","answer","")
        assertEquals(1,repository.candidates(one.id).size)
        val before=repository.candidates(one.id).single().schedule
        val after=FsrsScheduler().review(before,Rating.GOOD,clock.instant())
        db.dao().insertLog(ReviewLog("log",card.id,one.id,"session",Rating.GOOD,clock.millis(),1,before,after,null).toEntity())
        repository.deleteDeck(one.id)
        assertTrue(repository.candidates(one.id).isEmpty())
        assertTrue(repository.observeSchedules().first().isEmpty())
        assertTrue(repository.observeLogs().first().isEmpty())
        assertEquals(listOf(two),repository.observeDecks().first())
    }
    @Test fun invalidInputCannotWriteData()=runBlocking {
        val deck=repository.createDeck("英语")
        try { repository.saveCard(null,deck.id," ","answer","");fail("expected rejection") } catch(_:IllegalArgumentException) {}
        assertTrue(repository.candidates(deck.id).isEmpty())
    }
    @Test fun settingsPersistAndValidate()=runBlocking {
        assertEquals(AppSettings(),repository.observeSettings().first())
        val settings=AppSettings(0,ThemeMode.DARK)
        repository.saveSettings(settings)
        assertEquals(settings,repository.observeSettings().first())
        try { repository.saveSettings(AppSettings(101));fail("expected rejection") } catch(_:IllegalArgumentException) {}
        assertEquals(settings,repository.observeSettings().first())
    }
    @Test fun fileDatabaseSurvivesCloseAndReopen()=runBlocking {
        val name="persistence-${System.nanoTime()}.db"
        val first=Room.databaseBuilder(context,XunjiDatabase::class.java,name).build()
        val repo=RoomStudyRepository(first,clock)
        val deck=repo.createDeck("长期记忆")
        val card=repo.saveCard(null,deck.id,"remember","记住","")
        first.close()
        val reopened=Room.databaseBuilder(context,XunjiDatabase::class.java,name).build()
        try { assertEquals(listOf(card),RoomStudyRepository(reopened,clock).candidates(deck.id).map { it.card }) }
        finally { reopened.close();context.deleteDatabase(name) }
    }
}
