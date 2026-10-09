package com.xunji.core.serialization
import com.xunji.core.model.*
import org.junit.Assert.*
import org.junit.Test
class ScheduleCodecTest {
    @Test fun nullableNewStateRoundTrips() {
        val state=CardSchedule("id",Phase.NEW,null,null,null,1000,null,null,0)
        assertEquals(state,ScheduleCodec.decode(ScheduleCodec.encode(state)))
    }
    @Test fun learnedStateRoundTripsAllFields() {
        val state=CardSchedule("id",Phase.RELEARNING,0,1.234,5.678,5000,2000,1000,9)
        assertEquals(state,ScheduleCodec.decode(ScheduleCodec.encode(state)))
    }
}
