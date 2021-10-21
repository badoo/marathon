package com.malinskiy.marathon.android

import com.malinskiy.marathon.test.Test
import java.io.File
import java.util.UUID

class RemoteFileManager(private val device: AndroidDevice) {
    private val tempDir = "/data/local/tmp/marathon"

    fun pullFile(remoteFilePath: String, localFile: File) {
        device.pullFile(remoteFilePath, localFile.absolutePath)
    }

    fun pullFromFilesDir(applicationId: String, remoteFilePath: String, localDir: File, fileMatch: List<String>) {
        val findArgs = fileMatch.toFindArgs()

        val files = device.safeExecuteShellCommand(
            command = "run-as $applicationId find $remoteFilePath -type f \\($findArgs\\)"
        ).trimIndent().lines()

        if (files.isNotEmpty()) {
            val filesWithTempFiles = files.associateWith { "$tempDir/${UUID.randomUUID()}" }

            device.executeCommand(
                command = filesWithTempFiles.values.joinToString(";", prefix = "mkdir -p $tempDir;") { "touch $it" },
                errorMessage = "Failed to create temporary files in $tempDir"
            )

            val copyCommands = filesWithTempFiles.entries.joinToString(";") { "cp ${it.key} ${it.value}" }
            device.executeCommand(
                command = "run-as $applicationId sh -c \"$copyCommands\"",
                errorMessage = "Failed to copy files to $tempDir"
            )

            filesWithTempFiles.forEach { (filePath, tempFilePath) ->
                val fileName = filePath.substring(filePath.lastIndexOf('/') + 1)
                device.pullFile(tempFilePath, localDir.resolve(fileName).absolutePath)
            }

            device.executeCommand(
                command = filesWithTempFiles.values.joinToString(";") { "rm $it" },
                errorMessage = "Failed to delete temporary files in $tempDir"
            )
        }
    }

    fun remove(remotePath: String) {
        device.executeCommand(
            command = "rm -r $remotePath",
            errorMessage = "Failed to delete $remotePath"
        )
    }

    fun removeFromFilesDir(applicationId: String, remotePath: String) {
        device.executeCommand(
            command = "run-as $applicationId rm -r $remotePath",
            errorMessage = "Failed to delete $remotePath"
        )
    }

    fun prepareTempDirectory() {
        device.safeExecuteShellCommand("rm -r $tempDir;mkdir -p $tempDir")
    }

    fun remoteVideoForTest(test: Test): String {
        val fileName = "${test.pkg}.${test.clazz}-${test.method}.mp4"
        return "${device.getExternalStorageMount()}/$fileName"
    }

    fun getScreenshotsDir(applicationId: String): String =
        "${getFilesDir(applicationId)}/screenshots"

    private fun getFilesDir(applicationId: String): String =
        "/data/data/$applicationId/files"

    private fun List<String>.toFindArgs() =
        mapIndexed { index, s ->
            if (index != 0) {
                " -o "
            } else {
                ""
            } + " -name '$s' "
        }.joinToString(separator = "")
}
