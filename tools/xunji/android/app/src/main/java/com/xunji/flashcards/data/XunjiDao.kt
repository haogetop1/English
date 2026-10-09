package com.xunji.flashcards.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao interface XunjiDao {
    @Query("SELECT * FROM decks ORDER BY createdAt, id") fun watchDecks():Flow<List<DeckEntity>>
    @Query("SELECT * FROM decks ORDER BY createdAt, id") suspend fun decks():List<DeckEntity>
    @Query("SELECT * FROM decks WHERE id=:id") suspend fun deck(id:String):DeckEntity?
    @Insert suspend fun insertDeck(deck:DeckEntity)
    @Update suspend fun updateDeck(deck:DeckEntity)
    @Query("DELETE FROM decks WHERE id=:id") suspend fun deleteDeck(id:String)
    @Query("DELETE FROM decks") suspend fun deleteAllDecks()

    @Transaction @Query("SELECT * FROM cards WHERE (:deckId IS NULL OR deckId=:deckId) ORDER BY createdAt,id")
    fun watchCandidates(deckId:String?):Flow<List<CardRelations>>
    @Transaction @Query("SELECT * FROM cards WHERE (:deckId IS NULL OR deckId=:deckId) ORDER BY createdAt,id")
    suspend fun candidates(deckId:String?):List<CardRelations>
    @Query("SELECT * FROM cards ORDER BY id") suspend fun cards():List<CardEntity>
    @Query("SELECT * FROM cards WHERE id=:id") suspend fun card(id:String):CardEntity?
    @Insert suspend fun insertCard(card:CardEntity)
    @Update suspend fun updateCard(card:CardEntity)
    @Query("DELETE FROM cards WHERE id=:id") suspend fun deleteCard(id:String)

    @Query("SELECT * FROM schedules ORDER BY cardId") fun watchSchedules():Flow<List<ScheduleEntity>>
    @Query("SELECT * FROM schedules ORDER BY cardId") suspend fun schedules():List<ScheduleEntity>
    @Query("SELECT * FROM schedules WHERE cardId=:id") suspend fun schedule(id:String):ScheduleEntity?
    @Upsert suspend fun putSchedule(schedule:ScheduleEntity)

    @Query("SELECT * FROM review_logs ORDER BY reviewedAt,id") fun watchLogs():Flow<List<ReviewLogEntity>>
    @Query("SELECT * FROM review_logs ORDER BY reviewedAt,id") suspend fun logs():List<ReviewLogEntity>
    @Query("SELECT * FROM review_logs WHERE id=:id") suspend fun log(id:String):ReviewLogEntity?
    @Insert suspend fun insertLog(log:ReviewLogEntity)
    @Update suspend fun updateLog(log:ReviewLogEntity)
    @Query("SELECT * FROM review_logs WHERE sessionId=:sessionId ORDER BY rowid DESC LIMIT 1") suspend fun latestSessionLog(sessionId:String):ReviewLogEntity?

    @Query("SELECT * FROM settings WHERE id=0") fun watchSettings():Flow<SettingsEntity?>
    @Query("SELECT * FROM settings WHERE id=0") suspend fun settings():SettingsEntity?
    @Upsert suspend fun putSettings(settings:SettingsEntity)
}
