package com.juleah.chickens.sim

interface Clock {
    fun nowMs(): Long
}

class SystemClock : Clock {
    override fun nowMs(): Long = System.currentTimeMillis()
}

class FakeClock(private var time: Long = 0L) : Clock {
    override fun nowMs(): Long = time
    fun advanceBy(ms: Long) { time += ms }
    fun set(ms: Long) { time = ms }
}
