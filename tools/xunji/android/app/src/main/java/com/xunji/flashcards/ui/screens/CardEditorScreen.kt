package com.xunji.flashcards.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.xunji.flashcards.ui.*
import com.xunji.flashcards.ui.components.*

@Composable fun CardEditorScreen(state:EditorUiState,viewModel:EditorViewModel,editing:Boolean,onExit:()->Unit) {
 var discard by rememberSaveable { mutableStateOf(false) }
 val requestExit={ if(!state.saving){ if(state.dirty)discard=true else onExit() };Unit }
 BackHandler(onBack=requestExit)
 LaunchedEffect(state.saved) { if(state.saved)onExit() }
 Column(Modifier.fillMaxSize().imePadding()) {
  Row(Modifier.fillMaxWidth().padding(10.dp,6.dp),verticalAlignment=Alignment.CenterVertically) { IconAction("back","返回",requestExit,"editor-exit",!state.saving);Text(if(editing)"编辑卡片" else "新建卡片",style=MaterialTheme.typography.titleLarge) }
  if(state.loading){Busy();return}
  Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(22.dp),verticalArrangement=Arrangement.spacedBy(20.dp)) {
   Notice("让问题简单一点，让答案清楚一点。")
   EditorField("正面 · 问题或词汇","例如：resilient",state.front,{viewModel.update("front",it)},state.errors["front"],"editor-front",!state.saving)
   EditorField("背面 · 答案或释义","例如：有韧性的；能迅速恢复的",state.back,{viewModel.update("back",it)},state.errors["back"],"editor-back",!state.saving)
   EditorField("例句 · 可选","给这个知识点一个上下文",state.example,{viewModel.update("example",it)},state.errors["example"],"editor-example",!state.saving)
   if(state.error!=null)Notice(state.error,true)
  }
  Box(Modifier.padding(22.dp,12.dp,22.dp,20.dp)) { PrimaryButton(if(state.saving)"正在保存…" else "保存卡片",viewModel::save,enabled=!state.saving && !state.loading,tag="save-card") }
 }
 if(discard)AlertDialog(onDismissRequest={discard=false},title={Text("放弃未保存的修改？")},text={Text("当前内容还没有保存到卡片。")},confirmButton={TextButton({discard=false;onExit()}) {Text("放弃修改",color=MaterialTheme.colorScheme.error)}},dismissButton={TextButton({discard=false}) {Text("继续编辑")} })
}
@Composable private fun EditorField(label:String,placeholder:String,value:String,onChange:(String)->Unit,error:String?,tag:String,enabled:Boolean) {
 Column(verticalArrangement=Arrangement.spacedBy(10.dp)) {
  Text(label,style=MaterialTheme.typography.titleMedium)
  OutlinedTextField(value,onChange,modifier=Modifier.fillMaxWidth().testTag(tag),placeholder={Text(placeholder)},minLines=3,maxLines=8,enabled=enabled,isError=error!=null,shape=RoundedCornerShape(18.dp),supportingText={if(error!=null)Text(error)})
 }
}
