package com.xunji.flashcards.ui.components

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import java.time.*
import java.time.format.DateTimeFormatter

@Composable fun Glyph(kind:String,modifier:Modifier=Modifier,color:Color=MaterialTheme.colorScheme.onSurface) {
 Canvas(modifier.size(24.dp)) {
  val w=size.width;val h=size.height;val stroke=Stroke(1.8.dp.toPx(),cap=StrokeCap.Round,join=StrokeJoin.Round)
  fun line(x1:Float,y1:Float,x2:Float,y2:Float)=drawLine(color,Offset(x1*w,y1*h),Offset(x2*w,y2*h),stroke.width,StrokeCap.Round)
  when(kind) {
   "back"->{line(.7f,.2f,.35f,.5f);line(.35f,.5f,.7f,.8f)}
   "add"->{line(.5f,.18f,.5f,.82f);line(.18f,.5f,.82f,.5f)}
   "today"->{drawCircle(color,w*.32f,style=stroke);line(.5f,.3f,.5f,.5f);line(.5f,.5f,.66f,.58f)}
   "decks"->{drawRoundRect(color,Offset(w*.12f,h*.23f),Size(w*.65f,h*.65f),CornerRadius(w*.1f),style=stroke);line(.3f,.12f,.85f,.12f);line(.88f,.15f,.88f,.65f)}
   "stats"->{line(.2f,.82f,.2f,.52f);line(.5f,.82f,.5f,.2f);line(.8f,.82f,.8f,.4f)}
   "settings"->{drawCircle(color,w*.31f,style=stroke);drawCircle(color,w*.1f,style=stroke);for(i in 0..7){ val a=i*Math.PI/4;val c=kotlin.math.cos(a).toFloat();val s=kotlin.math.sin(a).toFloat();line(.5f+c*.32f,.5f+s*.32f,.5f+c*.43f,.5f+s*.43f) }}
   "edit"->{line(.25f,.7f,.7f,.25f);line(.7f,.25f,.8f,.35f);line(.8f,.35f,.35f,.8f);line(.35f,.8f,.2f,.83f);line(.2f,.83f,.25f,.7f)}
   "delete"->{line(.2f,.3f,.8f,.3f);line(.35f,.15f,.65f,.15f);line(.28f,.3f,.33f,.85f);line(.33f,.85f,.67f,.85f);line(.67f,.85f,.72f,.3f);line(.43f,.45f,.43f,.7f);line(.57f,.45f,.57f,.7f)}
   "check"->{line(.2f,.5f,.42f,.72f);line(.42f,.72f,.8f,.28f)}
   "arrow"->{line(.15f,.5f,.85f,.5f);line(.6f,.25f,.85f,.5f);line(.85f,.5f,.6f,.75f)}
   else->{val p=Path().apply { moveTo(w*.23f,h*.8f);cubicTo(w*.1f,h*.25f,w*.8f,h*.18f,w*.82f,h*.16f);cubicTo(w*.86f,h*.7f,w*.44f,h*.85f,w*.23f,h*.8f) };drawPath(p,color,style=stroke);line(.24f,.8f,.67f,.38f)}
  }
 }
}
@Composable fun IconAction(kind:String,label:String,onClick:()->Unit,tag:String="",enabled:Boolean=true) {
 IconButton(onClick=onClick,enabled=enabled,modifier=Modifier.sizeIn(minWidth=48.dp,minHeight=48.dp).testTag(tag).semantics { contentDescription=label }) { Glyph(kind,color=if(enabled)MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline) }
}
@Composable fun Mark(modifier:Modifier=Modifier) {
 Box(modifier.size(40.dp).clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.primary),contentAlignment=Alignment.Center) { Glyph("leaf",color=MaterialTheme.colorScheme.onPrimary) }
}
@Composable fun Eyebrow(text:String) { Text(text,style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onSurfaceVariant) }
@Composable fun Paper(modifier:Modifier=Modifier,content:@Composable ColumnScope.()->Unit) {
 Surface(modifier,shape=RoundedCornerShape(24.dp),color=MaterialTheme.colorScheme.surface,border=BorderStroke(1.dp,MaterialTheme.colorScheme.outlineVariant)) { Column(Modifier.padding(22.dp),verticalArrangement=Arrangement.spacedBy(16.dp),content=content) }
}
@Composable fun PrimaryButton(text:String,onClick:()->Unit,modifier:Modifier=Modifier,enabled:Boolean=true,tag:String="") {
 Button(onClick,modifier.fillMaxWidth().heightIn(min=56.dp).testTag(tag),enabled=enabled,shape=RoundedCornerShape(18.dp),contentPadding=PaddingValues(18.dp,15.dp)) { Text(text,style=MaterialTheme.typography.labelLarge) }
}
@Composable fun Notice(text:String,error:Boolean=false) {
 Text(text,style=MaterialTheme.typography.bodyMedium,color=if(error)MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,modifier=Modifier.semantics { liveRegion=LiveRegionMode.Polite })
}
@Composable fun EmptyState(title:String,description:String,action:String?=null,onAction:()->Unit={}) {
 Paper(Modifier.fillMaxWidth()) {
  Box(Modifier.size(64.dp).clip(RoundedCornerShape(22.dp)).background(MaterialTheme.colorScheme.primaryContainer),contentAlignment=Alignment.Center) { Glyph("leaf",Modifier.size(32.dp),MaterialTheme.colorScheme.primary) }
  Text(title,style=MaterialTheme.typography.headlineMedium);Notice(description)
  if(action!=null)PrimaryButton(action,onAction)
 }
}
@Composable fun Busy() { Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center) { CircularProgressIndicator(Modifier.size(32.dp),strokeWidth=3.dp) } }
@Composable fun SectionTitle(title:String,subtitle:String?=null) { Column(verticalArrangement=Arrangement.spacedBy(5.dp)) { Text(title,style=MaterialTheme.typography.headlineLarge);if(subtitle!=null)Notice(subtitle) } }
@Composable fun Metric(value:String,label:String,modifier:Modifier=Modifier) { Column(modifier,verticalArrangement=Arrangement.spacedBy(5.dp)) { Text(value,style=MaterialTheme.typography.headlineLarge,fontWeight=FontWeight.Bold);Notice(label) } }
fun intervalLabel(dueAt:Long,now:Long):String {
 val minutes=((dueAt-now).coerceAtLeast(0)/60000).coerceAtLeast(1)
 return when { minutes<60->"${minutes} 分钟";minutes<1440->"${minutes/60} 小时";else->"${minutes/1440} 天" }
}
fun localTime(timestamp:Long):String=Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("M月d日 HH:mm"))
fun durationLabel(ms:Long):String=if(ms<60000) "${ms/1000} 秒" else "${ms/60000} 分钟"
