package com.xunji.flashcards.backup

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.xunji.core.backup.*
import com.xunji.core.model.*
import com.xunji.core.scheduling.FsrsScheduler
import com.xunji.flashcards.data.*
import com.xunji.flashcards.study.*
import java.io.*
import java.time.*
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class) @Config(sdk=[28])
class BackupServiceTest {
    private val clock=Clock.fixed(Instant.parse("2026-10-09T08:00:00Z"),ZoneOffset.UTC)
    private lateinit var db:XunjiDatabase
    private lateinit var repo:RoomStudyRepository
    private lateinit var service:BackupService
    @Before fun setup() { db=Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(),XunjiDatabase::class.java).allowMainThreadQueries().build();repo=RoomStudyRepository(db,clock);service=BackupService(db,clock) }
    @After fun close() { db.close() }
    private suspend fun seed() {
        val deck=repo.createDeck("备份");val card=repo.saveCard(null,deck.id,"word","词","")
        val review=ReviewService(db,FsrsScheduler(),clock) { ZoneOffset.UTC }
        val session=UUID.randomUUID().toString();val op=UUID.randomUUID().toString()
        assertTrue(review.grade(GradeCommand(op,session,card.id,0,Rating.GOOD,1000)) is GradeResult.Recorded)
        review.undo(session,op)
        assertTrue(review.grade(GradeCommand(UUID.randomUUID().toString(),session,card.id,2,Rating.EASY,1000)) is GradeResult.Recorded)
        repo.saveSettings(AppSettings(7,ThemeMode.DARK))
    }
    @Test fun exportAndReplacePreserveEverything()=runBlocking {
        seed();val before=service.export();val prepared=service.prepare(ByteArrayInputStream(BackupCodec.encode(before).toByteArray()))
        repo.deleteDeck(before.decks.single().id);repo.createDeck("different")
        service.replace(prepared);assertEquals(before,service.export())
        assertEquals(2,before.reviewLogs.size);assertNotNull(before.reviewLogs.first { it.undoneAt!=null })
    }
    @Test fun readFailuresNeverMutateDatabase()=runBlocking {
        seed();val before=service.export()
        val broken=object:InputStream() { override fun read():Int=throw IOException("read interrupted") }
        try { service.prepare(broken);fail() } catch(_:IOException) {}
        try { service.prepare(ByteArrayInputStream("{}".toByteArray()));fail() } catch(_:IllegalArgumentException) {}
        assertEquals(before,service.export())
    }
    @Test fun rejectsOversizeStreamBeforeDecode()=runBlocking {
        val oversized=object:InputStream() {
            var remaining=50L*1024*1024+1
            override fun read():Int=if(remaining-->0) 32 else -1
            override fun read(b:ByteArray,off:Int,len:Int):Int { if(remaining<=0)return -1;val n=minOf(len.toLong(),remaining).toInt();b.fill(32,off,off+n);remaining-=n;return n }
        }
        try { service.prepare(oversized);fail() } catch(e:IllegalArgumentException) { assertTrue(e.message.orEmpty().contains("50")) }
    }
    @Test fun restoreWriteFailureRollsBackOldData()=runBlocking {
        seed();val before=service.export();val prepared=service.prepare(ByteArrayInputStream(BackupCodec.encode(before).toByteArray()))
        db.openHelper.writableDatabase.execSQL("CREATE TRIGGER fail_restore BEFORE INSERT ON cards BEGIN SELECT RAISE(ABORT, 'test restore failure'); END")
        var failed=false;try { service.replace(prepared) } catch(_:Exception) { failed=true }
        assertTrue(failed);assertEquals(before,service.export())
    }
}
