package com.malinskiy.marathon.android

import com.malinskiy.marathon.test.Test
import java.io.File

class RemoteFileManager(private val device: AndroidDevice) {

    fun pullFile(remoteFilePath: String, localFile: File) {
        device.pullFile(remoteFilePath, localFile.absolutePath)
    }

    fun remove(remotePath: String) {
        device.executeCommand(
            command = "rm -r $remotePath",
            errorMessage = "Failed to delete $remotePath"
        )
    }

    fun remoteVideoForTest(test: Test): String {
        val fileName = "${test.pkg}.${test.clazz}-${test.method}.mp4"
        return "${device.getExternalStorageMount()}/$fileName"
    }
}
