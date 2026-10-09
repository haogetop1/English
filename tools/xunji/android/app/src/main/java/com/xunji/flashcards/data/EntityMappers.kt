package com.xunji.flashcards.data

import com.xunji.core.model.*
import com.xunji.core.serialization.ScheduleCodec

fun DeckEntity.toModel()=Deck(id,name,createdAt,updatedAt)
fun Deck.toEntity()=DeckEntity(id,name,createdAt,updatedAt)
fun CardEntity.toModel()=Flashcard(id,deckId,front,back,example,createdAt,updatedAt)
fun Flashcard.toEntity()=CardEntity(id,deckId,front,back,example,createdAt,updatedAt)
fun ScheduleEntity.toModel()=CardSchedule(cardId,Phase.valueOf(phase),step,stability,difficulty,dueAt,lastReviewAt,firstReviewedAt,revision)
fun CardSchedule.toEntity()=ScheduleEntity(cardId,phase.name,step,stability,difficulty,dueAt,lastReviewAt,firstReviewedAt,revision)
fun ReviewLogEntity.toModel()=ReviewLog(id,cardId,deckId,sessionId,Rating.entries[rating-1],reviewedAt,durationMs,ScheduleCodec.decode(beforeSchedule),ScheduleCodec.decode(afterSchedule),undoneAt)
fun ReviewLog.toEntity()=ReviewLogEntity(id,cardId,deckId,sessionId,rating.ordinal+1,reviewedAt,durationMs,ScheduleCodec.encode(beforeSchedule),ScheduleCodec.encode(afterSchedule),undoneAt)
fun SettingsEntity.toModel()=AppSettings(newCardsPerDay,ThemeMode.valueOf(themeMode),schemaVersion)
fun AppSettings.toEntity()=SettingsEntity(0,newCardsPerDay,themeMode.name,schemaVersion)
fun CardRelations.toModel()=CardCandidate(card.toModel(),schedule.toModel(),deck.name)
