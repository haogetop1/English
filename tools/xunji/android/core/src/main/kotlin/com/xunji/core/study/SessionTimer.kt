package com.xunji.core.study
/** Foreground elapsed time only. All inputs use the same monotonic clock. */
class SessionTimer {
    private var accumulated=0L
    private var started:Long?=null
    fun resume(monotonicMs:Long) { if(started==null)started=monotonicMs }
    fun pause(monotonicMs:Long) { accumulated=elapsedMs(monotonicMs);started=null }
    fun reset() { accumulated=0;started=null }
    fun elapsedMs(monotonicMs:Long):Long=(accumulated+(started?.let { (monotonicMs-it).coerceAtLeast(0) } ?: 0)).coerceIn(0,300_000)
}
