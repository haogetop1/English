package com.xunji.flashcards.ui

import androidx.lifecycle.*
import com.xunji.core.model.*
import com.xunji.core.scheduling.Scheduler
import com.xunji.core.study.*
import com.xunji.flashcards.data.StudyRepository
import com.xunji.flashcards.study.*
import java.time.*
import java.util.UUID
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class ReviewViewModel(private val repository:StudyRepository,private val actions:ReviewActions,private val scheduler:Scheduler,private val handle:SavedStateHandle=SavedStateHandle(),private val clock:Clock=Clock.systemUTC(),private val zone:()->ZoneId={ZoneId.systemDefault()},private val monotonic:()->Long={android.os.SystemClock.elapsedRealtime()}):ViewModel() {
    private val mutable=MutableStateFlow<ReviewUiState>(ReviewUiState.Loading)
    val state:StateFlow<ReviewUiState> = mutable.asStateFlow()
    private val undoable=MutableStateFlow(false)
    val canUndo:StateFlow<Boolean> = undoable.asStateFlow()
    private val timer=SessionTimer()
    private var sessionId=""
    private var deckId:String?=null
    private var active=false
    private var foreground=true
    private var current:CardCandidate?=null
    private var pending:GradeCommand?=null
    private var lastLog:ReviewLog?=null
    private var job:Job?=null
    private var refreshJob:Job?=null
    private var generation=0L
    private var operation=0L
    private var refreshSequence=0L
    private data class Token(val generation:Long,val operation:Long)
    private fun valid(token:Token)=active && token.generation==generation && token.operation==operation
    private fun invalidateRefresh() { refreshSequence++;refreshJob?.cancel();refreshJob=null }
    fun start(deckId:String?) {
        if(active && this.deckId==deckId)return
        finish();active=true;this.deckId=deckId;sessionId=UUID.randomUUID().toString();handle["sessionId"]=sessionId;loadNext()
    }
    private fun launchOperation(block:suspend (Token)->Unit) {
        invalidateRefresh();job?.cancel()
        val token=Token(generation,++operation)
        job=viewModelScope.launch {
            try { block(token) } catch(e:CancellationException) { throw e } catch(_:Exception) {
                if(valid(token))mutable.value=ReviewUiState.Failure("暂时无法读取卡片，请重试")
            }
        }
    }
    private fun showQuestion(candidate:CardCandidate,token:Token) {
        if(!valid(token))return
        current=candidate;pending=null;timer.reset();if(foreground)timer.resume(monotonic())
        handle["cardId"]=candidate.card.id;handle["revealed"]=false
        mutable.value=ReviewUiState.Question(candidate,sessionId)
    }
    private fun answer(candidate:CardCandidate)=ReviewUiState.Answer(candidate,sessionId,scheduler.preview(candidate.schedule,clock.instant()))
    private suspend fun next(token:Token) {
        if(!valid(token))return
        val candidates=repository.candidates(deckId)
        if(!valid(token))return
        val schedules=repository.observeSchedules().first()
        if(!valid(token))return
        val settings=repository.observeSettings().first()
        if(!valid(token))return
        val remaining=NewCardQuota.remaining(schedules,settings,clock.instant(),zone())
        when(val decision=QueuePlanner.next(candidates,remaining,clock.instant())) {
            is QueueDecision.Ready->showQuestion(decision.candidate,token)
            else->{
                val logs=repository.observeLogs().first().filter { it.sessionId==sessionId && it.undoneAt==null }
                if(!valid(token))return
                current=null;pending=null;timer.reset();handle["revealed"]=false
                mutable.value=ReviewUiState.Complete((decision as? QueueDecision.Waiting)?.nextDueAt,logs.size,logs.map { it.cardId }.toSet().size,logs.sumOf { it.durationMs })
            }
        }
    }
    private fun loadNext() { if(!active)return;mutable.value=ReviewUiState.Loading;launchOperation { next(it) } }
    fun refresh() {
        if(!active || mutable.value is ReviewUiState.Submitting || mutable.value is ReviewUiState.Loading)return
        val old=current
        if(old==null || mutable.value is ReviewUiState.Failure) { loadNext();return }
        invalidateRefresh()
        val sequence=refreshSequence
        val token=Token(generation,operation)
        val selectedDeck=deckId
        fun stillCurrent()=valid(token) && sequence==refreshSequence && current?.card?.id==old.card.id
        refreshJob=viewModelScope.launch {
            try {
                val candidate=repository.candidates(selectedDeck).firstOrNull { it.card.id==old.card.id && it.schedule.revision==old.schedule.revision }
                if(!stillCurrent())return@launch
                if(candidate==null)loadNext() else {
                    val shown=mutable.value
                    // Rebuild from the state that is still visible, never a pre-suspension snapshot.
                    val refreshed=when(shown) { is ReviewUiState.Answer->answer(candidate).copy(error=shown.error,retryRating=shown.retryRating);is ReviewUiState.Question->ReviewUiState.Question(candidate,sessionId);else->return@launch }
                    current=candidate;mutable.value=refreshed
                }
            } catch(e:CancellationException) { throw e } catch(_:Exception) {
                if(stillCurrent())mutable.value=ReviewUiState.Failure("暂时无法读取卡片，请重试")
            }
        }
    }
    fun reveal() {
        val question=mutable.value as? ReviewUiState.Question ?: return
        invalidateRefresh()
        try { mutable.value=answer(question.candidate);handle["revealed"]=true } catch(_:Exception) { mutable.value=ReviewUiState.Failure("暂时无法计算复习间隔，请重试") }
    }
    fun grade(rating:Rating) {
        if(!active)return
        val shown=mutable.value as? ReviewUiState.Answer ?: return
        if(pending!=null && pending!!.rating!=rating)return
        val command=pending ?: GradeCommand(UUID.randomUUID().toString(),sessionId,shown.candidate.card.id,shown.candidate.schedule.revision,rating,timer.elapsedMs(monotonic())).also { pending=it }
        timer.pause(monotonic());mutable.value=ReviewUiState.Submitting(shown)
        launchOperation { token->
            try {
                val result=actions.grade(command)
                if(!valid(token))return@launchOperation
                when(result) {
                    is GradeResult.Recorded->{ lastLog=result.log;undoable.value=true;next(token) }
                    is GradeResult.Duplicate->{ lastLog=result.log;undoable.value=result.log.undoneAt==null;next(token) }
                    GradeResult.MissingCard,GradeResult.StaleSchedule,GradeResult.QuotaReached->next(token)
                    GradeResult.InvalidCommand->mutable.value=shown.copy(error="无法记录此评分，请刷新卡片",retryRating=rating)
                }
            } catch(e:CancellationException) { throw e } catch(_:Exception) {
                if(valid(token))mutable.value=shown.copy(error="记录未完成，请重试",retryRating=rating)
            }
        }
    }
    fun undo() {
        if(!active || !undoable.value || mutable.value is ReviewUiState.Submitting || mutable.value is ReviewUiState.Loading)return
        val log=lastLog ?: return
        undoable.value=false;mutable.value=ReviewUiState.Loading
        val selectedSession=sessionId;val selectedDeck=deckId
        launchOperation { token->
            val result=actions.undo(selectedSession,log.id)
            if(!valid(token))return@launchOperation
            lastLog=null
            when(result) {
                is UndoResult.Restored->{ val c=repository.candidates(selectedDeck).firstOrNull { it.card.id==log.cardId };if(!valid(token))return@launchOperation;if(c!=null)showQuestion(c,token) else next(token) }
                UndoResult.Unavailable->next(token)
            }
        }
    }
    fun setForeground(active:Boolean) {
        foreground=active
        if(active) { if(this.active) { if(pending==null)timer.resume(monotonic());refresh() } } else timer.pause(monotonic())
    }
    override fun onCleared() { finish();super.onCleared() }
    fun finish() {
        generation++;operation++;active=false;invalidateRefresh();job?.cancel();job=null;current=null;pending=null;lastLog=null;undoable.value=false;timer.reset();handle["revealed"]=false
        mutable.value=ReviewUiState.Complete(null,0,0,0)
    }
}
