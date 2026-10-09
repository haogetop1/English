package com.xunji.flashcards.data

import com.xunji.core.model.*
import com.xunji.core.validation.InputValidator
import androidx.room.withTransaction
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.util.UUID

class RoomStudyRepository(private val db:XunjiDatabase,private val clock:Clock=Clock.systemUTC(),private val idFactory:()->String={UUID.randomUUID().toString()}):StudyRepository {
    private val dao=db.dao()
    override fun observeDecks()=dao.watchDecks().map { rows -> rows.map { it.toModel() } }
    override fun observeCards(deckId:String)=dao.watchCandidates(deckId).map { rows -> rows.map { it.toModel() } }
    override fun observeSchedules()=dao.watchSchedules().map { rows -> rows.map { it.toModel() } }
    override fun observeLogs()=dao.watchLogs().map { rows -> rows.map { it.toModel() } }
    override fun observeSettings()=dao.watchSettings().map { it?.toModel()?:AppSettings() }
    override suspend fun candidates(deckId:String?)=dao.candidates(deckId).map { it.toModel() }
    override suspend fun createDeck(name:String):Deck {
        val deck=Deck(idFactory(),InputValidator.deckName(name),clock.millis(),clock.millis())
        dao.insertDeck(deck.toEntity())
        return deck
    }
    override suspend fun renameDeck(id:String,name:String)=db.withTransaction {
        val validName=InputValidator.deckName(name)
        val deck=requireNotNull(dao.deck(id)) { "牌组已被删除" }
        dao.updateDeck(deck.copy(name=validName,updatedAt=maxOf(clock.millis(),deck.createdAt)))
    }
    override suspend fun deleteDeck(id:String)=db.withTransaction { dao.deleteDeck(id) }
    override suspend fun saveCard(id:String?,deckId:String,front:String,back:String,example:String):Flashcard=db.withTransaction {
        InputValidator.cardContent(front,back,example)
        requireNotNull(dao.deck(deckId)) { "牌组已被删除" }
        val previous=if(id!=null) requireNotNull(dao.card(id)) { "卡片已被删除" } else null
        require(previous==null || previous.deckId==deckId) { "卡片不属于此牌组" }
        val now=clock.millis()
        val card=Flashcard(id?:idFactory(),deckId,front,back,example,previous?.createdAt?:now,maxOf(now,previous?.createdAt?:now))
        if(previous==null) {
            dao.insertCard(card.toEntity())
            dao.putSchedule(CardSchedule(card.id,Phase.NEW,null,null,null,now,null,null,0).toEntity())
        } else dao.updateCard(card.toEntity())
        card
    }
    override suspend fun deleteCard(id:String)=db.withTransaction { dao.deleteCard(id) }
    override suspend fun saveSettings(settings:AppSettings) {
        require(settings.newCardsPerDay in 0..100 && settings.schemaVersion==1) { "每天新卡数量应为 0～100" }
        dao.putSettings(settings.toEntity())
    }
}
