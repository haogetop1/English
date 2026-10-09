package com.xunji.flashcards.data

import com.xunji.core.model.*
import kotlinx.coroutines.flow.Flow

interface StudyRepository {
    fun observeDecks():Flow<List<Deck>>
    fun observeCards(deckId:String):Flow<List<CardCandidate>>
    fun observeSchedules():Flow<List<CardSchedule>>
    fun observeLogs():Flow<List<ReviewLog>>
    fun observeSettings():Flow<AppSettings>
    suspend fun createDeck(name:String):Deck
    suspend fun renameDeck(id:String,name:String)
    suspend fun deleteDeck(id:String)
    suspend fun saveCard(id:String?,deckId:String,front:String,back:String,example:String):Flashcard
    suspend fun deleteCard(id:String)
    suspend fun candidates(deckId:String?):List<CardCandidate>
    suspend fun saveSettings(settings:AppSettings)
}
