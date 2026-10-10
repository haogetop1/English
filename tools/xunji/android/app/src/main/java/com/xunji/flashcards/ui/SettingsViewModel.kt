package com.xunji.flashcards.ui

import android.net.Uri
import androidx.lifecycle.*
import com.xunji.core.model.*
import com.xunji.flashcards.backup.*
import com.xunji.flashcards.data.StudyRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class SettingsViewModel(private val repo:StudyRepository,private val backup:BackupService,private val files:FileGateway,private val restored:()->Unit):ViewModel() {
    private val mutable=MutableStateFlow(SettingsUiState())
    val state:StateFlow<SettingsUiState> = mutable.asStateFlow()
    init { viewModelScope.launch { repo.observeSettings().collect { settings->mutable.value=mutable.value.copy(settings=settings) } } }
    private fun operation(block:suspend ()->Unit) {
        if(mutable.value.busy)return
        mutable.value=mutable.value.copy(busy=true,message=null,error=null)
        viewModelScope.launch {
            try { block() } catch(e:CancellationException) { throw e } catch(_:Exception) { mutable.value=mutable.value.copy(error="操作未完成，请检查文件或重试",message=null) }
            finally { mutable.value=mutable.value.copy(busy=false) }
        }
    }
    fun saveSettings(settings:AppSettings)=operation { repo.saveSettings(settings);mutable.value=mutable.value.copy(message="设置已保存") }
    fun prepareRestore(uri:Uri)=operation { mutable.value=mutable.value.copy(prepared=null);val prepared=backup.prepare(files.read(uri));mutable.value=mutable.value.copy(prepared=prepared) }
    fun confirmRestore() {
        val prepared=mutable.value.prepared ?: return
        operation { backup.replace(prepared);restored();mutable.value=mutable.value.copy(prepared=null,message="备份已恢复") }
    }
    fun cancelRestore() { if(!mutable.value.busy)mutable.value=mutable.value.copy(prepared=null) }
    fun export(uri:Uri)=operation { files.write(uri,backup.export());mutable.value=mutable.value.copy(message="备份已导出") }
}
