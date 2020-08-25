package com.malinskiy.marathon.android.executor.logcat

import com.malinskiy.marathon.android.executor.logcat.BatchLogSaver.SaveEntry
import com.malinskiy.marathon.android.executor.logcat.model.LogcatEvent
import com.malinskiy.marathon.android.executor.logcat.parse.LogcatEventsListener
import com.malinskiy.marathon.device.Device
import com.malinskiy.marathon.log.MarathonLogging
import com.malinskiy.marathon.report.logs.BatchLogs
import com.malinskiy.marathon.report.logs.LogEvent
import com.malinskiy.marathon.report.logs.LogReport
import com.malinskiy.marathon.report.logs.LogTest
import com.malinskiy.marathon.report.logs.LogsProvider
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout

class LogcatCollector : LogcatEventsListener, LogsProvider {

    private val logger = MarathonLogging.logger(LogcatCollector::class.java.simpleName)

    private val devices: MutableMap<Device, DeviceState> = hashMapOf()
    private val batchCollectors: MutableMap<String, BatchLogSaver> = hashMapOf()

    @Synchronized
    override fun onLogcatEvent(event: LogcatEvent) {
        when (event) {
            is LogcatEvent.Message -> {
                val currentBatchId: String = devices[event.device]?.currentBatchId ?: return
                val currentTest = devices[event.device]?.currentTest?.test
                val batchCollector = batchCollectors.getOrPut(currentBatchId) { BatchLogSaver() }
                val entry = SaveEntry.Message(event.logcatMessage)
                batchCollector.save(entry, currentTest)
            }
            is LogcatEvent.FatalError -> {
                val currentBatchId: String = devices[event.device]?.currentBatchId ?: return
                val currentTestState = devices[event.device]?.currentTest
                val batchCollector = batchCollectors.getOrPut(currentBatchId) { BatchLogSaver() }
                val entryToSave = SaveEntry.Event(LogEvent.Crash(event.message))

                if (currentTestState == null) {
                    batchCollector.save(entryToSave, null)
                } else {
                    if (currentTestState.processId == event.processId) {
                        batchCollector.save(entryToSave, currentTestState.test)
                    }
                }
            }
            is LogcatEvent.BatchStarted -> {
                devices.compute(event.device) { _, oldState ->
                    val state = oldState ?: DeviceState()
                    state.copy(currentBatchId = event.batchId, currentTest = null)
                }
            }
            is LogcatEvent.BatchFinished -> {
                val oldState = devices[event.device]
                if (oldState?.currentBatchId == null) {
                    logger.error { "Incorrect state: batch ${event.batchId} finished but not started (state = ${oldState})" }
                    return
                }
                batchCollectors[event.batchId]?.onBatchFinished()
                devices[event.device] = oldState.copy(currentBatchId = null, currentTest = null)
            }
            is LogcatEvent.TestStarted -> {
                val oldState = devices[event.device]
                if (oldState?.currentBatchId == null) {
                    logger.error { "Incorrect state: test ${event.test} started but no active batches found (state = ${oldState})" }
                    return
                }

                devices[event.device] = oldState.copy(currentTest = TestState(event.test, event.processId))
            }
            is LogcatEvent.TestFinished -> {
                val oldState = devices[event.device]
                if (oldState?.currentBatchId == null) {
                    logger.error { "Incorrect state: test ${event.test} finished but no active batches found (state = ${oldState})" }
                    return
                }

                if (oldState.currentTest?.test != event.test) {
                    logger.error { "Incorrect state: test ${event.test} finished but current active test is ${oldState.currentTest}" }
                    return
                }

                batchCollectors[oldState.currentBatchId]?.onTestFinished(event.test)
                devices[event.device] = oldState.copy(currentTest = null)
            }
            is LogcatEvent.DeviceDisconnected -> {
                val currentBatchId = devices[event.device]?.currentBatchId
                batchCollectors[currentBatchId]?.onBatchFinished()
                devices[event.device] = DeviceState()
            }
        }
    }

    @Synchronized
    override fun getFullReport(): LogReport =
        runBlocking {
            LogReport(batchCollectors.mapValues { it.value.getBatchLogs(forceCreate = true) })
        }

    override suspend fun getBatchReport(batchId: String): BatchLogs? {
        val deferred: Deferred<BatchLogs?> = GlobalScope.async {
            batchCollectors[batchId]?.getBatchLogs(forceCreate = false)
        }

        withTimeout(GET_BATCH_REPORT_TIMEOUT_MILLIS) { deferred.await() }

        return if (deferred.isCompleted) {
            deferred.getCompleted()
        } else {
            null
        }
    }

    private data class DeviceState(
        val currentBatchId: String? = null,
        val currentTest: TestState? = null
    )

    private data class TestState(
        val test: LogTest,
        val processId: Int
    )

    private companion object {
        private const val GET_BATCH_REPORT_TIMEOUT_MILLIS = 60 * 1000L
    }
}

