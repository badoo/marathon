package com.malinskiy.marathon.android

import com.malinskiy.marathon.test.Test
import java.io.File
import java.io.IOException

class RemoteFileManager(private val device: AndroidDevice) {
    private val tempDir = "/data/local/tmp"

    fun pullFile(remoteFilePath: String, localFile: File) {
        device.pullFile(remoteFilePath, localFile.absolutePath)
    }

    fun pullFromFilesDir(applicationId: String, remoteDir: String, localDir: File) {
        val files = device.safeExecuteShellCommand(
            command = "run-as $applicationId find $remoteDir -type f"
        ).trimIndent().lines()

        if (files.isNotEmpty()) {
            val archiveFile = localDir.resolve("$applicationId.tar.gz")
            val remoteArchiveFile = "$tempDir/${archiveFile.name}"
            device.executeCommand(
                command = "touch $remoteArchiveFile",
                errorMessage = "Failed to create empty file $remoteArchiveFile"
            )
            device.executeCommand(
                command = "run-as $applicationId sh -c \"cd $remoteDir && tar -czf $remoteArchiveFile *\"",
                errorMessage = "Failed to archive files"
            )

            device.pullFile(remoteArchiveFile, archiveFile.absolutePath)
            device.executeCommand(
                command = "rm $remoteArchiveFile",
                errorMessage = "Failed to delete temporary file $remoteArchiveFile"
            )

            untar(archiveFile, localDir)
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

    fun remoteVideoForTest(test: Test): String {
        val fileName = "${test.pkg}.${test.clazz}-${test.method}.mp4"
        return "${device.getExternalStorageMount()}/$fileName"
    }

    fun getScreenshotsDir(applicationId: String): String =
        "${getFilesDir(applicationId)}/screenshots/default"

    private fun getFilesDir(applicationId: String): String =
        "/data/data/$applicationId/files"

    private fun untar(archive: File, destination: File) {
        val process = ProcessBuilder()
            .command("tar", "-xzf", archive.absolutePath)
            .directory(destination)
            .start()
        if (process.waitFor() != 0) {
            throw IOException("Failed to extract archive $archive to $destination")
        }
    }
}
