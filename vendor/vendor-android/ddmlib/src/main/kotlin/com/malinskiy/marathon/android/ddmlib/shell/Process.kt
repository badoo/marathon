package com.malinskiy.marathon.android.ddmlib.shell

import java.io.File

fun spawnProcess(
    command: Array<String>,
    workingDirectory: File? = null,
    outputTo: File? = null
): Process =
    ProcessBuilder(*command)
        .apply {
            if (workingDirectory != null) {
                directory(workingDirectory)
            }

            if (outputTo != null) {
                redirectOutput(outputTo)
            }
        }
        .start()
