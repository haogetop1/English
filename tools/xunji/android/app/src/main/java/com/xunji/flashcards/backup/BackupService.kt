package com.xunji.flashcards.backup

import androidx.room.withTransaction
import com.xunji.core.backup.*
import com.xunji.core.model.AppSettings
import com.xunji.flashcards.data.*
import java.io.InputStream
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.time.Clock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class PreparedBackup internal constructor(val document:BackupDocument) {
    val deckCount get()=document.decks.size
    val cardCount get()=document.cards.size
    val logCount get()=document.reviewLogs.size
}
class BackupService(private val db:XunjiDatabase,private val clock:Clock=Clock.systemUTC()) {
    suspend fun export():BackupDocument=db.withTransaction {
        val dao=db.dao()
        BackupDocument(exportedAt=clock.millis(),decks=dao.decks().map { it.toModel() },cards=dao.cards().map { it.toModel() },schedules=dao.schedules().map { it.toModel() },reviewLogs=dao.logs().map { it.toModel() },settings=dao.settings()?.toModel() ?: AppSettings())
    }
    suspend fun prepare(input:InputStream):PreparedBackup=withContext(Dispatchers.IO) {
        input.use { stream ->
            val output=ByteArrayOutputStream();val buffer=ByteArray(8192);var total=0L
            while(true) {
                val n=stream.read(buffer);if(n<0)break;if(n==0)continue
                total+=n;require(total<=50L*1024*1024) { "备份超过 50 MiB 上限" };output.write(buffer,0,n)
            }
            val decoder=Charsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT)
            val text=try { decoder.decode(ByteBuffer.wrap(output.toByteArray())).toString() } catch(e:java.nio.charset.CharacterCodingException) { throw IllegalArgumentException("备份不是有效的 UTF-8 文件",e) }
            PreparedBackup(BackupCodec.decode(text))
        }
    }
    suspend fun replace(prepared:PreparedBackup) {
        BackupValidator.validate(prepared.document)
        db.withTransaction {
            val dao=db.dao();val d=prepared.document
            dao.deleteAllDecks()
            d.decks.forEach { dao.insertDeck(it.toEntity()) };d.cards.forEach { dao.insertCard(it.toEntity()) }
            d.schedules.forEach { dao.putSchedule(it.toEntity()) };d.reviewLogs.forEach { dao.insertLog(it.toEntity()) }
            dao.putSettings(d.settings.toEntity())
        }
    }
}
