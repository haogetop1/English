package com.xunji.flashcards.ui

import com.xunji.core.model.*
import com.xunji.core.stats.*
import com.xunji.flashcards.backup.PreparedBackup

data class MainUiState(val loading:Boolean=true,val decks:List<DeckSummary> = emptyList(),val cards:List<CardCandidate> = emptyList(),val due:Int=0,val availableNew:Int=0,val learnedNew:Int=0,val statistics:StatisticsSummary?=null,val settings:AppSettings=AppSettings(),val error:String?=null)
data class EditorUiState(val front:String="",val back:String="",val example:String="",val loading:Boolean=false,val saving:Boolean=false,val saved:Boolean=false,val dirty:Boolean=false,val errors:Map<String,String> = emptyMap(),val error:String?=null)
sealed interface ReviewUiState {
 data object Loading:ReviewUiState
 data class Question(val candidate:CardCandidate,val sessionId:String):ReviewUiState
 data class Answer(val candidate:CardCandidate,val sessionId:String,val preview:Map<Rating,CardSchedule>,val error:String?=null,val retryRating:Rating?=null):ReviewUiState
 data class Submitting(val answer:Answer):ReviewUiState
 data class Complete(val nextDueAt:Long?,val scoreCount:Int,val distinctCards:Int,val durationMs:Long):ReviewUiState
 data class Failure(val message:String):ReviewUiState
}
data class SettingsUiState(val settings:AppSettings=AppSettings(),val busy:Boolean=false,val prepared:PreparedBackup?=null,val message:String?=null,val error:String?=null)
