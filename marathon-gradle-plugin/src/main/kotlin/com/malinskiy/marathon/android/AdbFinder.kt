package com.malinskiy.marathon.android

import com.android.SdkConstants.FN_LOCAL_PROPERTIES
import java.io.File
import java.util.*

fun findAdbPath(rootDir: File): File {
    val localProperties = File(rootDir, FN_LOCAL_PROPERTIES)
    val properties = Properties()

    if (localProperties.isFile) {
        localProperties.bufferedReader().use {
            properties.load(it)
        }
    }

    return findSdkLocation(properties, rootDir)
        ?.resolve("platform-tools")
        ?.resolve("adb")
        ?: throw RuntimeException("SDK location not found. Define location with sdk.dir in the local.properties file or with an ANDROID_HOME environment variable.")
}

private fun findSdkLocation(properties: Properties, rootDir: File): File? {
    var sdkDirProp: String? = properties.getProperty("sdk.dir")
    if (sdkDirProp != null) {
        var sdk = File(sdkDirProp)
        if (!sdk.isAbsolute) {
            sdk = rootDir.resolve(sdkDirProp)
        }
        return sdk
    }

    sdkDirProp = properties.getProperty("android.dir")
    if (sdkDirProp != null) {
        return rootDir.resolve(sdkDirProp)
    }

    val envVar = System.getenv("ANDROID_HOME")
    if (envVar != null) {
        var sdk = File(envVar)
        if (!sdk.isAbsolute) {
            sdk = rootDir.resolve(envVar)
        }
        return sdk
    }

    val property = System.getProperty("android.home")
    return when {
        property != null -> File(property)
        else -> null
    }
}
