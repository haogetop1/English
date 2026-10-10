package com.xunji.flashcards

import android.app.Application
import android.content.Context
import com.xunji.core.scheduling.FsrsScheduler
import com.xunji.flashcards.data.*
import com.xunji.flashcards.study.ReviewService
import com.xunji.flashcards.backup.*

class AppContainer(context:Context) {
    val database=XunjiDatabase.open(context.applicationContext)
    val repository=RoomStudyRepository(database)
    val scheduler=FsrsScheduler()
    val review=ReviewService(database,scheduler)
    val backup=BackupService(database)
    val files:FileGateway=BackupFileGateway(context.contentResolver)
}
class XunjiApplication:Application() {
    val container:AppContainer by lazy { AppContainer(this) }
}
