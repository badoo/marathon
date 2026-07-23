package com.malinskiy.marathon.time

class StubTimer : Timer {
    private var counter = 0L

    override var startTimeMillis: Long = 0L
    override var elapsedTimeMillis: Long = 0L

    override fun currentTimeMillis(): Long = counter++
    override fun measure(block: () -> Unit): Long = 0L
}
