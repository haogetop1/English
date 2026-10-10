package com.xunji.flashcards.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.xunji.core.model.*
import com.xunji.flashcards.ui.*
import com.xunji.flashcards.ui.components.*
import kotlinx.coroutines.*

@Composable fun DeckDetailScreen(deckId:String,state:MainUiState,viewModel:MainViewModel,onEdit:(String?)->Unit,onReview:()->Unit) {
 val deck=state.decks.firstOrNull { it.deck.id==deckId };val cards=state.cards.filter { it.card.deckId==deckId }
 var deleting by rememberSaveable { mutableStateOf<String?>(null) };var error by remember { mutableStateOf<String?>(null) };var busy by remember { mutableStateOf(false) };val scope=rememberCoroutineScope()
 if(state.loading){Busy();return}
 if(deck==null){ Box(Modifier.padding(22.dp)) { EmptyState("牌组已不存在","请返回牌组列表，选择其它内容。") };return }
 LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(22.dp,18.dp,22.dp,28.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
  item { SectionTitle(deck.deck.name,"${cards.size} 张卡片 · ${deck.due} 张到期复习") }
  item { PrimaryButton("复习这个牌组",onReview,enabled=cards.isNotEmpty(),tag="review-start") }
  item { Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) { Text("所有卡片",style=MaterialTheme.typography.titleLarge);IconAction("add","新增卡片",{onEdit(null)},"add-card") } }
  if(cards.isEmpty())item { EmptyState("留下一点新知识","正面写问题，背面写答案。例句让记忆有上下文。","创建第一张卡",{onEdit(null)}) }
  items(cards,key={it.card.id}) { candidate ->
   Paper(Modifier.fillMaxWidth().clickable { onEdit(candidate.card.id) }) {
    Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
     Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(8.dp)) {
      Text(candidate.card.front,style=MaterialTheme.typography.titleMedium,maxLines=2,overflow=TextOverflow.Ellipsis)
      val label=when(candidate.schedule.phase) { Phase.NEW->"新卡";Phase.LEARNING,Phase.RELEARNING->"学习中";Phase.REVIEW->"已学习" }
      Notice(label+if(candidate.schedule.phase==Phase.NEW)" · 尚未开始" else " · ${localTime(candidate.schedule.dueAt)}")
     }
     IconAction("delete","删除卡片",{deleting=candidate.card.id;error=null})
    }
   }
  }
 }
 if(deleting!=null)AlertDialog(onDismissRequest={if(!busy)deleting=null},title={ Text("删除卡片？") },text={ Column { Text("这张卡片及其学习记录将被删除，无法撤回。");if(error!=null)Notice(error!!,true) } },confirmButton={ TextButton({ if(!busy){busy=true;scope.launch { try { viewModel.deleteCard(deleting!!);deleting=null } catch(e:CancellationException){throw e} catch(_:Exception){error="删除未完成，请重试"} finally {busy=false} }} },enabled=!busy) { Text("确认删除",color=MaterialTheme.colorScheme.error) } },dismissButton={ TextButton({deleting=null},enabled=!busy) { Text("取消") } })
}
