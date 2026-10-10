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
    private var generation=0
    fun start(deckId:String?) {
        finish();active=true;this.deckId=deckId;sessionId=UUID.randomUUID().toString();handle["sessionId"]=sessionId;loadNext()
    }
    private fun launchOperation(block:suspend ()->Unit) {
        val token=generation
        job=viewModelScope.launch {
            try { block() } catch(e:CancellationException) { throw e } catch(_:Exception) {
                if(token==generation && active) mutable.value=ReviewUiState.Failure("暂时无法读取卡片，请重试")
            }
        }
    }
    private fun showQuestion(candidate:CardCandidate) {
        current=candidate;pending=null;timer.reset();if(foreground)timer.resume(monotonic())
        handle["cardId"]=candidate.card.id;handle["revealed"]=false
        mutable.value=ReviewUiState.Question(candidate,sessionId)
    }
    private fun answer(candidate:CardCandidate)=ReviewUiState.Answer(candidate,sessionId,scheduler.preview(candidate.schedule,clock.instant()))
    private suspend fun next() {
        val candidates=repository.candidates(deckId)
        val remaining=NewCardQuota.remaining(repository.observeSchedules().first(),repository.observeSettings().first(),clock.instant(),zone())
        when(val decision=QueuePlanner.next(candidates,remaining,clock.instant())) {
            is QueueDecision.Ready->showQuestion(decision.candidate)
            else->{
                current=null;pending=null;timer.reset();handle["revealed"]=false
                val logs=repository.observeLogs().first().filter { it.sessionId==sessionId && it.undoneAt==null }
                mutable.value=ReviewUiState.Complete((decision as? QueueDecision.Waiting)?.nextDueAt,logs.size,logs.map { it.cardId }.toSet().size,logs.sumOf { it.durationMs })
            }
        }
    }
    private fun loadNext() { if(!active)return;mutable.value=ReviewUiState.Loading;launchOperation { next() } }
    fun refresh() {
        if(!active || mutable.value is ReviewUiState.Submitting)return
        val previous=mutable.value
        launchOperation {
            val candidate=current?.let { old -> repository.candidates(deckId).firstOrNull { it.card.id==old.card.id && it.schedule.revision==old.schedule.revision } }
            if(candidate==null)next() else { current=candidate;mutable.value=if(previous is ReviewUiState.Answer)answer(candidate).copy(error=previous.error,retryRating=previous.retryRating) else ReviewUiState.Question(candidate,sessionId) }
        }
    }
    fun reveal() {
        val question=mutable.value as? ReviewUiState.Question ?: return
        try { mutable.value=answer(question.candidate);handle["revealed"]=true } catch(_:Exception) { mutable.value=ReviewUiState.Failure("暂时无法计算复习间隔，请重试") }
    }
    fun grade(rating:Rating) {
        if(!active)return
        val shown=mutable.value as? ReviewUiState.Answer ?: return
        if(pending!=null && pending!!.rating!=rating)return
        val command=pending ?: GradeCommand(UUID.randomUUID().toString(),sessionId,shown.candidate.card.id,shown.candidate.schedule.revision,rating,timer.elapsedMs(monotonic())).also { pending=it }
        timer.pause(monotonic());mutable.value=ReviewUiState.Submitting(shown)
        val token=generation
        launchOperation {
            try {
                val result=actions.grade(command)
                if(token!=generation || !active)return@launchOperation
                when(result) {
                    is GradeResult.Recorded->{ lastLog=result.log;undoable.value=true;next() }
                    is GradeResult.Duplicate->{ lastLog=result.log;undoable.value=result.log.undoneAt==null;next() }
                    GradeResult.MissingCard,GradeResult.StaleSchedule,GradeResult.QuotaReached->next()
                    GradeResult.InvalidCommand->mutable.value=shown.copy(error="无法记录此评分，请刷新卡片",retryRating=rating)
                }
            } catch(e:CancellationException) { throw e } catch(_:Exception) {
                if(token==generation && active)mutable.value=shown.copy(error="记录未完成，请重试",retryRating=rating)
            }
        }
    }
    fun undo() {
        if(!active || !undoable.value || mutable.value is ReviewUiState.Submitting)return
        val log=lastLog ?: return
        undoable.value=false;mutable.value=ReviewUiState.Loading
        launchOperation {
            when(actions.undo(sessionId,log.id)) {
                is UndoResult.Restored->{ lastLog=null;val c=repository.candidates(deckId).firstOrNull { it.card.id==log.cardId };if(c!=null)showQuestion(c) else next() }
                UndoResult.Unavailable->{ lastLog=null;next() }
            }
        }
    }
    fun setForeground(active:Boolean) {
        foreground=active
        if(active) { if(this.active) { if(pending==null)timer.resume(monotonic());refresh() } } else timer.pause(monotonic())
    }
    fun finish() {
        generation++;active=false;job?.cancel();job=null;current=null;pending=null;lastLog=null;undoable.value=false;timer.reset();handle["revealed"]=false
        mutable.value=ReviewUiState.Complete(null,0,0,0)
    }
}
