package com.malinskiy.marathon.execution

import com.malinskiy.marathon.actor.Actor
import com.malinskiy.marathon.actor.safeSend
import com.malinskiy.marathon.analytics.external.Analytics
import com.malinskiy.marathon.analytics.internal.pub.Track
import com.malinskiy.marathon.device.Device
import com.malinskiy.marathon.device.DeviceInfo
import com.malinskiy.marathon.device.DevicePoolId
import com.malinskiy.marathon.device.toDeviceInfo
import com.malinskiy.marathon.execution.device.DeviceActor
import com.malinskiy.marathon.execution.device.DeviceEvent
import com.malinskiy.marathon.execution.progress.ProgressReporter
import com.malinskiy.marathon.execution.queue.QueueActor
import com.malinskiy.marathon.execution.queue.QueueMessage
import com.malinskiy.marathon.log.MarathonLogging
import com.malinskiy.marathon.report.logs.LogsProvider
import com.malinskiy.marathon.test.TestBatch
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.delay
import java.util.concurrent.TimeUnit
import kotlin.coroutines.CoroutineContext

class DevicePoolActor(
    private val poolId: DevicePoolId,
    private val configuration: Configuration,
    analytics: Analytics,
    private val progressReporter: ProgressReporter,
    private val track: Track,
    private val logsProvider: LogsProvider,
    private val strictRunChecker: StrictRunChecker,
    parent: Job,
    context: CoroutineContext
) :
    Actor<DevicePoolMessage>(parent = parent, context = context) {

    private val logger = MarathonLogging.logger("DevicePoolActor[${poolId.name}]")

    override suspend fun receive(msg: DevicePoolMessage) {
        when (msg) {
            is DevicePoolMessage.FromScheduler.AddDevice -> addDevice(msg.device)
            is DevicePoolMessage.FromScheduler.AddTests -> addTests(msg.shard)
            is DevicePoolMessage.FromScheduler.RemoveDevice -> removeDevice(msg.device)
            is DevicePoolMessage.FromScheduler.RequestStop -> requestStop()
            is DevicePoolMessage.FromDevice.IsReady -> deviceReady(msg)
            is DevicePoolMessage.FromDevice.CompletedTestBatch -> deviceCompleted(msg.device, msg.results)
            is DevicePoolMessage.FromDevice.ReturnTestBatch -> deviceReturnedTestBatch(msg.device, msg.batch)
            is DevicePoolMessage.FromQueue.Notify -> notifyDevices()
            is DevicePoolMessage.FromQueue.Terminated -> onQueueTerminated()
            is DevicePoolMessage.FromQueue.ExecuteBatch -> executeBatch(msg.device, msg.batch)
        }
    }

    private val poolJob = Job(parent)

    private val queue: QueueActor = QueueActor(
        configuration,
        analytics,
        this,
        poolId,
        progressReporter,
        track,
        logsProvider,
        strictRunChecker,
        poolJob,
        context
    )

    private val devices = mutableMapOf<String, SendChannel<DeviceEvent>>()

    private var noDevicesTimeoutDeferred: Deferred<Unit>? = null

    private suspend fun notifyDevices() {
        logger.debug { "Notify devices" }
        devices.values.forEach {
            it.safeSend(DeviceEvent.WakeUp)
        }
    }

    private suspend fun onQueueTerminated() {
        devices.values.forEach {
            it.safeSend(DeviceEvent.Terminate)
        }
        terminate()
    }

    private suspend fun deviceReturnedTestBatch(device: Device, batch: TestBatch) {
        queue.send(QueueMessage.ReturnBatch(device.toDeviceInfo(), batch))
    }

    private suspend fun deviceCompleted(device: Device, results: TestBatchResults) {
        queue.send(QueueMessage.Completed(device.toDeviceInfo(), results))
    }

    private suspend fun deviceReady(msg: DevicePoolMessage.FromDevice.IsReady) {
        maybeRequestBatch(msg.device)
    }

    // Requests a batch of tests for a random device from the list of devices not running tests at the moment.
    // When @avoidingDevice is not null, attemtps to send the request for any other device whenever available.
    private suspend fun maybeRequestBatch(avoidingDevice: Device? = null) {
        val availableDevices = devices.values.asSequence()
            .map { it as DeviceActor }
            .filter { it.isAvailable }
            .filter { it.device != avoidingDevice }
            .toList()
        if (availableDevices.isEmpty()) {
            if (avoidingDevice != null) {
                devices[avoidingDevice.serialNumber]?.let {
                    val avoidingDeviceActor = it as? DeviceActor
                    if (avoidingDeviceActor?.isAvailable == true) {
                        queue.safeSend(QueueMessage.RequestBatch(avoidingDevice.toDeviceInfo()))
                    }
                }
            }
        } else {
            queue.safeSend(QueueMessage.RequestBatch(availableDevices.shuffled().first().device.toDeviceInfo()))
        }
    }

    private suspend fun executeBatch(device: DeviceInfo, batch: TestBatch) {
        devices[device.serialNumber]?.run {
            safeSend(DeviceEvent.Execute(batch))
        }
    }

    private suspend fun requestStop() {
        queue.send(QueueMessage.Stop)
    }

    private fun terminate() {
        poolJob.cancel()
        close()
    }

    private suspend fun removeDevice(device: Device) {
        logger.debug { "remove device ${device.serialNumber}" }
        val actor = devices.remove(device.serialNumber)
        actor?.safeSend(DeviceEvent.Terminate)
        logger.debug { "devices.size = ${devices.size}" }
        if (noActiveDevices()) {
            if (!queue.stopRequested) return // we may receive new tests in the future

            noDevicesTimeoutDeferred?.cancel()

            logger.debug { "scheduling terminating of device pool actor as no devices found" }
            noDevicesTimeoutDeferred = async(poolJob) {
                delay(TimeUnit.MINUTES.toMillis(NO_DEVICES_IN_POOL_TIMEOUT_MINUTES))
                logger.debug { "terminating device pool actor as no devices found after timeout" }
                terminate()
            }
        }
    }

    private fun noActiveDevices() = devices.isEmpty() || devices.all { it.value.isClosedForSend }

    private suspend fun addDevice(device: Device) {
        if (devices.containsKey(device.serialNumber)) {
            logger.warn { "device ${device.serialNumber} already present in pool ${poolId.name}" }
            return
        }

        logger.debug { "add device ${device.serialNumber}" }

        noDevicesTimeoutDeferred?.cancel()

        val actor = DeviceActor(poolId, this, configuration, device, progressReporter, track, poolJob, coroutineContext)
        devices[device.serialNumber] = actor
        actor.safeSend(DeviceEvent.Initialize)
    }

    private suspend fun addTests(shard: TestShard) {
        queue.send(QueueMessage.AddShard(shard))
    }

    private companion object {
        private const val NO_DEVICES_IN_POOL_TIMEOUT_MINUTES = 5L
    }
}
