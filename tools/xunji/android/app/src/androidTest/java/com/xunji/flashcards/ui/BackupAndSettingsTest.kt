package com.xunji.flashcards.ui

import android.net.Uri
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.xunji.core.backup.*
import com.xunji.flashcards.*
import com.xunji.flashcards.backup.*
import com.xunji.flashcards.ui.screens.SettingsScreen
import com.xunji.flashcards.ui.theme.XunjiTheme
import java.io.*
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*

class BackupAndSettingsTest {
 @get:Rule val compose=createComposeRule()
 @Test fun restorePreviewRequiresConfirmationAndExportFailureIsVisible()=runBlocking {
  val c=(InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as XunjiApplication).container
  c.database.clearAllTables();val deck=c.repository.createDeck("备份测试");c.repository.saveCard(null,deck.id,"word","词","")
  val backup=c.backup.export();val bytes=BackupCodec.encode(backup).toByteArray();var ended=0
  val files=object:FileGateway { override suspend fun read(uri:Uri)=ByteArrayInputStream(bytes);override suspend fun write(uri:Uri,document:BackupDocument) {throw IOException("disk full")} }
  val vm=SettingsViewModel(c.repository,c.backup,files) {ended++};val uri=Uri.parse("content://test/backup")
  compose.setContent {val s by vm.state.collectAsState();XunjiTheme {SettingsScreen(s,vm,{vm.export(uri)},{vm.prepareRestore(uri)})}}
  compose.onNodeWithTag("import-backup").performScrollTo().performClick()
  compose.waitUntil(10000) {vm.state.value.prepared!=null && !vm.state.value.busy}
  compose.onNodeWithText("备份包含 1 个牌组、1 张卡片、0 条评分记录。").assertIsDisplayed()
  compose.onNodeWithTag("restore-cancel").performClick();assertEquals(0,ended);assertEquals(backup,c.backup.export().copy(exportedAt=backup.exportedAt))
  compose.onNodeWithTag("import-backup").performScrollTo().performClick();compose.waitUntil(10000) {vm.state.value.prepared!=null && !vm.state.value.busy}
  compose.onNodeWithTag("restore-confirm").performClick();compose.waitUntil(10000) {ended==1 && !vm.state.value.busy}
  compose.onNodeWithTag("export-backup").performScrollTo().performClick();compose.waitUntil(10000) {vm.state.value.error!=null}
  compose.onNodeWithText("备份已导出").assertDoesNotExist();assertEquals(backup,c.backup.export().copy(exportedAt=backup.exportedAt));Unit
 }
}
