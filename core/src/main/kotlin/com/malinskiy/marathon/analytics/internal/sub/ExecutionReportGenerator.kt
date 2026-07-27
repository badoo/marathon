package com.malinskiy.marathon.analytics.internal.sub

import com.malinskiy.marathon.log.MarathonLogging
import com.malinskiy.marathon.report.Reporter
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import java.util.Collections
import java.util.LinkedList

class ExecutionReportGenerator(
    private val reporters: List<Reporter>,
    private val testEventInflators: List<TestEventInflator>,
    private val ioDispatcher: CoroutineDispatcher
) : TrackerInternal {

    private val logger = MarathonLogging.getLogger(ExecutionReportGenerator::class.java)
    private val devicePreparingEvents: MutableList<DevicePreparingEvent> = Collections.synchronizedList(LinkedList())
    private val deviceConnectedEvents: MutableList<DeviceConnectedEvent> = Collections.synchronizedList(LinkedList())
    private val deviceProviderPreparingEvents: MutableList<DeviceProviderPreparingEvent> = Collections.synchronizedList(LinkedList())
    private val testEvents: MutableList<TestEvent> = Collections.synchronizedList(mutableListOf())
    private val installEvents: MutableList<InstallationEvent> = Collections.synchronizedList(mutableListOf())
    private val installCheckEvents: MutableList<InstallationCheckEvent> = Collections.synchronizedList(mutableListOf())
    private val executingBatchEvent: MutableList<ExecutingBatchEvent> = Collections.synchronizedList(mutableListOf())
    private val cacheStoreEvent: MutableList<CacheStoreEvent> = Collections.synchronizedList(mutableListOf())
    private val cacheLoadEvent: MutableList<CacheLoadEvent> = Collections.synchronizedList(mutableListOf())

    override fun track(event: Event) {
        when (event) {
            is DeviceConnectedEvent -> deviceConnectedEvents.add(event)
            is DevicePreparingEvent -> devicePreparingEvents.add(event)
            is DeviceProviderPreparingEvent -> deviceProviderPreparingEvents.add(event)
            is TestEvent -> testEvents.add(event)
            is InstallationEvent -> installEvents.add(event)
            is InstallationCheckEvent -> installCheckEvents.add(event)
            is ExecutingBatchEvent -> executingBatchEvent.add(event)
            is CacheStoreEvent -> cacheStoreEvent.add(event)
            is CacheLoadEvent -> cacheLoadEvent.add(event)
        }
    }

    override suspend fun finish() {
        val inflatedTestEvents = testEvents.map {
            currentCoroutineContext().ensureActive()
            testEventInflators.fold(it) { event, inflator -> inflator.inflate(event) }
        }

        val report = ExecutionReport(
            deviceConnectedEvents = deviceConnectedEvents.sortedBy { it.instant },
            devicePreparingEvents = devicePreparingEvents.sortedBy { it.start },
            deviceProviderPreparingEvent = deviceProviderPreparingEvents.sortedBy { it.start },
            installCheckEvent = installCheckEvents.sortedBy { it.start },
            installEvent = installEvents.sortedBy { it.start },
            executeBatchEvent = executingBatchEvent.sortedBy { it.start },
            cacheStoreEvent = cacheStoreEvent.sortedBy { it.start },
            cacheLoadEvent = cacheLoadEvent.sortedBy { it.start },
            testEvents = inflatedTestEvents.sortedBy {
                if (it.testResult.isTimeInfoAvailable) {
                    it.testResult.startTime
                } else {
                    it.instant.toEpochMilli()
                }
            }
        )

        logger.info("Generating reports...")
        val failures = generate(reporters, report)

        failures.firstOrNull()?.let { first ->
            failures.drop(1).forEach(first::addSuppressed)
            throw first
        }
    }

    private suspend fun generate(
        reporters: List<Reporter>,
        report: ExecutionReport
    ): List<Exception> {
        val failures = arrayOfNulls<Exception>(reporters.size)
        coroutineScope {
            reporters.forEachIndexed { index, reporter ->
                launch(ioDispatcher) {
                    try {
                        reporter.generate(report)
                    } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                        currentCoroutineContext().ensureActive()
                        logger.error("Failed to generate report with {}", reporter::class.simpleName, e)
                        failures[index] = e
                    }
                }
            }
        }
        return failures.filterNotNull()
    }
}
