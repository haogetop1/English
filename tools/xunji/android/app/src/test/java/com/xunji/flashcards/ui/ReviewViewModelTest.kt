package com.xunji.flashcards.ui
import androidx.lifecycle.SavedStateHandle
import com.xunji.core.model.*
import com.xunji.core.scheduling.FsrsScheduler
import com.xunji.flashcards.study.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.ZoneOffset
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
}
