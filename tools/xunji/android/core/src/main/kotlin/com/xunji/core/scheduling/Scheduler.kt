package com.xunji.core.scheduling

import com.xunji.core.model.*
import java.time.Instant

interface Scheduler {
    fun review(schedule:CardSchedule,rating:Rating,at:Instant):CardSchedule
    fun preview(schedule:CardSchedule,at:Instant):Map<Rating,CardSchedule>
}
