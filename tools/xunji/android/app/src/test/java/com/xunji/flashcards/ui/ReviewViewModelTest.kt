package com.xunji.flashcards.ui
import androidx.lifecycle.SavedStateHandle
import com.xunji.core.model.*
import com.xunji.core.scheduling.FsrsScheduler
import com.xunji.flashcards.study.*
import com.xunji.flashcards.data.StudyRepository
import com.xunji.flashcards.data.RoomStudyRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.ZoneOffset
import java.time.Clock
import java.time.Duration
@RunWith(RobolectricTestRunner::class) @Config(sdk=[28])
class ReviewViewModelTest:UiTestBase() {
 private fun vm(actions:ReviewActions=service,handle:SavedStateHandle=SavedStateHandle())=ReviewViewModel(repo,actions,FsrsScheduler(),handle,clock,{ZoneOffset.UTC},{1000L})
 private suspend inline fun <reified T:ReviewUiState> await(v:ReviewViewModel):T=withTimeout(5000) { v.state.first { it is T } as T }
 @Test fun requiresRevealAndPreventsDoubleSubmit()=runBlocking {
  val v=vm();v.start(deck.id);await<ReviewUiState.Question>(v)
  v.grade(Rating.GOOD);assertTrue(repo.observeLogs().first().isEmpty())
  v.reveal();assertTrue(v.state.value is ReviewUiState.Answer)
  v.grade(Rating.GOOD);v.grade(Rating.GOOD);await<ReviewUiState.Complete>(v)
  assertEquals(1,repo.observeLogs().first().size);v.finish()
 }
 @Test fun preservesAnswerOnFailureAndRetriesSameOperation()=runBlocking {
  val commands=mutableListOf<GradeCommand>();var fail=true
  val action=object:ReviewActions {
   override suspend fun grade(c:GradeCommand):GradeResult { commands+=c;if(fail) { fail=false;throw java.io.IOException("failure") };return service.grade(c) }
   override suspend fun undo(s:String,l:String)=service.undo(s,l)
  }
  val v=vm(action);v.start(deck.id);await<ReviewUiState.Question>(v);v.reveal();v.grade(Rating.GOOD)
  val a=withTimeout(5000) { v.state.first { it is ReviewUiState.Answer && it.error!=null } } as ReviewUiState.Answer
  assertEquals(card.id,a.candidate.card.id);v.grade(Rating.GOOD);await<ReviewUiState.Complete>(v)
  assertEquals(commands[0],commands[1]);assertEquals(1,repo.observeLogs().first().size);v.finish()
 }
 @Test fun undoShowsFrontAndCannotUndoAgain()=runBlocking {
  val v=vm();v.start(deck.id);await<ReviewUiState.Question>(v);v.reveal();v.grade(Rating.EASY);await<ReviewUiState.Complete>(v)
  assertTrue(v.canUndo.value);v.undo();await<ReviewUiState.Question>(v);assertFalse(v.canUndo.value)
  assertNotNull(repo.observeLogs().first().single().undoneAt);v.undo();assertTrue(v.state.value is ReviewUiState.Question);v.finish()
 }
 @Test fun staleAndDeletedCardsRefreshQueue()=runBlocking {
  val v=vm();v.start(deck.id);await<ReviewUiState.Question>(v);v.reveal();repo.deleteCard(card.id);v.grade(Rating.GOOD);await<ReviewUiState.Complete>(v)
  assertTrue(repo.observeLogs().first().isEmpty());v.finish()
 }
 @Test fun rotationRetainsStateWhileNewProcessRegeneratesQueue()=runBlocking {
  val handle=SavedStateHandle();val v=vm(handle=handle);v.start(deck.id);await<ReviewUiState.Question>(v);v.reveal()
  v.setForeground(false);v.setForeground(true)
  val answer=await<ReviewUiState.Answer>(v);assertEquals(card.id,answer.candidate.card.id);assertEquals(true,handle.get<Boolean>("revealed"))
  v.finish();v.grade(Rating.GOOD);assertTrue(repo.observeLogs().first().isEmpty())
  val fresh=vm(handle=handle);fresh.start(deck.id);await<ReviewUiState.Question>(fresh);fresh.finish()
 }
 @Test fun delayedRefreshCannotHideRevealedAnswer()=runBlocking {
  val entered=CompletableDeferred<Unit>();val release=CompletableDeferred<Unit>();var hold=false
  val repository=object:StudyRepository by repo {
   override suspend fun candidates(deckId:String?):List<CardCandidate> {
    val snapshot=repo.candidates(deckId)
    if(hold) {entered.complete(Unit);release.await()}
    return snapshot
   }
  }
  val v=ReviewViewModel(repository,service,FsrsScheduler(),clock=clock)
  v.start(deck.id);await<ReviewUiState.Question>(v);hold=true;v.refresh();withTimeout(5000) {entered.await()}
  v.reveal();release.complete(Unit);yield()
  assertTrue("refresh must not revert answer to question",v.state.value is ReviewUiState.Answer);v.finish()
 }
 @Test fun refreshFromEndedSessionCannotReplaceNewSession()=runBlocking {
  val entered=CompletableDeferred<Unit>();val release=CompletableDeferred<Unit>();var calls=0
  val secondDeck=repo.createDeck("second");val second=repo.saveCard(null,secondDeck.id,"two","二","")
  val repository=object:StudyRepository by repo {
   override suspend fun candidates(deckId:String?):List<CardCandidate> {
    val snapshot=repo.candidates(deckId)
    calls++
    if(calls==2)withContext(NonCancellable) {entered.complete(Unit);release.await()}
    return snapshot
   }
  }
  val v=ReviewViewModel(repository,service,FsrsScheduler(),clock=clock)
  v.start(deck.id);await<ReviewUiState.Question>(v);v.refresh();withTimeout(5000) {entered.await()}
  v.start(secondDeck.id);val fresh=await<ReviewUiState.Question>(v);assertEquals(second.id,fresh.candidate.card.id)
  release.complete(Unit);yield()
  assertEquals("old success must not overwrite fresh session",second.id,(v.state.value as ReviewUiState.Question).candidate.card.id);v.finish()
 }
 @Test fun delayedRefreshCannotOverwriteSubmittingOrAdvanceBackwards()=runBlocking {
  val refreshEntered=CompletableDeferred<Unit>();val refreshRelease=CompletableDeferred<Unit>()
  val gradeEntered=CompletableDeferred<Unit>();val gradeRelease=CompletableDeferred<Unit>();var calls=0
  val second=RoomStudyRepository(db,Clock.offset(clock,Duration.ofSeconds(1))).saveCard(null,deck.id,"second","第二","")
  val repository=object:StudyRepository by repo {
   override suspend fun candidates(deckId:String?):List<CardCandidate> {
    val snapshot=repo.candidates(deckId);calls++
    if(calls==2)withContext(NonCancellable) {refreshEntered.complete(Unit);refreshRelease.await()}
    return snapshot
   }
  }
  val action=object:ReviewActions {
   override suspend fun grade(c:GradeCommand):GradeResult {gradeEntered.complete(Unit);gradeRelease.await();return service.grade(c)}
   override suspend fun undo(s:String,l:String)=service.undo(s,l)
  }
  val v=ReviewViewModel(repository,action,FsrsScheduler(),clock=clock)
  v.start(deck.id);assertEquals(card.id,await<ReviewUiState.Question>(v).candidate.card.id);v.reveal();v.refresh();withTimeout(5000) {refreshEntered.await()}
  v.grade(Rating.EASY);withTimeout(5000) {gradeEntered.await()};refreshRelease.complete(Unit);yield()
  assertTrue("refresh must not enable scoring while a write is pending",v.state.value is ReviewUiState.Submitting)
  gradeRelease.complete(Unit);assertEquals(second.id,await<ReviewUiState.Question>(v).candidate.card.id)
  assertEquals(1,repo.observeLogs().first().size);v.finish()
 }
}
