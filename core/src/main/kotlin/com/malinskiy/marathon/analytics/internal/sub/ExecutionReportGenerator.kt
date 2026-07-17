package com.malinskiy.marathon.analytics.internal.sub

import com.malinskiy.marathon.report.Reporter
import java.util.Collections
import java.util.LinkedList
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

class ExecutionReportGenerator(
    private val reporters: List<Reporter>,
    private val testEventInflatorsFactory: () -> List<TestEventInflator>
) : TrackerInternal {

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
        currentCoroutineContext().ensureActive()

        val testEventInflators = testEventInflatorsFactory.invoke()

        val testEvents = testEvents
            .map {
                currentCoroutineContext().ensureActive()
                testEventInflators.fold(it) { event, inflator -> inflator.inflate(event) }
            }

        val report = ExecutionReport(
            deviceConnectedEvents.sortedBy { it.instant },
            devicePreparingEvents.sortedBy { it.start },
            deviceProviderPreparingEvents.sortedBy { it.start },
            installCheckEvents.sortedBy { it.start },
            installEvents.sortedBy { it.start },
            executingBatchEvent.sortedBy { it.start },
            cacheStoreEvent.sortedBy { it.start },
            cacheLoadEvent.sortedBy { it.start },
            testEvents.sortedBy {
                if (it.testResult.isTimeInfoAvailable) {
                    it.testResult.startTime
                } else {
                    it.instant.toEpochMilli()
                }
            }
        )

        for (reporter in reporters) {
            currentCoroutineContext().ensureActive()
            reporter.generate(report)
        }
    }
}
