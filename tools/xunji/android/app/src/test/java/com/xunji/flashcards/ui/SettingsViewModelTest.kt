package com.xunji.flashcards.ui
import android.net.Uri
import com.xunji.core.model.*
import com.xunji.core.backup.*
import com.xunji.core.scheduling.FsrsScheduler
import com.xunji.flashcards.backup.*
import java.io.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import com.xunji.flashcards.data.StudyRepository
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
@RunWith(RobolectricTestRunner::class) @Config(sdk=[28])
class SettingsViewModelTest:UiTestBase() {
 @Test fun cancellationDoesNotReplaceAndConfirmationEndsReview()=runBlocking {
  val backup=BackupService(db,clock);val original=backup.export();val bytes=BackupCodec.encode(original).toByteArray()
  val gateway=object:FileGateway { override suspend fun read(uri:Uri)=ByteArrayInputStream(bytes);override suspend fun write(uri:Uri,document:BackupDocument) {} }
  val review=ReviewViewModel(repo,service,FsrsScheduler(),clock=clock);review.start(deck.id);withTimeout(5000) { review.state.first { it is ReviewUiState.Question } };review.reveal()
  var ended=false;val v=SettingsViewModel(repo,backup,gateway) { ended=true;review.finish() }
  val uri=Uri.parse("content://test/backup");v.prepareRestore(uri);withTimeout(5000) { v.state.first { it.prepared!=null } };v.cancelRestore()
  assertEquals(original,backup.export());assertFalse(ended)
  v.prepareRestore(uri);withTimeout(5000) { v.state.first { it.prepared!=null } };v.confirmRestore();withTimeout(5000) { v.state.first { it.message=="备份已恢复" } }
  assertTrue(ended);review.grade(Rating.GOOD);assertTrue(repo.observeLogs().first().isEmpty())
 }
 @Test fun savesSettingsAndReportsExportFailure()=runBlocking {
  val gateway=object:FileGateway { override suspend fun read(uri:Uri):InputStream=throw IOException();override suspend fun write(uri:Uri,document:BackupDocument) { throw IOException("disk full") } }
  val v=SettingsViewModel(repo,BackupService(db,clock),gateway) {}
  v.saveSettings(AppSettings(0,ThemeMode.DARK));withTimeout(5000) { repo.observeSettings().first { it.newCardsPerDay==0 } }
  v.export(Uri.parse("content://test/file"));withTimeout(5000) { v.state.first { it.error!=null } };assertNull(v.state.value.message)
 }
 @Test fun settingsObservationFailureIsRecoverableWithoutUncaughtException()=runBlocking {
  var fail=true
  val repository=object:StudyRepository by repo { override fun observeSettings()=flow {if(fail)throw IOException("database open failed");emit(AppSettings(4,ThemeMode.DARK))} }
  val gateway=object:FileGateway {override suspend fun read(uri:Uri):InputStream=throw IOException();override suspend fun write(uri:Uri,document:BackupDocument) {}}
  val v=SettingsViewModel(repository,BackupService(db,clock),gateway) {}
  withTimeout(5000) {v.state.first {it.initializationError!=null}}
  fail=false;v.retryObservation()
  val recovered=withTimeout(5000) {v.state.first {it.settings.newCardsPerDay==4}}
  assertNull(recovered.initializationError)
 }
}
