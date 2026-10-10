package com.xunji.flashcards.ui
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.xunji.core.model.*
import com.xunji.core.scheduling.FsrsScheduler
import com.xunji.flashcards.data.*
import com.xunji.flashcards.study.*
import java.time.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.*
@OptIn(ExperimentalCoroutinesApi::class)
abstract class UiTestBase {
 protected val clock=Clock.fixed(Instant.parse("2026-10-09T08:00:00Z"),ZoneOffset.UTC)
 protected lateinit var db:XunjiDatabase
 protected lateinit var repo:RoomStudyRepository
 protected lateinit var service:ReviewService
 protected lateinit var deck:Deck
 protected lateinit var card:Flashcard
 @Before fun setup()=runBlocking { Dispatchers.setMain(UnconfinedTestDispatcher());db=Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(),XunjiDatabase::class.java).allowMainThreadQueries().build();repo=RoomStudyRepository(db,clock);service=ReviewService(db,FsrsScheduler(),clock) { ZoneOffset.UTC };deck=repo.createDeck("雅思");card=repo.saveCard(null,deck.id,"word","词","example") }
 @After fun close() { db.close();Dispatchers.resetMain() }
}
