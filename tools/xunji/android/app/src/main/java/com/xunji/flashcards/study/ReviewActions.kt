package com.xunji.flashcards.study
interface ReviewActions {
 suspend fun grade(command:GradeCommand):GradeResult
 suspend fun undo(sessionId:String,logId:String):UndoResult
}
