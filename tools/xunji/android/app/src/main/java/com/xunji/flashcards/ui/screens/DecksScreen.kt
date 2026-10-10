package com.xunji.flashcards.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.xunji.flashcards.ui.*
import com.xunji.flashcards.ui.components.*
import kotlinx.coroutines.*

@Composable fun DecksScreen(state:MainUiState,viewModel:MainViewModel,onDeck:(String)->Unit) {
 var editing by rememberSaveable { mutableStateOf<String?>(null) };var adding by rememberSaveable { mutableStateOf(false) };var deleting by rememberSaveable { mutableStateOf<String?>(null) }
 var name by rememberSaveable { mutableStateOf("") };var error by remember { mutableStateOf<String?>(null) };var busy by remember { mutableStateOf(false) };val scope=rememberCoroutineScope()
 LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(22.dp,18.dp,22.dp,28.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
  item { Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) { SectionTitle("记忆花园","${state.decks.size} 个牌组 · ${state.cards.size} 张卡片");IconAction("add","创建牌组",{ adding=true;name="";error=null },"add-deck") } }
  if(state.loading)item { Busy() }
  if(state.decks.isEmpty() && !state.loading)item { EmptyState("给记忆一个归处","将相似的内容放进同一个牌组，复习时更专注。","新建牌组",{ adding=true;name="";error=null }) }
  items(state.decks,key={ it.deck.id }) { summary ->
   Paper(Modifier.fillMaxWidth().clickable { onDeck(summary.deck.id) }) {
    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
     Mark();Column(Modifier.weight(1f)) { Text(summary.deck.name,style=MaterialTheme.typography.titleMedium);Notice("${summary.total} 张卡片") }
     IconAction("edit","重命名牌组",{ editing=summary.deck.id;name=summary.deck.name;error=null })
     IconAction("delete","删除牌组",{ deleting=summary.deck.id;error=null })
    }
    HorizontalDivider(color=MaterialTheme.colorScheme.outlineVariant)
    Row(horizontalArrangement=Arrangement.spacedBy(20.dp)) { Text("${summary.due} 张待复习",color=MaterialTheme.colorScheme.primary);Notice("${summary.newCards} 张新卡") }
   }
  }
 }
 if(adding || editing!=null)AlertDialog(onDismissRequest={ if(!busy){adding=false;editing=null} },title={ Text(if(adding)"新建牌组" else "重命名牌组") },text={ Column { OutlinedTextField(name,{ name=it;error=null },label={ Text("牌组名称") },singleLine=true,isError=error!=null,enabled=!busy,modifier=Modifier.testTag("deck-name"));if(error!=null)Notice(error!!,true) } },confirmButton={ TextButton(onClick={ if(!busy){busy=true;scope.launch { try { if(adding)viewModel.createDeck(name) else viewModel.renameDeck(editing!!,name);adding=false;editing=null } catch(e:CancellationException){throw e} catch(e:IllegalArgumentException){error=e.message ?: "请检查名称"} catch(_:Exception){error="保存未完成，请重试"} finally {busy=false} }} },enabled=!busy,modifier=Modifier.testTag("save-deck")) { Text(if(busy)"保存中…" else "保存") } },dismissButton={ TextButton({adding=false;editing=null},enabled=!busy) { Text("取消") } })
 if(deleting!=null)AlertDialog(onDismissRequest={if(!busy)deleting=null},title={ Text("删除牌组？") },text={ Column { Text("将删除全部卡片和学习记录，无法撤回。");if(error!=null)Notice(error!!,true) } },confirmButton={ TextButton({ if(!busy){busy=true;scope.launch { try { viewModel.deleteDeck(deleting!!);deleting=null } catch(e:CancellationException){throw e} catch(_:Exception){error="删除未完成，请重试"} finally {busy=false} }} },enabled=!busy) { Text("确认删除",color=MaterialTheme.colorScheme.error) } },dismissButton={ TextButton({deleting=null},enabled=!busy) { Text("取消") } })
}
