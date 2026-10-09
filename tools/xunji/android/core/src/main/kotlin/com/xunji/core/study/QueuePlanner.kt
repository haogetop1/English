package com.xunji.core.study
import com.xunji.core.model.*
import java.time.Instant
sealed interface QueueDecision {
    data class Ready(val candidate:CardCandidate):QueueDecision
    data class Waiting(val nextDueAt:Long):QueueDecision
    data object Done:QueueDecision
}
object QueuePlanner {
    fun next(candidates:List<CardCandidate>,remainingNew:Int,at:Instant):QueueDecision {
        val due=candidates.filter { it.schedule.phase!=Phase.NEW && it.schedule.dueAt<=at.toEpochMilli() }
        val ordered=due.sortedWith(compareBy<CardCandidate> { if(it.schedule.phase==Phase.REVIEW) 1 else 0 }
            .thenBy { it.schedule.dueAt }.thenBy { it.card.id })
        ordered.firstOrNull()?.let { return QueueDecision.Ready(it) }
        if(remainingNew>0) candidates.filter { it.schedule.phase==Phase.NEW }
            .minWithOrNull(compareBy<CardCandidate> { it.card.createdAt }.thenBy { it.card.id })
            ?.let { return QueueDecision.Ready(it) }
        val next=candidates.filter { it.schedule.phase!=Phase.NEW }.minOfOrNull { it.schedule.dueAt }
        return if(next!=null) QueueDecision.Waiting(next) else QueueDecision.Done
    }
}
