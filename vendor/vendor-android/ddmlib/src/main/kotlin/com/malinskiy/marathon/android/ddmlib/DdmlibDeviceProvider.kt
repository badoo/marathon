package com.malinskiy.marathon.android.ddmlib

import com.android.ddmlib.AndroidDebugBridge
import com.android.ddmlib.DdmPreferences
import com.android.ddmlib.IDevice
import com.android.ddmlib.TimeoutException
import com.malinskiy.marathon.actor.unboundedChannel
import com.malinskiy.marathon.analytics.internal.pub.Track
import com.malinskiy.marathon.android.AndroidAppInstaller
import com.malinskiy.marathon.android.AndroidConfiguration
import com.malinskiy.marathon.android.executor.logcat.LogcatListener
import com.malinskiy.marathon.device.DeviceProvider
import com.malinskiy.marathon.device.DeviceProvider.DeviceEvent.DeviceConnected
import com.malinskiy.marathon.device.DeviceProvider.DeviceEvent.DeviceDisconnected
import com.malinskiy.marathon.exceptions.NoDevicesException
import com.malinskiy.marathon.execution.Configuration
import com.malinskiy.marathon.execution.StrictRunChecker
import com.malinskiy.marathon.io.AttachmentManager
import com.malinskiy.marathon.io.FileManager
import com.malinskiy.marathon.log.MarathonLogging
import com.malinskiy.marathon.time.Timer
import kotlinx.coroutines.CompletableJob
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class DdmlibDeviceProvider(
    private val track: Track,
    private val timer: Timer,
    private val config: Configuration,
    private val androidAppInstaller: AndroidAppInstaller,
    private val fileManager: FileManager,
    private val strictRunChecker: StrictRunChecker,
    private val logcatListener: LogcatListener,
    private val attachmentManager: AttachmentManager
) : DeviceProvider, AndroidDebugBridge.IDeviceChangeListener {
    private val logger = MarathonLogging.logger("AndroidDeviceProvider")

    private val channel: Channel<DeviceProvider.DeviceEvent> = unboundedChannel()
    private val devices: ConcurrentMap<String, DdmlibAndroidDevice> = ConcurrentHashMap()

    private val dispatcher = Dispatchers.IO.limitedParallelism(4)
    private val job = SupervisorJob()
    private val coroutineScope = CoroutineScope(job + dispatcher)

    override val deviceInitializationTimeoutMillis: Long = 180_000

    override suspend fun initialize() {
        DdmPreferences.setTimeOut(DEFAULT_DDM_LIB_TIMEOUT)
        @Suppress("DEPRECATION")
        AndroidDebugBridge.initIfNeeded(false)
        AndroidDebugBridge.addDeviceChangeListener(this)

        val adb = AndroidDebugBridge.createBridge(vendorConfiguration.adbPath.absolutePath, false, ADB_INIT_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS)
        logger.debug { "Created ADB bridge" }

        var getDevicesCountdown = config.noDevicesTimeoutMillis
        val sleepTime = DEFAULT_DDM_LIB_SLEEP_TIME
        while (!adb.hasInitialDeviceList() || !adb.hasDevices() && getDevicesCountdown >= 0) {
            logger.debug { "No devices, waiting..." }

            try {
                delay(sleepTime)
            } catch (e: InterruptedException) {
                throw TimeoutException("Timeout getting device list", e)
            }
            getDevicesCountdown -= sleepTime
        }

        logger.debug { "Finished waiting for device list" }

        adb.devices.forEach {
            logger.debug { "Notifying inital connected list: $it" }
            deviceConnected(it)
        }

        logger.debug { "Finished notifying" }

        if (!adb.hasInitialDeviceList() || printStackTraceAfterTimeout(PRINT_LOG_TIMEOUT) { !adb.hasDevices() }) {
            logger.debug { "Throwing no devices exception in DdmlibDeviceProvider" }
            throw NoDevicesException("No devices found.")
        }

        logger.debug { "Finished DdmlibDeviceProvider initialization" }
    }

    private fun <T> printStackTraceAfterTimeout(timeoutMillis: Long, block: () -> T): T {
        val currentThread = Thread.currentThread()
        val isBlockFinished = AtomicBoolean(false)

        Thread {
            Thread.sleep(timeoutMillis)
            if (!isBlockFinished.get() && currentThread.isAlive) {
                logger.debug { "Task is not finished within timeout. Printing thread stacktrace:" }
                currentThread
                    .stackTrace
                    .forEach { logger.debug { it } }
            }
        }.start()

        val result = block()

        isBlockFinished.set(true)

        return result
    }

    private fun getDeviceOrPut(androidDevice: DdmlibAndroidDevice): DdmlibAndroidDevice {
        val newAndroidDevice = devices.getOrPut(androidDevice.serialNumber) {
            androidDevice
        }

        if (newAndroidDevice != androidDevice) {
            logger.debug { "There was a device with the same serial number as the new device ($newAndroidDevice), closing the old device" }
            androidDevice.close()
            logger.debug { "Old device closed ($androidDevice)" }
        }

        return newAndroidDevice
    }

    private fun matchDdmsToDevice(device: IDevice): DdmlibAndroidDevice? {
        val observedDevices = devices.values
        return observedDevices.findLast {
            device == it.ddmsDevice ||
                device.serialNumber == it.ddmsDevice.serialNumber
        }
    }

    private fun AndroidDebugBridge.hasDevices(): Boolean = devices.isNotEmpty()

    override suspend fun terminate() {
        job.completeRecursively()
        job.join()
        channel.close()
    }

    override fun close() {
        AndroidDebugBridge.removeDeviceChangeListener(this)
        channel.close()
        coroutineScope.cancel()
        AndroidDebugBridge.terminate()
    }

    override fun subscribe() = channel

    override fun deviceChanged(device: IDevice, changeMask: Int) {
        logger.debug { "Device changed: $device" }

        coroutineScope.launch {
            val maybeNewAndroidDevice = DdmlibAndroidDevice(
                ddmsDevice = device,
                adbPath = vendorConfiguration.adbPath,
                track = track,
                timer = timer,
                androidAppInstaller = androidAppInstaller,
                attachmentManager = attachmentManager,
                reportsFileManager = fileManager,
                serialStrategy = vendorConfiguration.serialStrategy,
                logcatListener = logcatListener,
                strictRunChecker = strictRunChecker,
                parentJob = job
            )
            val healthy = maybeNewAndroidDevice.healthy

            logger.debug { "Device ${device.serialNumber} changed state. Healthy = $healthy" }
            if (healthy) {
                verifyBooted(maybeNewAndroidDevice)
                val androidDevice = getDeviceOrPut(maybeNewAndroidDevice)
                notifyConnected(androidDevice)
            } else {
                // This shouldn't have any side effects even if device was previously removed
                logger.debug { "Device is not healthy, notifying disconnected $device" }
                notifyDisconnected(maybeNewAndroidDevice)
                maybeNewAndroidDevice.close()
            }
        }
    }

    override fun deviceConnected(device: IDevice) {
        logger.debug { "Device connected: $device" }

        coroutineScope.launch {
            val maybeNewAndroidDevice = DdmlibAndroidDevice(
                ddmsDevice = device,
                track = track,
                timer = timer,
                serialStrategy = vendorConfiguration.serialStrategy,
                androidAppInstaller = androidAppInstaller,
                attachmentManager = attachmentManager,
                reportsFileManager = fileManager,
                adbPath = vendorConfiguration.adbPath,
                logcatListener = logcatListener,
                strictRunChecker = strictRunChecker,
                parentJob = job
            )

            val healthy = maybeNewAndroidDevice.healthy
            logger.debug("Device ${maybeNewAndroidDevice.serialNumber} connected. Healthy = $healthy")

            if (healthy) {
                verifyBooted(maybeNewAndroidDevice)
                val androidDevice = getDeviceOrPut(maybeNewAndroidDevice)
                notifyConnected(androidDevice)
            }
        }
    }

    override fun deviceDisconnected(device: IDevice) {
        logger.debug { "Device ${device.serialNumber} disconnected" }
        coroutineScope.launch {
            matchDdmsToDevice(device)?.let {
                notifyDisconnected(it)
                it.close()
                devices.remove(it.serialNumber)
            }
        }
    }

    private suspend fun verifyBooted(device: DdmlibAndroidDevice) {
        if (!waitForBoot(device)) throw TimeoutException("Timeout waiting for device ${device.serialNumber} to boot")
    }

    private suspend fun waitForBoot(device: DdmlibAndroidDevice): Boolean {
        var booted = false

        track.trackProviderDevicePreparing(device) {
            @Suppress("UnusedPrivateProperty")
            for (i in 1..30) {
                if (device.booted) {
                    logger.debug { "Device ${device.serialNumber} booted!" }
                    booted = true
                    break
                } else {
                    delay(1000)
                    logger.debug { "Device ${device.serialNumber} is still booting..." }
                }

                if (Thread.interrupted() || !currentCoroutineContext().isActive) {
                    booted = true
                    break
                }
            }
        }

        return booted
    }

    private suspend fun notifyConnected(device: DdmlibAndroidDevice) {
        logger.debug { "Notify device connected $device" }
        logger.debug { "Send DeviceConnected message for $device" }
        channel.send(DeviceConnected(device))
    }

    private suspend fun notifyDisconnected(device: DdmlibAndroidDevice) {
        androidAppInstaller.onDisconnected(device)
        channel.send(DeviceDisconnected(device))
        logcatListener.onDeviceDisconnected(device)
    }

    private val vendorConfiguration: AndroidConfiguration
        get() = config.vendorConfiguration as AndroidConfiguration

    private fun CompletableJob.completeRecursively(): Boolean {
        job.children
            .filterIsInstance<CompletableJob>()
            .forEach { it.complete() }
        return complete()
    }

    companion object {
        private val ADB_INIT_TIMEOUT = Duration.ofSeconds(60)
        private const val DEFAULT_DDM_LIB_TIMEOUT = 30000
        private const val DEFAULT_DDM_LIB_SLEEP_TIME = 500L
        private const val PRINT_LOG_TIMEOUT = 20000L
    }
}
