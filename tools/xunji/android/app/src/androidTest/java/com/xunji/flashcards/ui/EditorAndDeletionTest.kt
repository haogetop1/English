package com.xunji.flashcards.ui
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.xunji.flashcards.*
import kotlinx.coroutines.runBlocking
import org.junit.*
class EditorAndDeletionTest {
 @get:Rule val compose=createAndroidComposeRule<MainActivity>()
 @Before fun seed()=runBlocking { val c=(InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as XunjiApplication).container;c.database.clearAllTables();c.repository.createDeck("测试牌组");Unit }
 @Test fun validationDiscardAndDeckDeleteCancellation() {
  compose.onNodeWithTag("nav-decks").performClick()
  compose.waitUntil(10000) { compose.onAllNodesWithText("测试牌组").fetchSemanticsNodes().isNotEmpty() }
  compose.onNodeWithText("测试牌组").performClick();compose.onNodeWithTag("add-card").performClick()
  compose.onNodeWithTag("editor-front").performTextInput("hello");compose.onNodeWithTag("save-card").performClick()
  compose.onNodeWithText("请填写此项").assertIsDisplayed();compose.onNodeWithTag("editor-front").assertTextContains("hello")
  compose.onNodeWithTag("editor-exit").performClick();compose.onNodeWithText("继续编辑").performClick()
  compose.onNodeWithTag("editor-front").assertTextContains("hello")
  compose.onNodeWithTag("editor-exit").performClick();compose.onNodeWithText("放弃修改").performClick()
  compose.onNodeWithTag("page-back").performClick();compose.onNodeWithContentDescription("删除牌组").performClick()
  compose.onNodeWithText("将删除全部卡片和学习记录，无法撤回。").assertIsDisplayed()
  compose.onNodeWithText("取消").performClick();compose.onNodeWithText("测试牌组").assertIsDisplayed()
  compose.onNodeWithContentDescription("删除牌组").performClick();compose.onNodeWithText("确认删除").performClick()
  compose.waitUntil(10000) { compose.onAllNodesWithText("测试牌组").fetchSemanticsNodes().isEmpty() }
 }
}
