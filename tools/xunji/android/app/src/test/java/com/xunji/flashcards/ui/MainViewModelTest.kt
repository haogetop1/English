package com.xunji.flashcards.ui
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.ZoneOffset
@RunWith(RobolectricTestRunner::class) @Config(sdk=[28])
class MainViewModelTest:UiTestBase() {
 @Test fun reflectsCardsQuotaAndContentUpdates()=runBlocking {
  val v=MainViewModel(repo,clock);v.refresh(clock.instant(),ZoneOffset.UTC)
  val s=withTimeout(5000) { v.state.first { !it.loading } };assertEquals(1,s.availableNew);assertEquals(0,s.due)
  repo.saveCard(card.id,deck.id,"updated","词","")
  withTimeout(5000) { v.state.first { it.cards.single().card.front=="updated" } };Unit
 }
}
