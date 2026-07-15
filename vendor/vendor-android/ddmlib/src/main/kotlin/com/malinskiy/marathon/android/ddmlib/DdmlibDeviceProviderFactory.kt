package com.malinskiy.marathon.android.ddmlib

import com.malinskiy.marathon.android.AndroidAppInstaller
import com.malinskiy.marathon.android.DeviceProviderFactory
import com.malinskiy.marathon.android.executor.logcat.LogcatListener
import com.malinskiy.marathon.device.DeviceProvider
import com.malinskiy.marathon.vendor.VendorDependencies

class DdmlibDeviceProviderFactory : DeviceProviderFactory {
    override fun create(dependencies: VendorDependencies, logcatListener: LogcatListener): DeviceProvider =
        DdmlibDeviceProvider(
            track = dependencies.track,
            timer = dependencies.timer,
            config = dependencies.configuration,
            androidAppInstaller = AndroidAppInstaller(dependencies.fileHasher, dependencies.track, dependencies.configuration),
            fileManager = dependencies.fileManager,
            strictRunChecker = dependencies.strictRunChecker,
            logcatListener = logcatListener,
            attachmentManager = dependencies.attachmentManager
        )
}
