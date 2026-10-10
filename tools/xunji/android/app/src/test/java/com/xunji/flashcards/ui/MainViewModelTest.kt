package com.xunji.flashcards.ui
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import com.xunji.flashcards.data.StudyRepository
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.ZoneOffset
import com.xunji.core.model.*
import com.xunji.flashcards.study.*
import java.util.UUID
@RunWith(RobolectricTestRunner::class) @Config(sdk=[28])
class MainViewModelTest:UiTestBase() {
 @Test fun reflectsCardsQuotaAndContentUpdates()=runBlocking {
  val v=MainViewModel(repo,clock);v.refresh(clock.instant(),ZoneOffset.UTC)
  val s=withTimeout(5000) { v.state.first { !it.loading } };assertEquals(1,s.availableNew);assertEquals(0,s.due)
  repo.saveCard(card.id,deck.id,"updated","词","")
  withTimeout(5000) { v.state.first { it.cards.single().card.front=="updated" } };Unit
 }
 @Test fun actualLearnedCountSurvivesQuotaReduction()=runBlocking {
  service.grade(GradeCommand(UUID.randomUUID().toString(),UUID.randomUUID().toString(),card.id,0,Rating.EASY,1000))
  repo.saveSettings(AppSettings(0))
  val v=MainViewModel(repo,clock);v.refresh(clock.instant(),ZoneOffset.UTC)
  val s=withTimeout(5000) {v.state.first { !it.loading }}
  assertEquals(0,s.availableNew);assertEquals(1,s.learnedNew)
 }
 @Test fun retryRestartsDatabaseObservation()=runBlocking {
  var fail=true
  val repository=object:StudyRepository by repo {override fun observeSettings()=flow {if(fail)throw java.io.IOException("cannot open database");emit(AppSettings())} }
  val v=MainViewModel(repository,clock)
  withTimeout(5000) {v.state.first {it.error!=null}}
  fail=false;v.retryObservation()
  val recovered=withTimeout(5000) {v.state.first {!it.loading && it.error==null}}
  assertEquals(1,recovered.availableNew)
 }
}
