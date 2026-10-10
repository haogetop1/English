package com.xunji.flashcards.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.*
import com.xunji.core.model.*
import com.xunji.flashcards.ui.*
import com.xunji.flashcards.ui.components.*

@OptIn(ExperimentalLayoutApi::class)
@Composable fun ReviewScreen(state:ReviewUiState,canUndo:Boolean,onReveal:()->Unit,onGrade:(Rating)->Unit,onUndo:()->Unit,onRefresh:()->Unit,onExit:()->Unit) {
 val submitting=state is ReviewUiState.Submitting
 Column(Modifier.fillMaxSize()) {
  Row(Modifier.fillMaxWidth().padding(10.dp,6.dp),verticalAlignment=Alignment.CenterVertically) {
   IconAction("back","结束复习",onExit,"review-exit",!submitting)
   Text("专注这一张",style=MaterialTheme.typography.titleLarge,modifier=Modifier.weight(1f))
   if(canUndo)TextButton(onUndo,enabled=!submitting,modifier=Modifier.testTag("undo-rating")) { Text("撤销") }
  }
  when(state) {
   ReviewUiState.Loading->Busy()
   is ReviewUiState.Failure->Box(Modifier.padding(22.dp)) { EmptyState("暂时遇到一点问题",state.message,"重新读取",onRefresh) }
   is ReviewUiState.Complete->Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(22.dp),verticalArrangement=Arrangement.spacedBy(24.dp)) {
    Spacer(Modifier.height(16.dp));Box(Modifier.fillMaxWidth(),contentAlignment=Alignment.Center) { Mark(Modifier.size(64.dp)) }
    SectionTitle(if(state.scoreCount>0)"这一轮，辛苦了。" else "现在可以歇一歇。",if(state.nextDueAt!=null)"下一张将在 ${localTime(state.nextDueAt)} 到期。" else "当前没有可以学习的卡片。")
    Paper(Modifier.fillMaxWidth().testTag("review-complete")) { Eyebrow("本次复习");Row(horizontalArrangement=Arrangement.spacedBy(20.dp)) { Metric(state.scoreCount.toString(),"评分次数",Modifier.weight(1f));Metric(state.distinctCards.toString(),"学习卡片",Modifier.weight(1f)) };Notice("有效用时 ${durationLabel(state.durationMs)}") }
    Notice("学习步骤会在到期后再次出现。留一点空间，让记忆沉淀。")
    PrimaryButton("完成复习",onExit);OutlinedButton(onRefresh,Modifier.fillMaxWidth().heightIn(min=52.dp),shape=RoundedCornerShape(18.dp)) { Text("检查到期卡片") }
   }
   else->{
    val answer=when(state) { is ReviewUiState.Answer->state;is ReviewUiState.Submitting->state.answer;else->null }
    val candidate=answer?.candidate ?: (state as ReviewUiState.Question).candidate
    Column(Modifier.fillMaxSize().padding(22.dp,12.dp,22.dp,18.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
     FlowRow(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(18.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) { Eyebrow(candidate.deckName);Eyebrow(if(answer==null)"先回忆，再揭晓" else "按真实感受评分") }
     Surface(Modifier.weight(1f).fillMaxWidth(),shape=RoundedCornerShape(28.dp),color=MaterialTheme.colorScheme.surface,border=BorderStroke(1.dp,MaterialTheme.colorScheme.outlineVariant)) {
      Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(26.dp),verticalArrangement=Arrangement.spacedBy(20.dp)) {
       Eyebrow("QUESTION · 正面")
       Text(candidate.card.front,style=MaterialTheme.typography.headlineLarge.copy(fontFamily=FontFamily.Serif,fontSize=32.sp,lineHeight=44.sp),modifier=Modifier.testTag("card-front"))
       AnimatedVisibility(answer!=null,enter=fadeIn(tween(200)),exit=fadeOut(tween(180))) {
        Column(verticalArrangement=Arrangement.spacedBy(20.dp)) {
         HorizontalDivider(color=MaterialTheme.colorScheme.outlineVariant);Eyebrow("ANSWER · 背面")
         Text(candidate.card.back,style=MaterialTheme.typography.bodyLarge,modifier=Modifier.testTag("card-back"))
         if(candidate.card.example.isNotBlank()) { Surface(shape=RoundedCornerShape(16.dp),color=MaterialTheme.colorScheme.surfaceVariant) { Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) { Eyebrow("放进句子里");Text(candidate.card.example,style=MaterialTheme.typography.bodyLarge) } } }
        }
       }
      }
     }
     if(answer==null)PrimaryButton("显示答案",onReveal,tag="reveal-answer")
     else {
      if(answer.error!=null)Notice(answer.error,true)
      if(answer.retryRating!=null)PrimaryButton(if(submitting)"正在记录…" else "重试记录",{onGrade(answer.retryRating)},enabled=!submitting,tag="retry-rating")
      else RatingGrid(answer,submitting,onGrade)
     }
    }
   }
  }
 }
}
@Composable private fun RatingGrid(answer:ReviewUiState.Answer,submitting:Boolean,onGrade:(Rating)->Unit) {
 val fontScale=LocalDensity.current.fontScale
 BoxWithConstraints(Modifier.fillMaxWidth()) {
  val columns=if(maxWidth<360.dp || fontScale>1.2f)2 else 4
  Column(verticalArrangement=Arrangement.spacedBy(10.dp)) {
   Rating.entries.chunked(columns).forEach { row->
    Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
     row.forEach { rating->
      val(label,tag)=when(rating) { Rating.AGAIN->"忘记" to "rating-again";Rating.HARD->"困难" to "rating-hard";Rating.GOOD->"记得" to "rating-good";Rating.EASY->"轻松" to "rating-easy" }
      val color=when(rating) { Rating.AGAIN->MaterialTheme.colorScheme.error;Rating.HARD->MaterialTheme.colorScheme.secondary;else->MaterialTheme.colorScheme.primary }
      val modifier=Modifier.weight(1f).heightIn(min=64.dp).testTag(tag)
      val content:@Composable RowScope.()->Unit={Column(horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(3.dp)) { Text(label,style=MaterialTheme.typography.labelLarge);Text(intervalLabel(answer.preview.getValue(rating).dueAt,System.currentTimeMillis()),style=MaterialTheme.typography.labelMedium) } }
      if(rating==Rating.GOOD)Button({onGrade(rating)},modifier,enabled=!submitting,shape=RoundedCornerShape(16.dp),contentPadding=PaddingValues(8.dp,12.dp),content=content)
      else OutlinedButton({onGrade(rating)},modifier,enabled=!submitting,shape=RoundedCornerShape(16.dp),colors=ButtonDefaults.outlinedButtonColors(contentColor=color),contentPadding=PaddingValues(8.dp,12.dp),border=BorderStroke(1.dp,MaterialTheme.colorScheme.outlineVariant),content=content)
     }
    }
   }
  }
 }
}
