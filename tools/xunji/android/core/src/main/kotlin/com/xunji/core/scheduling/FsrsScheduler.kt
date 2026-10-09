package com.xunji.core.scheduling
import com.xunji.core.model.*
import java.time.Instant
import java.time.Duration
import io.github.openspacedrepetition.Card as FsrsCard
import io.github.openspacedrepetition.State as FsrsState
import io.github.openspacedrepetition.Rating as FsrsRating
import io.github.openspacedrepetition.Scheduler as LibraryScheduler

class FsrsScheduler:Scheduler {
    private val delegate = LibraryScheduler.builder()
        .desiredRetention(0.9)
        .learningSteps(arrayOf(Duration.ofMinutes(1),Duration.ofMinutes(10)))
        .relearningSteps(arrayOf(Duration.ofMinutes(10)))
        .enableFuzzing(false).build()

    override fun review(schedule:CardSchedule,rating:Rating,at:Instant):CardSchedule {
        require(schedule.lastReviewAt == null || at.toEpochMilli() >= schedule.lastReviewAt) { "设备时间早于上次复习，请校准时间后重试" }
        val card = FsrsCard.builder().cardId(0)
            .state(when(schedule.phase) {
                Phase.NEW,Phase.LEARNING -> FsrsState.LEARNING
                Phase.REVIEW -> FsrsState.REVIEW
                Phase.RELEARNING -> FsrsState.RELEARNING
            })
            .step(if(schedule.phase==Phase.NEW) 0 else schedule.step)
            .stability(schedule.stability).difficulty(schedule.difficulty)
            .due(Instant.ofEpochMilli(schedule.dueAt))
            .lastReview(schedule.lastReviewAt?.let(Instant::ofEpochMilli)).build()
        val result=delegate.reviewCard(card,FsrsRating.valueOf(rating.name),at,null).card()
        return schedule.copy(
            phase=Phase.valueOf(result.state.name),step=result.step,
            stability=result.stability,difficulty=result.difficulty,dueAt=result.due.toEpochMilli(),
            lastReviewAt=at.toEpochMilli(),firstReviewedAt=schedule.firstReviewedAt?:at.toEpochMilli(),
            revision=schedule.revision+1,
        )
    }
    override fun preview(schedule:CardSchedule,at:Instant)=Rating.entries.associateWith { review(schedule,it,at) }
}
