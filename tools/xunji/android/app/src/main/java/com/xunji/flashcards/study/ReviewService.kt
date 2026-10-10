package com.xunji.flashcards.study

import com.xunji.core.scheduling.Scheduler
import com.xunji.core.model.*
import com.xunji.core.study.NewCardQuota
import com.xunji.flashcards.data.*
import androidx.room.withTransaction
import java.time.Clock
import java.time.ZoneId

class ReviewService(private val db:XunjiDatabase,private val scheduler:Scheduler,private val clock:Clock=Clock.systemUTC(),private val zone:()->ZoneId={ZoneId.systemDefault()}) {
    suspend fun grade(command:GradeCommand):GradeResult=db.withTransaction {
        val dao=db.dao()
        if(command.operationId.isBlank() || command.sessionId.isBlank() || command.durationMs !in 0..300_000 || command.expectedRevision<0) return@withTransaction GradeResult.InvalidCommand
        dao.log(command.operationId)?.toModel()?.let { previous ->
            return@withTransaction if(previous.cardId==command.cardId && previous.sessionId==command.sessionId && previous.rating==command.rating && previous.durationMs==command.durationMs && previous.beforeSchedule.revision==command.expectedRevision) GradeResult.Duplicate(previous) else GradeResult.InvalidCommand
        }
        val card=dao.card(command.cardId) ?: return@withTransaction GradeResult.MissingCard
        val before=dao.schedule(command.cardId)?.toModel() ?: return@withTransaction GradeResult.MissingCard
        if(before.revision!=command.expectedRevision) return@withTransaction GradeResult.StaleSchedule
        val at=clock.instant()
        if(before.phase==Phase.NEW && NewCardQuota.remaining(dao.schedules().map { it.toModel() },dao.settings()?.toModel() ?: AppSettings(),at,zone())==0) return@withTransaction GradeResult.QuotaReached
        val after=scheduler.review(before,command.rating,at)
        val log=ReviewLog(command.operationId,card.id,card.deckId,command.sessionId,command.rating,at.toEpochMilli(),command.durationMs,before,after,null)
        dao.putSchedule(after.toEntity())
        dao.insertLog(log.toEntity())
        GradeResult.Recorded(log)
    }
    suspend fun undo(sessionId:String,logId:String):UndoResult=db.withTransaction {
        val dao=db.dao()
        val log=dao.latestSessionLog(sessionId)?.toModel() ?: return@withTransaction UndoResult.Unavailable
        if(log.id!=logId || log.undoneAt!=null) return@withTransaction UndoResult.Unavailable
        val current=dao.schedule(log.cardId)?.toModel() ?: return@withTransaction UndoResult.Unavailable
        if(current.revision!=log.afterSchedule.revision) return@withTransaction UndoResult.Unavailable
        val restored=log.beforeSchedule.copy(revision=current.revision+1)
        dao.putSchedule(restored.toEntity())
        dao.updateLog(log.copy(undoneAt=clock.millis()).toEntity())
        UndoResult.Restored(restored)
    }
}
