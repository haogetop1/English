package com.xunji.flashcards.study

import com.xunji.core.model.*

data class GradeCommand(val operationId:String,val sessionId:String,val cardId:String,val expectedRevision:Long,val rating:Rating,val durationMs:Long)
sealed interface GradeResult {
    data class Recorded(val log:ReviewLog):GradeResult
    data class Duplicate(val log:ReviewLog):GradeResult
    data object MissingCard:GradeResult
    data object StaleSchedule:GradeResult
    data object QuotaReached:GradeResult
    data object InvalidCommand:GradeResult
}
sealed interface UndoResult {
    data class Restored(val schedule:CardSchedule):UndoResult
    data object Unavailable:UndoResult
}
