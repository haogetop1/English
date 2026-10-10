package com.xunji.flashcards.ui

import android.graphics.Bitmap
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.*
import androidx.test.platform.app.InstrumentationRegistry
import com.xunji.core.model.*
import com.xunji.core.scheduling.FsrsScheduler
import com.xunji.flashcards.ui.screens.ReviewScreen
import com.xunji.flashcards.ui.theme.XunjiTheme
import java.io.File
import java.time.Instant
import org.junit.*

class AccessibilityLayoutTest {
 @get:Rule val compose=createComposeRule()
 @OptIn(ExperimentalTestApi::class)
 @Test fun rendersLightDarkAndLargeFontsAtNarrowWidth() {
  val now=Instant.now().toEpochMilli();val card=Flashcard("card","deck","resilient","有韧性的；能迅速恢复的。","She remained resilient in the face of change.",now,now)
  val schedule=CardSchedule("card",Phase.NEW,null,null,null,now,null,null,0)
  val candidate=CardCandidate(card,schedule,"雅思 · 核心词汇")
  var mode by mutableStateOf(ThemeMode.LIGHT);var scale by mutableFloatStateOf(1f);var longText by mutableStateOf(false)
  compose.setContent {CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density,scale)) {XunjiTheme(mode) {Surface(color=MaterialTheme.colorScheme.background) {Box(Modifier.width(320.dp).fillMaxHeight()) {val c=if(longText)candidate.copy(card=card.copy(front="word ".repeat(400),back="释义 ".repeat(3333),example="例句 ".repeat(1333))) else candidate;ReviewScreen(ReviewUiState.Answer(c,"session",FsrsScheduler().preview(schedule,Instant.ofEpochMilli(now))),false,{}, {},{}, {}, {})}}}}}
  fun screenshot(name:String) { compose.waitForIdle();val file=File(InstrumentationRegistry.getInstrumentation().targetContext.filesDir,"screenshots/$name.png");file.parentFile!!.mkdirs();file.outputStream().use { compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG,100,it) } }
  screenshot("review-light-320")
  mode=ThemeMode.DARK;screenshot("review-dark-320")
  scale=1.5f;screenshot("review-dark-font150")
  scale=2f;screenshot("review-dark-font200")
  listOf("rating-again","rating-hard","rating-good","rating-easy").forEach {compose.onNodeWithTag(it).assertIsDisplayed().assertIsEnabled()}
  longText=true;screenshot("review-long-text-font200")
  compose.onNodeWithTag("rating-good").assertIsDisplayed()
 }
}
