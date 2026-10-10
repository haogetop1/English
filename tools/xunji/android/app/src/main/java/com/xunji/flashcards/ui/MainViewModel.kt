package com.xunji.flashcards.ui

import androidx.lifecycle.*
import com.xunji.core.model.*
import com.xunji.core.stats.StudyStatistics
import com.xunji.core.study.NewCardQuota
import com.xunji.core.validation.FieldValidationException
import com.xunji.flashcards.data.StudyRepository
import java.time.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class MainViewModel(private val repo:StudyRepository,private val clock:Clock=Clock.systemUTC()):ViewModel() {
    private val mutable=MutableStateFlow(MainUiState())
    val state:StateFlow<MainUiState> = mutable.asStateFlow()
    private val moment=MutableStateFlow(clock.instant() to ZoneId.systemDefault())
    init {
        viewModelScope.launch {
            combine(repo.observeDecks(),repo.observeAllCards(),repo.observeLogs(),repo.observeSettings(),moment) { decks,cards,logs,settings,time ->
                val(at,zone)=time
                val remaining=NewCardQuota.remaining(cards.map { it.schedule },settings,at,zone)
                fun due(c:CardCandidate)=c.schedule.phase!=Phase.NEW && c.schedule.dueAt<=at.toEpochMilli()
                MainUiState(loading=false,decks=decks.map { deck -> val subset=cards.filter { it.card.deckId==deck.id };DeckSummary(deck,subset.size,subset.count(::due),subset.count { it.schedule.phase==Phase.NEW }) },cards=cards,due=cards.count(::due),availableNew=minOf(remaining,cards.count { it.schedule.phase==Phase.NEW }),learnedNew=settings.newCardsPerDay-remaining,statistics=StudyStatistics.summarize(logs,at,zone),settings=settings)
            }.catch { emit(MainUiState(loading=false,error="无法读取数据，请重启应用后重试")) }.collect { mutable.value=it }
        }
    }
    fun refresh(at:Instant=clock.instant(),zone:ZoneId=ZoneId.systemDefault()) { moment.value=at to zone }
    suspend fun createDeck(name:String):String=repo.createDeck(name).id
    suspend fun renameDeck(id:String,name:String)=repo.renameDeck(id,name)
    suspend fun deleteDeck(id:String)=repo.deleteDeck(id)
    suspend fun deleteCard(id:String)=repo.deleteCard(id)
}
