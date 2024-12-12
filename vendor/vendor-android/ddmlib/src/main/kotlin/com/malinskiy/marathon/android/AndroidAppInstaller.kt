package com.malinskiy.marathon.android

import com.android.ddmlib.InstallException
import com.malinskiy.marathon.analytics.internal.pub.Track
import com.malinskiy.marathon.execution.Configuration
import com.malinskiy.marathon.execution.withRetry
import com.malinskiy.marathon.io.FileHasher
import com.malinskiy.marathon.log.MarathonLogging
import java.io.File
import java.time.Instant
import kotlin.system.measureTimeMillis

class AndroidAppInstaller(
    private val fileHasher: FileHasher,
    private val track: Track,
    configuration: Configuration
) {

    private val logger = MarathonLogging.getLogger(AndroidAppInstaller::class.java)
    private val androidConfiguration = configuration.vendorConfiguration as AndroidConfiguration
    private val installedApps: MutableMap<String, MutableMap<String, String>> = hashMapOf()

    suspend fun ensureInstalled(device: AndroidDevice, componentInfo: AndroidComponentInfo) {
        val applicationInfo = ApkParser().parseInstrumentationInfo(componentInfo.testApplicationOutput)
        val installationTimeMillis = measureTimeMillis {
            componentInfo.applicationOutput?.let {
                logger.debug("[{}] Installing application package {}", device.serialNumber, applicationInfo.applicationPackage)
                ensureInstalled(device, applicationInfo.applicationPackage, it)
            }
            logger.debug("[{}] Installing instrumentation package {}", device.serialNumber, applicationInfo.instrumentationPackage)
            ensureInstalled(device, applicationInfo.instrumentationPackage, componentInfo.testApplicationOutput)
        }
        logger.debug("[{}] Installation finished in {}ms", device.serialNumber, installationTimeMillis)
    }

    fun onDisconnected(device: AndroidDevice) {
        installedApps.remove(device.serialNumber)
    }

    @Suppress("TooGenericExceptionThrown")
    private suspend fun ensureInstalled(device: AndroidDevice, appPackage: String, appApk: File) {
        withRetry(attempts = MAX_RETIRES, delayTime = 1000) {
            try {
                val checkStarted = Instant.now()
                val fileHash = fileHasher.getHash(appApk)
                val isApkInstalled = isApkInstalled(device, appPackage, fileHash)
                track.installationCheck(device.serialNumber, checkStarted, Instant.now())

                if (isApkInstalled) {
                    logger.info("[{}] Skipping installation of {} - APK is already installed", device.serialNumber, appPackage)
                } else {
                    cleanupSpaceBeforeInstallation(device)
                    logger.info("[{}] Installing {} from {}", device.serialNumber, appPackage, appApk.absolutePath)
                    val installationStarted = Instant.now()
                    val installMessage = device.safeInstallPackage(appApk.absolutePath, true, optionalParams(device))
                    installMessage?.let { logger.info(it) }
                    track.installation(device.serialNumber, installationStarted, Instant.now())
                    installedApps
                        .getOrPut(device.serialNumber) { hashMapOf() }
                        .put(appPackage, fileHash)
                }
            } catch (e: InstallException) {
                logger.error("[{}] Error while installing {} from {}", device.serialNumber, appPackage, appApk.absolutePath, e)
                throw RuntimeException("Error while installing $appPackage on ${device.serialNumber}", e)
            }
        }
    }

    @Suppress("NestedBlockDepth")
    private fun cleanupSpaceBeforeInstallation(device: AndroidDevice) {
        val storageUsedPercentage = device
            .safeExecuteShellCommand("df /storage/emulated -h | grep '/storage/emulated' | awk '{print \$5}'")
            .substringBefore("%")
            .toInt()
        logger.info("[{}] Used {}% of storage", device.serialNumber, storageUsedPercentage)
        val usedStorageThresholdInPercents = androidConfiguration.usedStorageThresholdInPercents
        if (storageUsedPercentage > usedStorageThresholdInPercents) {
            logger.warn("[{}] Used more than {}% of storage", device.serialNumber, usedStorageThresholdInPercents)
            val appsToClean = device.safeExecuteShellCommand(INSTALLED_TEST_APPS_SCRIPT).lines().filter { it.isNotEmpty() }
            logger.info("[{}] Uninstalling {} apps", device.serialNumber, appsToClean.size)
            appsToClean.forEach {
                try {
                    val error = device.safeUninstallPackage(it)
                    if (error != null) {
                        logger.error("[{}] Error while uninstalling {} : {}", device.serialNumber, it, error)
                    } else {
                        logger.info("[{}] Uninstalled {}", device.serialNumber, it)
                        installedApps[device.serialNumber]?.remove(it)
                    }
                } catch (@Suppress("TooGenericExceptionCaught") e: Throwable) {
                    logger.error("[{}] Error while uninstalling {}", device.serialNumber, it, e)
                }
            }
        }
    }

    private fun isApkInstalled(device: AndroidDevice, appPackage: String, fileHash: String): Boolean {
        if (installedApps[device.serialNumber]?.get(appPackage) == fileHash) {
            return true
        }

        val hashOnDevice = getHashOnDevice(device, appPackage) ?: return false
        return hashOnDevice == fileHash
    }

    private fun getHashOnDevice(device: AndroidDevice, appPackage: String): String? {
        val apkPaths = device
            .safeExecuteShellCommand("pm path $appPackage")
            .lines()
            .map { it.removePrefix(PACKAGE_PREFIX) }
            .filter { it.isNotBlank() }

        if (apkPaths.isEmpty()) return null
        if (apkPaths.size > 1) {
            logger.warn("[{}] Multiple packages of {} installed, skipping hash check", device.serialNumber, appPackage)
            return null
        }

        val apkPath = apkPaths.first()
        val md5Output = device.safeExecuteShellCommand("md5sum \"$apkPath\"")

        val hash = md5Output.substringBefore(" ")
        if (hash.length != MD5_HASH_SIZE) {
            logger.warn("[{}] Error while calculating hash for {}: {}, skipping hash check", device.serialNumber, appPackage, md5Output)
            return null
        }

        return hash
    }

    private fun optionalParams(device: AndroidDevice): String {
        val options = mutableListOf("-r")
        if (device.apiLevel >= MARSHMALLOW_VERSION_CODE && androidConfiguration.autoGrantPermission) {
            options += "-g"
        }
        options += androidConfiguration.installOptions
        return options.joinToString(" ")
    }

    companion object {
        private const val MAX_RETIRES = 3
        private const val MARSHMALLOW_VERSION_CODE = 23
        private const val MD5_HASH_SIZE = 32
        private const val INSTALLED_TEST_APPS_SCRIPT = "pm list packages -3 | grep -E '\\.test\$' | tr -d '\\r' | cut -d ':' -f 2"
        private const val PACKAGE_PREFIX = "package:"
    }
}
