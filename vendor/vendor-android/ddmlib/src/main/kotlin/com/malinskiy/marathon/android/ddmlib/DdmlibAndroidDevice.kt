package com.malinskiy.marathon.android.ddmlib

import com.android.ddmlib.AdbCommandRejectedException
import com.android.ddmlib.CollectingOutputReceiver
import com.android.ddmlib.IDevice
import com.android.ddmlib.IDevice.MNT_EXTERNAL_STORAGE
import com.android.ddmlib.InstallException
import com.android.ddmlib.NullOutputReceiver
import com.android.ddmlib.RawImage
import com.android.ddmlib.ShellCommandUnresponsiveException
import com.android.ddmlib.SyncException
import com.android.ddmlib.TimeoutException
import com.android.ddmlib.logcat.LogCatMessage
import com.android.sdklib.AndroidVersion
import com.malinskiy.marathon.analytics.internal.pub.Track
import com.malinskiy.marathon.android.AndroidAppInstaller
import com.malinskiy.marathon.android.AndroidComponentInfo
import com.malinskiy.marathon.android.AndroidDevice
import com.malinskiy.marathon.android.RemoteFileManager
import com.malinskiy.marathon.android.ddmlib.shell.CliLogcatReceiver
import com.malinskiy.marathon.android.ddmlib.shell.receiver.CollectingShellOutputReceiver
import com.malinskiy.marathon.android.ddmlib.shell.toMarathonLogcatMessage
import com.malinskiy.marathon.android.exception.CommandRejectedException
import com.malinskiy.marathon.android.exception.InvalidSerialConfiguration
import com.malinskiy.marathon.android.exception.TransferException
import com.malinskiy.marathon.android.executor.listeners.CompositeTestRunListener
import com.malinskiy.marathon.android.executor.listeners.DebugTestRunListener
import com.malinskiy.marathon.android.executor.listeners.NoOpTestRunListener
import com.malinskiy.marathon.android.executor.listeners.ProgressTestRunListener
import com.malinskiy.marathon.android.executor.listeners.TestRunListener
import com.malinskiy.marathon.android.executor.listeners.TestRunResultsListener
import com.malinskiy.marathon.android.executor.listeners.pull.PullScreenshotTestRunListener
import com.malinskiy.marathon.android.executor.listeners.screenshot.ScreenCapturerTestRunListener
import com.malinskiy.marathon.android.executor.listeners.video.ScreenRecorderHandler
import com.malinskiy.marathon.android.executor.listeners.video.ScreenRecorderOptions
import com.malinskiy.marathon.android.executor.listeners.video.ScreenRecorderTestRunListener
import com.malinskiy.marathon.android.executor.logcat.LogcatListener
import com.malinskiy.marathon.android.serial.SerialStrategy
import com.malinskiy.marathon.device.Device
import com.malinskiy.marathon.device.DeviceFeature
import com.malinskiy.marathon.device.DevicePoolId
import com.malinskiy.marathon.device.NetworkState
import com.malinskiy.marathon.device.OperatingSystem
import com.malinskiy.marathon.exceptions.DeviceLostException
import com.malinskiy.marathon.execution.Configuration
import com.malinskiy.marathon.execution.StrictRunChecker
import com.malinskiy.marathon.execution.TestBatchResults
import com.malinskiy.marathon.execution.measure
import com.malinskiy.marathon.execution.progress.ProgressReporter
import com.malinskiy.marathon.io.AttachmentManager
import com.malinskiy.marathon.io.FileManager
import com.malinskiy.marathon.log.MarathonLogging
import com.malinskiy.marathon.report.attachment.AttachmentProvider
import com.malinskiy.marathon.test.TestBatch
import com.malinskiy.marathon.time.Timer
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.newFixedThreadPoolContext
import kotlinx.coroutines.runBlocking
import java.awt.image.BufferedImage
import java.io.IOException
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.coroutines.CoroutineContext

