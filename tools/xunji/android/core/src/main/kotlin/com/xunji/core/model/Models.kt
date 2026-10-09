package com.xunji.core.model

import kotlinx.serialization.Serializable

@Serializable enum class Phase { NEW, LEARNING, REVIEW, RELEARNING }
@Serializable enum class Rating { AGAIN, HARD, GOOD, EASY }
@Serializable enum class ThemeMode { SYSTEM, LIGHT, DARK }
@Serializable data class Deck(val id:String,val name:String,val createdAt:Long,val updatedAt:Long)
@Serializable data class Flashcard(val id:String,val deckId:String,val front:String,val back:String,val example:String,val createdAt:Long,val updatedAt:Long)
@Serializable data class CardSchedule(val cardId:String,val phase:Phase,val step:Int?,val stability:Double?,val difficulty:Double?,val dueAt:Long,val lastReviewAt:Long?,val firstReviewedAt:Long?,val revision:Long)
@Serializable data class AppSettings(val newCardsPerDay:Int=20,val themeMode:ThemeMode=ThemeMode.SYSTEM,val schemaVersion:Int=1)
data class CardCandidate(val card:Flashcard,val schedule:CardSchedule,val deckName:String)
data class CardItem(val candidate:CardCandidate)
data class DeckSummary(val deck:Deck,val total:Int,val due:Int,val newCards:Int)
