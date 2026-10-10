package com.xunji.flashcards.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import com.xunji.flashcards.ui.*
import com.xunji.flashcards.ui.components.*

@Composable fun StatisticsScreen(state:MainUiState) {
 val stats=state.statistics
 LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(22.dp,18.dp,22.dp,28.dp),verticalArrangement=Arrangement.spacedBy(22.dp)) {
  item { SectionTitle("留下的足迹","每次认真回忆，都在积累。") }
  if(stats==null || stats.lastSevenDays.sumOf { it.scoreCount }==0)item { EmptyState("还没有学习记录","完成一次评分后，在这里看看你走过的路。") }
  if(stats!=null) {
   item { Paper(Modifier.fillMaxWidth()) { Eyebrow("今天");Row(horizontalArrangement=Arrangement.spacedBy(24.dp)) { Metric(stats.today.scoreCount.toString(),"评分次数",Modifier.weight(1f));Metric(stats.today.distinctCards.toString(),"学习卡片",Modifier.weight(1f)) };HorizontalDivider(color=MaterialTheme.colorScheme.outlineVariant);Row(horizontalArrangement=Arrangement.spacedBy(24.dp)) { Metric(stats.today.againRatio?.let { "${(it*100).toInt()}%" } ?: "—","忘记比例",Modifier.weight(1f));Metric(durationLabel(stats.today.durationMs),"有效用时",Modifier.weight(1f)) } } }
   item { Paper(Modifier.fillMaxWidth()) { Text("最近 7 天",style=MaterialTheme.typography.titleLarge);Notice("评分次数 · 包含今天")
    val max=stats.lastSevenDays.maxOf { it.scoreCount }.coerceAtLeast(1)
    Row(Modifier.fillMaxWidth().height(152.dp),horizontalArrangement=Arrangement.spacedBy(8.dp),verticalAlignment=Alignment.Bottom) {
     stats.lastSevenDays.forEach { day->Column(Modifier.weight(1f).semantics(mergeDescendants=true) { contentDescription="${day.date}：${day.scoreCount} 次评分，${day.distinctCards} 张卡片" },horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(8.dp)) {
      Text(day.scoreCount.toString(),style=MaterialTheme.typography.labelMedium)
      Box(Modifier.fillMaxWidth().height((6+80f*day.scoreCount/max).dp).clip(RoundedCornerShape(8.dp)).background(if(day.date==stats.today.date)MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer))
      Text(day.date.dayOfMonth.toString(),style=MaterialTheme.typography.labelMedium)
     } }
    }
   } }
   items(stats.lastSevenDays.size) { i->val day=stats.lastSevenDays.reversed()[i];Paper(Modifier.fillMaxWidth()) { Text("${day.date.monthValue}月${day.date.dayOfMonth}日",style=MaterialTheme.typography.titleMedium);Notice("${day.scoreCount} 次评分 · ${day.distinctCards} 张卡片 · ${durationLabel(day.durationMs)}");Notice("忘记比例 ${day.againRatio?.let { "${(it*100).toInt()}%" } ?: "—"}") } }
  }
  item { Notice("忘记比例 =「忘记」评分 ÷ 有效评分总数。它帮助你了解复习感受；被撤销的评分不会计入。") }
 }
}
