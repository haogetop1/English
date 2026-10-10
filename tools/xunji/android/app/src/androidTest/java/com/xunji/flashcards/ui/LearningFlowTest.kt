package com.xunji.flashcards.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.xunji.flashcards.*
import org.junit.*

class LearningFlowTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    @Before fun clear() { (InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as XunjiApplication).container.database.clearAllTables() }
    private fun node(tag:String)=compose.onNodeWithTag(tag)
    @Test fun createReviewUndoAndFinish() {
        node("nav-decks").performClick();node("add-deck").performClick()
        node("deck-name").performTextInput("雅思词汇");node("save-deck").performClick()
        compose.waitUntil(10000) { compose.onAllNodesWithText("雅思词汇").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("雅思词汇").performClick();node("add-card").performClick()
        node("editor-front").performTextInput("resilient");node("editor-back").performTextInput("有韧性的");node("editor-example").performTextInput("Be resilient.")
        node("save-card").performClick()
        compose.waitUntil(10000) { compose.onAllNodesWithTag("review-start").fetchSemanticsNodes().isNotEmpty() }
        node("review-start").performClick()
        compose.waitUntil(10000) { compose.onAllNodesWithTag("card-front").fetchSemanticsNodes().isNotEmpty() }
        node("card-back").assertDoesNotExist();node("rating-good").assertDoesNotExist()
        node("reveal-answer").performClick();node("card-back").assertIsDisplayed();node("rating-good").assertIsEnabled()
        compose.activityRule.scenario.recreate()
        node("card-back").assertIsDisplayed()
        node("rating-easy").performClick()
        compose.waitUntil(10000) { compose.onAllNodesWithTag("review-complete").fetchSemanticsNodes().isNotEmpty() }
        node("undo-rating").performClick()
        compose.waitUntil(10000) { compose.onAllNodesWithTag("card-front").fetchSemanticsNodes().isNotEmpty() }
        node("card-back").assertDoesNotExist();node("reveal-answer").performClick();node("rating-easy").performClick()
        compose.waitUntil(10000) { compose.onAllNodesWithTag("review-complete").fetchSemanticsNodes().isNotEmpty() }
    }
}
