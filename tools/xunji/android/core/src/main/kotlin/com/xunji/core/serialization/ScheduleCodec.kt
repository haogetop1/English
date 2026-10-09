package com.xunji.core.serialization
import com.xunji.core.model.CardSchedule
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
object ScheduleCodec {
    fun encode(schedule:CardSchedule)=Json.encodeToString(schedule)
    fun decode(json:String):CardSchedule=Json.decodeFromString(json)
}
