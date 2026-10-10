package com.xunji.flashcards.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.xunji.core.model.*
import com.xunji.flashcards.ui.*
import com.xunji.flashcards.ui.components.*

@OptIn(ExperimentalLayoutApi::class)
@Composable fun SettingsScreen(state:SettingsUiState,viewModel:SettingsViewModel,onExport:()->Unit,onImport:()->Unit) {
 var limit by rememberSaveable { mutableStateOf(state.settings.newCardsPerDay.toString()) }
 var limitError by remember { mutableStateOf(false) }
 LaunchedEffect(state.settings.newCardsPerDay) { limit=state.settings.newCardsPerDay.toString() }
 LazyColumn(Modifier.fillMaxSize().imePadding(),contentPadding=PaddingValues(22.dp,18.dp,22.dp,28.dp),verticalArrangement=Arrangement.spacedBy(22.dp)) {
  item { SectionTitle("按你的节奏","让工具适合你，而不是催促你。") }
  if(state.error!=null)item { Notice(state.error,true) }
  if(state.message!=null)item { Notice(state.message) }
  item { Paper(Modifier.fillMaxWidth()) {
   Text("每天的新卡",style=MaterialTheme.typography.titleLarge);Notice("所有牌组合计。设为 0 可暂停新卡，到期复习仍然保留。")
   OutlinedTextField(limit,{limit=it;limitError=false},label={Text("每日新卡上限")},suffix={Text("张")},singleLine=true,keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number),isError=limitError,enabled=!state.busy,modifier=Modifier.fillMaxWidth().testTag("new-card-limit"),supportingText={Text(if(limitError)"请输入 0～100 的整数" else "0～100 张 · 默认 20 张")},shape=RoundedCornerShape(16.dp))
   PrimaryButton("保存学习设置",{ val n=limit.toIntOrNull();if(n==null || n !in 0..100)limitError=true else viewModel.saveSettings(state.settings.copy(newCardsPerDay=n)) },enabled=!state.busy,tag="save-settings")
  } }
  item { Paper(Modifier.fillMaxWidth()) {
   Text("界面主题",style=MaterialTheme.typography.titleLarge)
   FlowRow(horizontalArrangement=Arrangement.spacedBy(10.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) { ThemeMode.entries.forEach { mode->FilterChip(selected=state.settings.themeMode==mode,onClick={viewModel.saveSettings(state.settings.copy(themeMode=mode))},enabled=!state.busy,label={Text(when(mode){ThemeMode.SYSTEM->"跟随系统";ThemeMode.LIGHT->"浅色";ThemeMode.DARK->"深色"})},modifier=Modifier.heightIn(min=48.dp).testTag("theme-${mode.name.lowercase()}")) } }
  } }
  item { Paper(Modifier.fillMaxWidth()) {
   Text("把记忆好好保存",style=MaterialTheme.typography.titleLarge);Notice("导出循记 JSON 备份，包含卡片、复习进度和设置。请将重要备份保存在可靠的位置。")
   PrimaryButton(if(state.busy)"正在处理…" else "导出备份",onExport,enabled=!state.busy,tag="export-backup")
   OutlinedButton(onImport,Modifier.fillMaxWidth().heightIn(min=56.dp).testTag("import-backup"),enabled=!state.busy,shape=RoundedCornerShape(18.dp)) { Text("从备份恢复") }
   Notice("恢复会替换当前全部数据。仅支持循记备份格式。")
  } }
  item { Eyebrow("循记 0.1.0");Spacer(Modifier.height(8.dp));Notice("独立、离线、本地保存。\n给知识一点时间，也给自己一点空间。") }
 }
 val prepared=state.prepared
 if(prepared!=null)AlertDialog(onDismissRequest=viewModel::cancelRestore,title={Text("替换当前全部数据？")},text={Column(verticalArrangement=Arrangement.spacedBy(12.dp)) { Text("备份包含 ${prepared.deckCount} 个牌组、${prepared.cardCount} 张卡片、${prepared.logCount} 条评分记录。");Text("当前牌组、卡片和记录将被替换，无法撤回。建议先导出当前数据。");if(state.error!=null)Notice(state.error,true) }},confirmButton={TextButton(viewModel::confirmRestore,enabled=!state.busy,modifier=Modifier.testTag("restore-confirm")) {Text(if(state.busy)"恢复中…" else "确认替换",color=MaterialTheme.colorScheme.error)}},dismissButton={TextButton(viewModel::cancelRestore,enabled=!state.busy,modifier=Modifier.testTag("restore-cancel")) {Text("取消")} })
}
