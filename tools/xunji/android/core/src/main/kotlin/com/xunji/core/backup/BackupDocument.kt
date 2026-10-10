package com.xunji.core.backup

import com.xunji.core.model.*
import kotlinx.serialization.Serializable

@Serializable data class BackupDocument(val format:String="xunji-backup",val schemaVersion:Int=1,val exportedAt:Long,val decks:List<Deck>,val cards:List<Flashcard>,val schedules:List<CardSchedule>,val reviewLogs:List<ReviewLog>,val settings:AppSettings)
