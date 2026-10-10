package com.xunji.flashcards.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.xunji.flashcards.ui.*
import com.xunji.flashcards.ui.components.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable fun TodayScreen(state:MainUiState,onReview:()->Unit,onDecks:()->Unit) {
 if(state.loading) { Busy();return }
 LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(22.dp,18.dp,22.dp,28.dp),verticalArrangement=Arrangement.spacedBy(22.dp)) {
  item { Eyebrow(LocalDate.now().format(DateTimeFormatter.ofPattern("M月d日 · EEEE",java.util.Locale.CHINESE)));Spacer(Modifier.height(10.dp));SectionTitle("每一点，都算数。","按自己的节奏，让记忆慢慢生长。") }
  if(state.error!=null)item { Notice(state.error,true) }
  item {
   Surface(shape=RoundedCornerShape(28.dp),color=MaterialTheme.colorScheme.primary) {
    Column(Modifier.fillMaxWidth().padding(24.dp),verticalArrangement=Arrangement.spacedBy(24.dp)) {
     Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) { Text("今天的学习",style=MaterialTheme.typography.titleMedium);Glyph("leaf",color=MaterialTheme.colorScheme.onPrimary) }
     Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(24.dp)) {
      Column(Modifier.weight(1f)) { Text(state.due.toString(),style=MaterialTheme.typography.displaySmall);Text("到期复习",style=MaterialTheme.typography.bodyMedium) }
      Column(Modifier.weight(1f)) { Text(state.availableNew.toString(),style=MaterialTheme.typography.displaySmall);Text("可学新卡",style=MaterialTheme.typography.bodyMedium) }
     }
     Button(onReview,Modifier.fillMaxWidth().heightIn(min=56.dp),enabled=state.due+state.availableNew>0,shape=RoundedCornerShape(16.dp),colors=ButtonDefaults.buttonColors(containerColor=MaterialTheme.colorScheme.background,contentColor=MaterialTheme.colorScheme.primary,disabledContainerColor=MaterialTheme.colorScheme.background.copy(alpha=.85f),disabledContentColor=MaterialTheme.colorScheme.primary)) { Text(if(state.due+state.availableNew>0)"开始复习" else "暂时没有待学卡片");Spacer(Modifier.width(12.dp));Glyph("arrow",color=MaterialTheme.colorScheme.primary) }
    }
   }
  }
  if(state.cards.isEmpty())item { EmptyState("从第一张卡开始","收集一个词、一句话，或一个想记住的答案。","创建牌组",onDecks) }
  else {
   item { Paper(Modifier.fillMaxWidth()) { Eyebrow("TODAY · 已经走过的路");Row(horizontalArrangement=Arrangement.spacedBy(24.dp)) { Metric((state.statistics?.today?.scoreCount ?: 0).toString(),"评分次数",Modifier.weight(1f));Metric(state.learnedNew.toString(),"已学新卡",Modifier.weight(1f)) };Notice("每日新卡上限 ${state.settings.newCardsPerDay} 张 · 到期复习不限量") } }
   item { Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) { Text("你的记忆花园",style=MaterialTheme.typography.titleLarge);TextButton(onDecks) { Text("全部牌组") } } }
   items(minOf(2,state.decks.size)) { i->val deck=state.decks[i];Paper(Modifier.fillMaxWidth().clickable(onClick=onDecks)) { Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(16.dp)) { Glyph("decks",color=MaterialTheme.colorScheme.primary);Column { Text(deck.deck.name,style=MaterialTheme.typography.titleMedium);Notice("${deck.total} 张卡片 · ${deck.due} 张到期") } } } }
  }
  item { Notice("偶尔停一天也没关系。你的卡片会在这里等你。") }
 }
}
