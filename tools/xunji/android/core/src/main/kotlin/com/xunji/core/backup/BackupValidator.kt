package com.xunji.core.backup

import com.xunji.core.model.*
import com.xunji.core.validation.InputValidator

object BackupValidator {
    private const val MAX_TIME=253402300799999L
    private val uuid=Regex("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")
    private fun id(value:String) { require(uuid.matches(value)) { "无效的 ID" } }
    private fun time(value:Long) { require(value in 0..MAX_TIME) { "时间超出范围" } }
    private fun <T> unique(values:List<T>,key:(T)->String) { require(values.map(key).toSet().size==values.size) { "存在重复 ID" } }
    private fun schedule(s:CardSchedule) {
        id(s.cardId);time(s.dueAt);s.lastReviewAt?.let(::time);s.firstReviewedAt?.let(::time)
        require(s.revision in 0 until Long.MAX_VALUE) { "无效的版本号" }
        if(s.phase==Phase.NEW) {
            require(s.step==null && s.stability==null && s.difficulty==null && s.lastReviewAt==null && s.firstReviewedAt==null) { "新卡状态不一致" }
        } else {
            require(s.stability!=null && s.stability.isFinite() && s.stability>=0.001) { "无效的记忆稳定性" }
            require(s.difficulty!=null && s.difficulty.isFinite() && s.difficulty in 1.0..10.0) { "无效的记忆难度" }
            require(s.lastReviewAt!=null && s.firstReviewedAt!=null && s.firstReviewedAt<=s.lastReviewAt) { "缺少学习时间" }
            require(s.revision>0) { "已学卡版本号无效" }
            require(when(s.phase) { Phase.LEARNING->s.step in 0..1;Phase.RELEARNING->s.step==0;Phase.REVIEW->s.step==null;else->false }) { "学习步骤无效" }
        }
    }
    fun validate(d:BackupDocument) {
        require(d.format=="xunji-backup" && d.schemaVersion==1 && d.settings.schemaVersion==1) { "不支持的备份格式或版本" }
        time(d.exportedAt);require(d.settings.newCardsPerDay in 0..100)
        unique(d.decks) { it.id };unique(d.cards) { it.id };unique(d.schedules) { it.cardId };unique(d.reviewLogs) { it.id }
        val decks=d.decks.associateBy { it.id };val cards=d.cards.associateBy { it.id }
        d.decks.forEach { id(it.id);InputValidator.deckName(it.name);time(it.createdAt);time(it.updatedAt);require(it.updatedAt>=it.createdAt) }
        d.cards.forEach { id(it.id);require(it.deckId in decks);InputValidator.cardContent(it.front,it.back,it.example);time(it.createdAt);time(it.updatedAt);require(it.updatedAt>=it.createdAt) }
        require(d.schedules.map { it.cardId }.toSet()==cards.keys) { "卡片与调度状态不对应" }
        d.schedules.forEach(::schedule)
        d.reviewLogs.forEach { log ->
            id(log.id);id(log.sessionId);time(log.reviewedAt);log.undoneAt?.let(::time)
            val card=cards[log.cardId];require(card!=null && card.deckId==log.deckId) { "记录与牌组不对应" }
            require(log.durationMs in 0..300_000)
            schedule(log.beforeSchedule);schedule(log.afterSchedule)
            require(log.beforeSchedule.cardId==log.cardId && log.afterSchedule.cardId==log.cardId) { "记录快照与卡片不对应" }
            require(log.afterSchedule.revision==log.beforeSchedule.revision+1 && log.afterSchedule.phase!=Phase.NEW && log.afterSchedule.lastReviewAt==log.reviewedAt) { "评分快照不一致" }
            require(log.afterSchedule.firstReviewedAt==(log.beforeSchedule.firstReviewedAt ?: log.reviewedAt)) { "首次学习时间不一致" }
        }
    }
}
