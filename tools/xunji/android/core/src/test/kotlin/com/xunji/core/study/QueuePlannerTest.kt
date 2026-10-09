package com.xunji.core.study

import com.xunji.core.model.*
import java.time.Instant
import org.junit.Assert.*
import org.junit.Test

class QueuePlannerTest {
    private val at = Instant.parse("2026-10-09T08:00:00Z")
    private fun candidate(id:String, phase:Phase, delta:Long=0) = CardCandidate(
        Flashcard(id,"deck",id,"answer","",at.toEpochMilli(),at.toEpochMilli()),
        CardSchedule(id,phase,null,null,null,at.toEpochMilli()+delta,null,null,0),"测试")

    @Test fun dueLearningPrecedesReviewAndNew() {
        val learn = candidate("learning",Phase.LEARNING)
        val cards = listOf(candidate("new",Phase.NEW),candidate("review",Phase.REVIEW,-1000),learn)
        assertEquals(QueueDecision.Ready(learn),QueuePlanner.next(cards,20,at))
    }
    @Test fun zeroQuotaStillAllowsDueReview() {
        val review = candidate("review",Phase.REVIEW)
        assertEquals(QueueDecision.Ready(review),QueuePlanner.next(listOf(candidate("new",Phase.NEW),review),0,at))
        assertEquals(QueueDecision.Done,QueuePlanner.next(listOf(candidate("new",Phase.NEW)),0,at))
    }
    @Test fun futureCardsAreNotShownEarly() {
        assertEquals(QueueDecision.Waiting(at.toEpochMilli()+60_000),QueuePlanner.next(listOf(candidate("later",Phase.LEARNING,60_000)),20,at))
    }
    @Test fun reviewPrecedesNewAndTieUsesId() {
        val a=candidate("a",Phase.REVIEW)
        assertEquals(QueueDecision.Ready(a),QueuePlanner.next(listOf(candidate("z",Phase.REVIEW),candidate("new",Phase.NEW),a),20,at))
    }
    @Test fun newCardsAreOrderedAndEmptyQueueCompletes() {
        val a=candidate("a",Phase.NEW)
        assertEquals(QueueDecision.Ready(a),QueuePlanner.next(listOf(candidate("z",Phase.NEW),a),1,at))
        assertEquals(QueueDecision.Done,QueuePlanner.next(emptyList(),20,at))
    }
}
