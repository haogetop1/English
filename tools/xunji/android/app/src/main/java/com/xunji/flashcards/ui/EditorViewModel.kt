package com.xunji.flashcards.ui

import androidx.lifecycle.*
import com.xunji.core.validation.*
import com.xunji.flashcards.data.StudyRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class EditorViewModel(private val repo:StudyRepository,private val deckId:String,private val cardId:String?=null,private val handle:SavedStateHandle=SavedStateHandle()):ViewModel() {
    private val mutable=MutableStateFlow(EditorUiState(front=handle["front"]?:"",back=handle["back"]?:"",example=handle["example"]?:"",dirty=handle["dirty"]?:false,loading=cardId!=null))
    val state:StateFlow<EditorUiState> = mutable.asStateFlow()
    init {
        if(cardId!=null)viewModelScope.launch {
            try {
                val card=repo.candidates(deckId).first { it.card.id==cardId }.card
                if(!mutable.value.dirty)mutable.value=mutable.value.copy(front=card.front,back=card.back,example=card.example)
                mutable.value=mutable.value.copy(loading=false)
            } catch(e:CancellationException) { throw e } catch(_:Exception) { mutable.value=mutable.value.copy(loading=false,error="卡片无法读取，请返回重试") }
        }
    }
    fun update(field:String,value:String) {
        if(mutable.value.saving || mutable.value.loading || mutable.value.saved)return
        handle[field]=value;handle["dirty"]=true
        mutable.value=when(field) { "front"->mutable.value.copy(front=value);"back"->mutable.value.copy(back=value);else->mutable.value.copy(example=value) }.copy(dirty=true,errors=emptyMap(),error=null)
    }
    fun save() {
        val s=mutable.value;if(s.saving || s.saved || s.loading)return
        try { InputValidator.cardContent(s.front,s.back,s.example) } catch(e:FieldValidationException) { mutable.value=s.copy(errors=mapOf(e.field to e.message.orEmpty()));return }
        mutable.value=s.copy(saving=true,errors=emptyMap(),error=null)
        viewModelScope.launch {
            try { repo.saveCard(cardId,deckId,s.front,s.back,s.example);handle["dirty"]=false;mutable.value=mutable.value.copy(saving=false,saved=true,dirty=false) }
            catch(e:CancellationException) { throw e } catch(_:Exception) { mutable.value=mutable.value.copy(saving=false,error="保存未完成，请重试") }
        }
    }
}
