package com.xunji.flashcards.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities=[DeckEntity::class,CardEntity::class,ScheduleEntity::class,ReviewLogEntity::class,SettingsEntity::class],version=1,exportSchema=true)
abstract class XunjiDatabase:RoomDatabase() {
    abstract fun dao():XunjiDao
    companion object {
        fun open(context:Context)=Room.databaseBuilder(context,XunjiDatabase::class.java,"xunji.db").build()
    }
}
