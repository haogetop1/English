package com.xunji.flashcards.data

import androidx.room.*

@Entity(tableName="decks")
data class DeckEntity(@PrimaryKey val id:String,val name:String,val createdAt:Long,val updatedAt:Long)

@Entity(tableName="cards",foreignKeys=[ForeignKey(entity=DeckEntity::class,parentColumns=["id"],childColumns=["deckId"],onDelete=ForeignKey.CASCADE)],indices=[Index("deckId")])
data class CardEntity(@PrimaryKey val id:String,val deckId:String,val front:String,val back:String,val example:String,val createdAt:Long,val updatedAt:Long)

@Entity(tableName="schedules",foreignKeys=[ForeignKey(entity=CardEntity::class,parentColumns=["id"],childColumns=["cardId"],onDelete=ForeignKey.CASCADE)],indices=[Index("dueAt"),Index("firstReviewedAt")])
data class ScheduleEntity(@PrimaryKey val cardId:String,val phase:String,val step:Int?,val stability:Double?,val difficulty:Double?,val dueAt:Long,val lastReviewAt:Long?,val firstReviewedAt:Long?,val revision:Long)

@Entity(tableName="review_logs",foreignKeys=[
    ForeignKey(entity=CardEntity::class,parentColumns=["id"],childColumns=["cardId"],onDelete=ForeignKey.CASCADE),
    ForeignKey(entity=DeckEntity::class,parentColumns=["id"],childColumns=["deckId"],onDelete=ForeignKey.CASCADE)
],indices=[Index("cardId"),Index("deckId"),Index("sessionId"),Index("reviewedAt")])
data class ReviewLogEntity(@PrimaryKey val id:String,val cardId:String,val deckId:String,val sessionId:String,val rating:Int,val reviewedAt:Long,val durationMs:Long,val beforeSchedule:String,val afterSchedule:String,val undoneAt:Long?)

@Entity(tableName="settings")
data class SettingsEntity(@PrimaryKey val id:Int=0,val newCardsPerDay:Int,val themeMode:String,val schemaVersion:Int=1)

data class CardRelations(
    @Embedded val card:CardEntity,
    @Relation(parentColumn="id",entityColumn="cardId") val schedule:ScheduleEntity,
    @Relation(parentColumn="deckId",entityColumn="id") val deck:DeckEntity,
)
