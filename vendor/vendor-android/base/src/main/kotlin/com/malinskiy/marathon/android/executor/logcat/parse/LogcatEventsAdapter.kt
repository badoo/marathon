package com.malinskiy.marathon.android.executor.logcat.parse

import com.malinskiy.marathon.android.AndroidDevice
import com.malinskiy.marathon.android.executor.logcat.LogcatListener
import com.malinskiy.marathon.android.executor.logcat.model.LogcatEvent.BatchFinished
import com.malinskiy.marathon.android.executor.logcat.model.LogcatEvent.BatchStarted
import com.malinskiy.marathon.android.executor.logcat.model.LogcatEvent.DeviceDisconnected
import com.malinskiy.marathon.android.executor.logcat.model.LogcatEvent.FatalError
import com.malinskiy.marathon.android.executor.logcat.model.LogcatEvent.Message
import com.malinskiy.marathon.android.executor.logcat.model.LogcatEvent.TestFinished
import com.malinskiy.marathon.android.executor.logcat.model.LogcatEvent.TestStarted
import com.malinskiy.marathon.android.executor.logcat.model.LogcatMessage
import com.malinskiy.marathon.report.logs.LogTest

class LogcatEventsAdapter(private val parsedEventsListener: LogcatEventsListener) : LogcatListener {
    override fun onMessage(device: AndroidDevice, message: LogcatMessage) {
        message.parseBatchStarted(device)?.let { parsedEventsListener.onLogcatEvent(it) }
        message.parseTestStarted(device)?.let { parsedEventsListener.onLogcatEvent(it) }

        parsedEventsListener.onLogcatEvent(Message(message, device))
        message.parseNativeCrash(device)?.let { parsedEventsListener.onLogcatEvent(it) }
        message.parseZygoteExitReason(device)?.let { parsedEventsListener.onLogcatEvent(it) }
        message.parseJvmCrash(device)?.let { parsedEventsListener.onLogcatEvent(it) }

        message.parseTestFinished(device)?.let { parsedEventsListener.onLogcatEvent(it) }
        message.parseBatchFinished(device)?.let { parsedEventsListener.onLogcatEvent(it) }
    }

    override fun onDeviceDisconnected(device: AndroidDevice) {
        parsedEventsListener.onLogcatEvent(DeviceDisconnected(device))
    }

    private fun LogcatMessage.parseNativeCrash(device: AndroidDevice): FatalError? =
        if (tag == NATIVE_CRASH_REPORT_TAG && body.startsWith(NATIVE_CRASH_REPORT_PREFIX)) {
            FatalError(body, processId, device)
        } else {
            null
        }

    private fun LogcatMessage.parseZygoteExitReason(device: AndroidDevice): FatalError? {
        if (tag == ZYGOTE_TAG) {
            if (body.contains(ZYGOTE_KILLED_EXTERNALLY_EXIT_REASON)) {
                return null
            }

            val match = ZYGOTE_PROCESS_EXITED_REGEXP.find(body)
            val processId = match?.groups?.get(1)?.value?.toIntOrNull()
            if (processId != null) {
                return FatalError(body, processId, device)
            }
        }

        return null
    }

    private fun LogcatMessage.parseJvmCrash(device: AndroidDevice): FatalError? = if (tag == JVM_CRASH_REPORT_TAG && body.startsWith(JVM_CRASH_REPORT_PREFIX)) {
        FatalError(body, processId, device)
    } else {
        null
    }

    private fun String.parseTestName(): LogTest {
        val (methodName, packageAndClass) = this.removeSuffix(")").split("(")
        val dotIndex = packageAndClass.lastIndexOf('.')

        return if (dotIndex < 0) {
            LogTest("", packageAndClass, methodName)
        } else {
            val pkg = packageAndClass.substring(0, dotIndex)
            val clazz = packageAndClass.substring(dotIndex + 1)
            LogTest(pkg, clazz, methodName)
        }
    }

    private fun LogcatMessage.parseTestFinished(device: AndroidDevice): TestFinished? = if (tag == TEST_RUNNER_TAG && body.startsWith(TEST_FINISHED_PREFIX)) {
        val test = body.removePrefix(TEST_FINISHED_PREFIX).parseTestName()
        TestFinished(test, processId, device)
    } else {
        null
    }

    private fun LogcatMessage.parseTestStarted(device: AndroidDevice): TestStarted? = if (tag == TEST_RUNNER_TAG && body.startsWith(TEST_STARTED_PREFIX)) {
        val test = body.removePrefix(TEST_STARTED_PREFIX).parseTestName()
        TestStarted(test, processId, device)
    } else {
        null
    }

    private fun LogcatMessage.parseBatchStarted(device: AndroidDevice): BatchStarted? = if (tag == MARATHON_TAG && body.startsWith(BATCH_STARTED_PREFIX)) {
        val batchId = body.removePrefix(BATCH_STARTED_PREFIX).removeSurrounding("{", "}")
        BatchStarted(batchId, device)
    } else {
        null
    }

    private fun LogcatMessage.parseBatchFinished(device: AndroidDevice): BatchFinished? = if (tag == MARATHON_TAG && body.startsWith(BATCH_FINISHED_PREFIX)) {
        val batchId = body.removePrefix(BATCH_FINISHED_PREFIX).removeSurrounding("{", "}")
        BatchFinished(batchId, device)
    } else {
        null
    }

    private companion object {
        private const val TEST_RUNNER_TAG = "TestRunner"
        private const val MARATHON_TAG = "marathon"

        private const val NATIVE_CRASH_REPORT_TAG = "libc"
        private const val NATIVE_CRASH_REPORT_PREFIX = "Fatal signal"

        private const val ZYGOTE_TAG = "Zygote"
        private val ZYGOTE_PROCESS_EXITED_REGEXP = "Process ([0-9]+) exited due to signal.*".toRegex()
        private const val ZYGOTE_KILLED_EXTERNALLY_EXIT_REASON = "exited due to signal 9"

        private const val JVM_CRASH_REPORT_TAG = "AndroidRuntime"
        private const val JVM_CRASH_REPORT_PREFIX = "FATAL EXCEPTION"

        private const val TEST_STARTED_PREFIX = "started: "
        private const val TEST_FINISHED_PREFIX = "finished: "

        private const val BATCH_STARTED_PREFIX = "batch_started: "
        private const val BATCH_FINISHED_PREFIX = "batch_finished: "
    }
}