class DdmlibAndroidDevice(
    val ddmsDevice: IDevice,
    private val adbPath: String,
    private val track: Track,
    private val timer: Timer,
    private val androidAppInstaller: AndroidAppInstaller,
    private val attachmentManager: AttachmentManager,
    private val reportsFileManager: FileManager,
    private val serialStrategy: SerialStrategy,
    private val logcatListener: LogcatListener,
    private val strictRunChecker: StrictRunChecker
) : Device, CoroutineScope, AndroidDevice {
    override val fileManager = RemoteFileManager(this)

    override val version: AndroidVersion by lazy { ddmsDevice.version }
    private val nullOutputReceiver = NullOutputReceiver()
    private val parentJob: Job = Job()

    private var logcatReceiver: CliLogcatReceiver? = null
    private val logMessagesListener: (List<LogCatMessage>) -> Unit = {
        it.forEach { msg ->
            logcatListener.onMessage(this, msg.toMarathonLogcatMessage())
        }
    }

    override fun pullFile(remoteFilePath: String, localFilePath: String) {
        try {
            ddmsDevice.pullFile(remoteFilePath, localFilePath)
        } catch (e: SyncException) {
            throw TransferException(e)
        }
    }

    override fun getExternalStorageMount(): String = ddmsDevice.getMountPoint(MNT_EXTERNAL_STORAGE)!!

    override fun executeCommand(command: String, errorMessage: String) {
        try {
            ddmsDevice.safeExecuteShellCommand(command, nullOutputReceiver)
        } catch (e: TimeoutException) {
            logger.error(errorMessage, e)
        } catch (e: AdbCommandRejectedException) {
            logger.error(errorMessage, e)
        } catch (e: ShellCommandUnresponsiveException) {
            logger.error(errorMessage, e)
        } catch (e: IOException) {
            logger.error(errorMessage, e)
        }
    }

    override fun getScreenshot(timeout: Long, units: TimeUnit): BufferedImage {
        return try {
            val rawImage = ddmsDevice.getScreenshot(timeout, units)
            bufferedImageFrom(rawImage)
        } catch (e: TimeoutException) {
            throw java.util.concurrent.TimeoutException(e.message)
        } catch (e: AdbCommandRejectedException) {
            throw CommandRejectedException(e)
        }
    }

    override fun safeStartScreenRecorder(
        handler: ScreenRecorderHandler,
        remoteFilePath: String,
        options: ScreenRecorderOptions
    ) {
        val recorderOptions = com.android.ddmlib.ScreenRecorderOptions.Builder()
            .setBitRate(options.bitrateMbps)
            .setShowTouches(options.showTouches)
            .setSize(options.width, options.height)
            .setTimeLimit(options.timeLimit, options.timeLimitUnits)
            .build()

        val receiver = CollectingOutputReceiver()
        handler.subscribeOnStop { receiver.cancel() }

        ddmsDevice.safeStartScreenRecorder(
            remoteFilePath,
            recorderOptions,
            receiver
        )
    }

    override fun waitForAsyncWork() {
        runBlocking(context = coroutineContext) {
            parentJob.children.forEach {
                it.join()
            }
        }
    }

    private fun bufferedImageFrom(rawImage: RawImage): BufferedImage {
        val image = BufferedImage(rawImage.width, rawImage.height, BufferedImage.TYPE_INT_ARGB)

        var index = 0
        val bytesPerPixel = rawImage.bpp shr 3
        for (y in 0 until rawImage.height) {
            for (x in 0 until rawImage.width) {
                image.setRGB(x, y, rawImage.getARGB(index) or -0x1000000)
                index += bytesPerPixel
            }
        }
        return image
    }

    private val dispatcher by lazy {
        newFixedThreadPoolContext(1, "AndroidDevice - execution - ${ddmsDevice.serialNumber}")
    }

    override val coroutineContext: CoroutineContext = dispatcher

    val logger = MarathonLogging.logger(DdmlibAndroidDevice::class.java.simpleName)

    override val abi: String by lazy {
        ddmsDevice.getProperty("ro.product.cpu.abi") ?: "Unknown"
    }

    override val model: String by lazy {
        ddmsDevice.getProperty("ro.product.model") ?: "Unknown"
    }

    override val manufacturer: String by lazy {
        ddmsDevice.getProperty("ro.product.manufacturer") ?: "Unknown"
    }

    override val deviceFeatures: Collection<DeviceFeature>
        get() {
            val videoSupport = ddmsDevice.supportsFeature(IDevice.Feature.SCREEN_RECORD) &&
                manufacturer != "Genymotion"
            val screenshotSupport = ddmsDevice.version.isGreaterOrEqualThan(AndroidVersion.VersionCodes.JELLY_BEAN)

            val features = mutableListOf<DeviceFeature>()

            if (videoSupport) features.add(DeviceFeature.VIDEO)
            if (screenshotSupport) features.add(DeviceFeature.SCREENSHOT)

            return features
        }
    override val apiLevel: Int
        get() = ddmsDevice.version.apiLevel

    override fun safeInstallPackage(absolutePath: String, reinstall: Boolean, optionalParams: String): String? {
        return try {
            ddmsDevice.safeInstallPackage(absolutePath, reinstall, optionalParams)
        } catch (e: InstallException) {
            throw com.malinskiy.marathon.android.exception.InstallException(e)
        }
    }

    /**
     * We can only call this after the device finished booting
     */
    private val realSerialNumber: String by lazy {
        val marathonSerialProp: String = ddmsDevice.getProperty("marathon.serialno") ?: ""
        val serialProp: String = ddmsDevice.getProperty("ro.boot.serialno") ?: ""
        val hostName: String = ddmsDevice.getProperty("net.hostname") ?: ""
        val serialNumber = ddmsDevice.serialNumber

        val result = when (serialStrategy) {
            SerialStrategy.AUTOMATIC -> {
                marathonSerialProp.takeIf { it.isNotEmpty() }
                    ?: serialProp.takeIf { it.isNotEmpty() }
                    ?: hostName.takeIf { it.isNotEmpty() }
                    ?: serialNumber.takeIf { it.isNotEmpty() }
                    ?: UUID.randomUUID().toString()
            }
            SerialStrategy.MARATHON_PROPERTY -> marathonSerialProp
            SerialStrategy.BOOT_PROPERTY -> serialProp
            SerialStrategy.HOSTNAME -> hostName
            SerialStrategy.DDMS -> serialNumber
        }

        result.apply {
            if (this == null) throw InvalidSerialConfiguration(serialStrategy)
        }
    }

    val booted: Boolean
        get() = ddmsDevice.getProperty("sys.boot_completed") != null

    override val serialNumber: String = when {
        booted -> realSerialNumber
        else -> ddmsDevice.serialNumber
    }

    override val operatingSystem: OperatingSystem by lazy {
        OperatingSystem(ddmsDevice.version.apiString)
    }

    override val networkState: NetworkState
        get() = when (ddmsDevice.isOnline) {
            true -> NetworkState.CONNECTED
            else -> NetworkState.DISCONNECTED
        }

    override val healthy: Boolean
        get() = when (ddmsDevice.state) {
            IDevice.DeviceState.ONLINE -> true
            else -> false
        }

    override suspend fun execute(
        configuration: Configuration,
        devicePoolId: DevicePoolId,
        testBatch: TestBatch,
        deferred: CompletableDeferred<TestBatchResults>,
        progressReporter: ProgressReporter
    ) {
        val androidComponentInfo = testBatch.componentInfo as AndroidComponentInfo

        try {
            async { ensureInstalled(androidComponentInfo) }.await()
        } catch (exception: Throwable) {
            logger.error { "Terminating device $serialNumber due to installation failures" }
            throw DeviceLostException(exception)
        }

        logger.measure("print_to_logcat") {
            safePrintToLogcat(SERVICE_LOGS_TAG, "\"batch_started: {${testBatch.id}}\"")
        }

        val deferredResult = async {
            val listeners = createListeners(configuration, devicePoolId, testBatch, deferred, progressReporter)
            val listener = DdmlibTestRunListener(testBatch.componentInfo, listeners)
            AndroidDeviceTestRunner(this@DdmlibAndroidDevice).execute(configuration, testBatch, listener)
        }
        deferredResult.await()

        safePrintToLogcat(SERVICE_LOGS_TAG, "\"batch_finished: {${testBatch.id}}\"")
    }

    private fun safePrintToLogcat(tag: String, message: String) {
        try {
            safeExecuteShellCommand("log -t $tag $message")
        } catch (exception: Throwable) {
            logger.error { "Error during printing logcat message $tag:$message to device $serialNumber" }
            exception.printStackTrace()
        }
    }

    private suspend fun ensureInstalled(componentInfo: AndroidComponentInfo) {
        androidAppInstaller.ensureInstalled(this, componentInfo)
    }

    private fun createListeners(
        configuration: Configuration,
        devicePoolId: DevicePoolId,
        testBatch: TestBatch,
        deferred: CompletableDeferred<TestBatchResults>,
        progressReporter: ProgressReporter
    ): CompositeTestRunListener {
        val fileManager = FileManager(configuration.outputDir)
        val attachmentProviders = mutableListOf<AttachmentProvider>()

        val features = this.deviceFeatures

        val preferableRecorderType = configuration.vendorConfiguration.preferableRecorderType()
        val recorderListener = selectRecorderType(preferableRecorderType, features)?.let { feature ->
            prepareRecorderListener(feature, fileManager, devicePoolId, attachmentProviders)
        } ?: NoOpTestRunListener()

        val pullScreenshotListener = if (configuration.isPullScreenshotEnabled()) {
            createPullScreenshotTestRunListener(devicePoolId, configuration, testBatch)
        } else {
            NoOpTestRunListener()
        }

        return CompositeTestRunListener(
            listOf(
                recorderListener,
                TestRunResultsListener(testBatch, this, deferred, timer, strictRunChecker, attachmentProviders),
                DebugTestRunListener(this),
                pullScreenshotListener,
                ProgressTestRunListener(this, devicePoolId, progressReporter)
            )
        )
    }

    override suspend fun prepare(configuration: Configuration) {
        track.trackDevicePreparing(this) {
            val deferred = async {
                fileManager.removeRemoteDirectory()
                fileManager.createRemoteDirectory()
                clearLogcat(ddmsDevice)

                logcatReceiver = CliLogcatReceiver(adbPath, reportsFileManager, ddmsDevice, logMessagesListener)
                logcatReceiver?.start()
            }
            deferred.await()
        }
    }

    override fun dispose() {
        logcatReceiver?.dispose()
        dispatcher.close()
    }

    private fun selectRecorderType(preferred: DeviceFeature?, features: Collection<DeviceFeature>) = when {
        features.contains(preferred) -> preferred
        features.contains(DeviceFeature.VIDEO) -> DeviceFeature.VIDEO
        features.contains(DeviceFeature.SCREENSHOT) -> DeviceFeature.SCREENSHOT
        else -> null
    }

    private fun Configuration.isPullScreenshotEnabled(): Boolean =
        pullScreenshotFilterConfiguration.whitelist.isNotEmpty()

    private fun createPullScreenshotTestRunListener(
        devicePoolId: DevicePoolId,
        configuration: Configuration,
        testBatch: TestBatch
    ): PullScreenshotTestRunListener =
        PullScreenshotTestRunListener(
            device = this,
            devicePoolId = devicePoolId,
            outputDir = configuration.outputDir,
            pullScreenshotFilterConfiguration = configuration.pullScreenshotFilterConfiguration,
            testBatch = testBatch,
            parentJob = parentJob
        )

    override fun safeUninstallPackage(appPackage: String): String? {
        return try {
            ddmsDevice.safeUninstallPackage(appPackage)
        } catch (e: InstallException) {
            throw com.malinskiy.marathon.android.exception.InstallException(e)
        }
    }

    override fun safeExecuteShellCommand(command: String): String {
        val receiver = CollectingShellOutputReceiver()
        ddmsDevice.safeExecuteShellCommand(command, receiver)
        return receiver.output()
    }

    private fun prepareRecorderListener(
        feature: DeviceFeature, fileManager: FileManager, devicePoolId: DevicePoolId,
        attachmentProviders: MutableList<AttachmentProvider>
    ): TestRunListener =
        when (feature) {
            DeviceFeature.VIDEO -> {
                ScreenRecorderTestRunListener(attachmentManager, this)
                    .also { attachmentProviders.add(it) }
            }

            DeviceFeature.SCREENSHOT -> {
                ScreenCapturerTestRunListener(attachmentManager, this)
                    .also { attachmentProviders.add(it) }
            }
        }

    private fun clearLogcat(device: IDevice) {
        val logger = MarathonLogging.logger("AndroidDevice.clearLogcat")
        try {
            device.safeExecuteShellCommand("logcat -c", NullOutputReceiver())
        } catch (e: Throwable) {
            logger.warn("Could not clear logcat on device: ${device.serialNumber}", e)
        }
    }

    override fun toString(): String {
        return "AndroidDevice(model=$model, serial=$serialNumber)"
    }

    private companion object {
        private const val SERVICE_LOGS_TAG = "marathon"
    }
}
