package com.malinskiy.marathon.android.executor.logcat.model

import com.malinskiy.marathon.android.AndroidDevice
import com.malinskiy.marathon.report.logs.LogTest

sealed class LogcatEvent {
    abstract val device: AndroidDevice

    data class Message(
        val logcatMessage: LogcatMessage,
        override val device: AndroidDevice,
    ) : LogcatEvent()

    data class BatchStarted(
        val batchId: String,
        override val device: AndroidDevice,
    ) : LogcatEvent()

    data class BatchFinished(
        val batchId: String,
        override val device: AndroidDevice,
    ) : LogcatEvent()

    data class TestStarted(
        val test: LogTest,
        val processId: Int,
        override val device: AndroidDevice,
    ) : LogcatEvent()

    data class TestFinished(
        val test: LogTest,
        val processId: Int,
        override val device: AndroidDevice,
    ) : LogcatEvent()

    data class FatalError(
        val message: String,
        val processId: Int,
        override val device: AndroidDevice,
    ) : LogcatEvent()

    data class DeviceDisconnected(override val device: AndroidDevice) : LogcatEvent()
}
