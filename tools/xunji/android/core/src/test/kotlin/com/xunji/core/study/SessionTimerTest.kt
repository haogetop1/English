package com.xunji.core.study
import org.junit.Test
import org.junit.Assert.*
class SessionTimerTest {
 @Test fun measuresOnlyForegroundTime() { val t=SessionTimer();t.resume(0);t.pause(2000);t.resume(12000);assertEquals(5000L,t.elapsedMs(15000));t.pause(15000);assertEquals(5000L,t.elapsedMs(90000)) }
 @Test fun repeatedResumeAndPauseDoNotDoubleCount() { val t=SessionTimer();t.resume(100);t.resume(500);t.pause(1100);t.pause(9000);assertEquals(1000L,t.elapsedMs(10000));t.reset();assertEquals(0L,t.elapsedMs(11000)) }
 @Test fun capsFiveMinutesAndClampsNegativeElapsed() { val t=SessionTimer();t.resume(500);assertEquals(0L,t.elapsedMs(400));assertEquals(300000L,t.elapsedMs(900000)) }
}
