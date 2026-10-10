package com.xunji.flashcards.ui

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.*
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.test.platform.app.InstrumentationRegistry
import com.xunji.core.model.AppSettings
import com.xunji.flashcards.AppContainer
import com.xunji.flashcards.data.StudyRepository
import java.io.IOException
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.junit.*

class InitializationRecoveryTest {
 @get:Rule val compose=createComposeRule()
 private val owner=object:ViewModelStoreOwner {override val viewModelStore=ViewModelStore()}
 private lateinit var container:AppContainer
 @After fun clear() {InstrumentationRegistry.getInstrumentation().runOnMainSync {owner.viewModelStore.clear()};if(::container.isInitialized)container.database.close()}
 @Test fun databaseFailureOffersOnlyRecoveryAndRetryRestartsNavigation() {
  container=AppContainer(InstrumentationRegistry.getInstrumentation().targetContext)
  var fail=true
  val repository=object:StudyRepository by container.repository {
   override fun observeSettings()=flow {if(fail)throw IOException("database unavailable");emit(AppSettings())}
  }
  val main=MainViewModel(repository)
  val settings=SettingsViewModel(repository,container.backup,container.files) {}
  val review=ReviewViewModel(container.repository,container.review,container.scheduler)
  val provider=ViewModelProvider(owner,object:ViewModelProvider.Factory {
   @Suppress("UNCHECKED_CAST")
   override fun <T:ViewModel> create(modelClass:Class<T>):T=when(modelClass) {
    MainViewModel::class.java->main;SettingsViewModel::class.java->settings;ReviewViewModel::class.java->review;else->error("unexpected model")
   } as T
  })
  provider[MainViewModel::class.java];provider[SettingsViewModel::class.java];provider[ReviewViewModel::class.java]
  runBlocking {withTimeout(10000) {main.state.first {it.error!=null};settings.state.first {it.initializationError!=null}}}
  compose.setContent {CompositionLocalProvider(LocalViewModelStoreOwner provides owner) {XunjiApp(container)}}
  compose.onNodeWithText("重新读取").assertIsDisplayed()
  compose.onNodeWithTag("nav-decks").assertDoesNotExist()
  compose.onNodeWithTag("open-settings").assertDoesNotExist()
  fail=false;compose.onNodeWithText("重新读取").performClick()
  compose.waitUntil(10000) {compose.onAllNodesWithTag("nav-decks").fetchSemanticsNodes().isNotEmpty()}
  compose.onNodeWithTag("nav-decks").performClick();compose.onNodeWithTag("add-deck").assertIsDisplayed()
 }
}
