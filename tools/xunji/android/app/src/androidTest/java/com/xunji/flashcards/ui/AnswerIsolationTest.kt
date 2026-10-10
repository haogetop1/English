package com.xunji.flashcards.ui

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.xunji.core.model.*
import com.xunji.core.scheduling.FsrsScheduler
import com.xunji.flashcards.ui.screens.ReviewScreen
import com.xunji.flashcards.ui.theme.XunjiTheme
import java.time.Instant
import org.junit.*

class AnswerIsolationTest {
 @get:Rule val compose=createComposeRule()
 @Test fun nextQuestionNeverShowsItsAnswerDuringTransition() {
  val now=Instant.now();val a=Flashcard("a","deck","A","ANSWER-A","EXAMPLE-A",now.toEpochMilli(),now.toEpochMilli());val b=a.copy(id="b",front="B",back="SECRET-B",example="EXAMPLE-B")
  val schedule=CardSchedule("a",Phase.NEW,null,null,null,now.toEpochMilli(),null,null,0)
  val candidate=CardCandidate(a,schedule,"deck");val next=CardCandidate(b,schedule.copy(cardId="b"),"deck")
  var state:ReviewUiState by mutableStateOf(ReviewUiState.Answer(candidate,"session",FsrsScheduler().preview(schedule,now)))
  compose.mainClock.autoAdvance=false
  compose.setContent {XunjiTheme {Surface(color=MaterialTheme.colorScheme.background) {ReviewScreen(state,false,{}, {}, {}, {}, {})}}}
  compose.onNodeWithText("ANSWER-A").assertExists()
  compose.runOnIdle {state=ReviewUiState.Question(next,"session")}
  compose.mainClock.advanceTimeByFrame()
  compose.onNodeWithTag("card-front").assertTextEquals("B")
  compose.onNodeWithText("SECRET-B").assertDoesNotExist()
  compose.onNodeWithText("EXAMPLE-B").assertDoesNotExist()
 }
}
