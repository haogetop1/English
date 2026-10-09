package com.xunji.core.model

import kotlinx.serialization.Serializable

@Serializable data class ReviewLog(val id:String,val cardId:String,val deckId:String,val sessionId:String,val rating:Rating,val reviewedAt:Long,val durationMs:Long,val beforeSchedule:CardSchedule,val afterSchedule:CardSchedule,val undoneAt:Long?)
