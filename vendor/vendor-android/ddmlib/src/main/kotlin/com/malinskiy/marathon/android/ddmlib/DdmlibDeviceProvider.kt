package com.malinskiy.marathon.android.ddmlib

import com.android.ddmlib.AndroidDebugBridge
import com.android.ddmlib.DdmPreferences
import com.android.ddmlib.IDevice
import com.malinskiy.marathon.actor.unboundedChannel
import com.malinskiy.marathon.analytics.internal.pub.Track
import com.malinskiy.marathon.android.AndroidAppInstaller
import com.malinskiy.marathon.android.AndroidConfiguration
import com.malinskiy.marathon.android.executor.logcat.LogcatListener
import com.malinskiy.marathon.device.DeviceEvent
import com.malinskiy.marathon.device.DeviceEvent.DeviceConnected
import com.malinskiy.marathon.device.DeviceEvent.DeviceDisconnected
import com.malinskiy.marathon.device.DeviceProvider
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
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.time.withTimeout
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.TimeUnit

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

    private val logger = MarathonLogging.getLogger(DdmlibDeviceProvider::class.java)
    private val channel: Channel<DeviceEvent> = unboundedChannel()
    private val devices: ConcurrentMap<String, DdmlibAndroidDevice> = ConcurrentHashMap()

    private val dispatcher = Dispatchers.IO.limitedParallelism(4)
    private val job = SupervisorJob()
    private val coroutineScope = CoroutineScope(job + dispatcher)

    override val deviceEvents: Flow<DeviceEvent>
        get() = channel.consumeAsFlow()

    override suspend fun initialize() {
        DdmPreferences.setTimeOut(DEFAULT_DDM_LIB_TIMEOUT)
        @Suppress("DEPRECATION")
        AndroidDebugBridge.initIfNeeded(false)
        AndroidDebugBridge.addDeviceChangeListener(this)

        logger.debug("Creating ADB bridge")

        val oldAdb = AndroidDebugBridge.getBridge()
        val adb = AndroidDebugBridge.createBridge(vendorConfiguration.adbPath.absolutePath, false, ADB_INIT_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS)
        val newAdbCreated = adb !== oldAdb
        if (!newAdbCreated) {
            logger.debug("Reusing existing ADB bridge")
        }

        adb.ensureInitialized()

        if (!newAdbCreated && adb.devices.isNotEmpty()) {
            logger.debug("Initial connected devices: {}", adb.devices.joinToString(", "))
            adb.devices.forEach {
                deviceConnected(it)
            }
        }
    }

    private fun getDeviceOrPut(androidDevice: DdmlibAndroidDevice): DdmlibAndroidDevice {
        val newAndroidDevice = devices.getOrPut(androidDevice.serialNumber) {
            androidDevice
        }

        if (newAndroidDevice != androidDevice) {
            logger.debug("[{}] Device already exists. Closing the old device", newAndroidDevice.serialNumber)
            androidDevice.close()
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

    override fun deviceChanged(device: IDevice, changeMask: Int) {
        logger.debug("Device {} changed, mask = {}", device, changeMask)

        coroutineScope.launch {
            val newAndroidDevice = device.toDdmlibAndroidDevice()
            val healthy = newAndroidDevice.healthy

            logger.debug("[{}] Changed state. Healthy = {}", newAndroidDevice.serialNumber, healthy)
            if (healthy) {
                verifyBooted(newAndroidDevice)
                val androidDevice = getDeviceOrPut(newAndroidDevice)
                notifyConnected(androidDevice)
            } else {
                // This shouldn't have any side effects even if device was previously removed
                logger.debug("[{}] Unhealthy. Disconnecting", newAndroidDevice.serialNumber)
                notifyDisconnected(newAndroidDevice)
                newAndroidDevice.close()
            }
        }
    }

    override fun deviceConnected(device: IDevice) {
        logger.debug("Device {} connected", device)

        coroutineScope.launch {
            val newAndroidDevice = device.toDdmlibAndroidDevice()
            val healthy = newAndroidDevice.healthy
            logger.debug("[{}] Connected. Healthy = {}", newAndroidDevice.serialNumber, healthy)

            if (healthy) {
                verifyBooted(newAndroidDevice)
                val androidDevice = getDeviceOrPut(newAndroidDevice)
                notifyConnected(androidDevice)
            }
        }
    }

    override fun deviceDisconnected(device: IDevice) {
        logger.debug("Device {} disconnected", device)
        coroutineScope.launch {
            matchDdmsToDevice(device)?.let {
                notifyDisconnected(it)
                it.close()
                devices.remove(it.serialNumber)
            }
        }
    }

    private suspend fun verifyBooted(device: DdmlibAndroidDevice) {
        track.trackProviderDevicePreparing(device) {
            device.waitForBoot()
        }
    }

    private suspend fun notifyConnected(device: DdmlibAndroidDevice) {
        channel.send(DeviceConnected(device))
    }

    private suspend fun notifyDisconnected(device: DdmlibAndroidDevice) {
        androidAppInstaller.onDisconnected(device)
        channel.send(DeviceDisconnected(device))
        logcatListener.onDeviceDisconnected(device)
    }

    private fun IDevice.toDdmlibAndroidDevice(): DdmlibAndroidDevice =
        DdmlibAndroidDevice(
            ddmsDevice = this,
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

    private suspend fun AndroidDebugBridge.ensureInitialized() {
        try {
            withTimeout(ADB_INIT_TIMEOUT) {
                while (isActive && !hasInitialDeviceList()) {
                    logger.debug("Waiting for ADB initialization...")
                    delay(500L)
                }
            }
        } catch (e: TimeoutCancellationException) {
            throw NoDevicesException(e)
        }
    }

    private val vendorConfiguration: AndroidConfiguration
        get() = config.vendorConfiguration as AndroidConfiguration

    private fun CompletableJob.completeRecursively(): Boolean {
        children
            .filterIsInstance<CompletableJob>()
            .forEach { it.complete() }
        return complete()
    }

    companion object {
        private val ADB_INIT_TIMEOUT = Duration.ofSeconds(60)
        private const val DEFAULT_DDM_LIB_TIMEOUT = 30000
    }
}
