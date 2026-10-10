package com.xunji.flashcards.ui
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
@RunWith(RobolectricTestRunner::class) @Config(sdk=[28])
class EditorViewModelTest:UiTestBase() {
 @Test fun requiredFieldsPreserveInputAndPreventDuplicateSave()=runBlocking {
  val v=EditorViewModel(repo,deck.id);v.update("front","hello");v.save()
  assertEquals("hello",v.state.value.front);assertTrue(v.state.value.errors.containsKey("back"))
  v.update("back","你好");v.save();v.save();withTimeout(5000) { v.state.first { it.saved } }
  assertEquals(2,repo.candidates(deck.id).size)
 }
 @Test fun editLoadsAndPreservesSchedule()=runBlocking {
  val before=repo.candidates(deck.id).single().schedule
  val v=EditorViewModel(repo,deck.id,card.id);withTimeout(5000) { v.state.first { !it.loading && it.front=="word" } }
  v.update("front","updated");v.save();withTimeout(5000) { v.state.first { it.saved } }
  assertEquals(before,repo.candidates(deck.id).single().schedule)
 }
}
