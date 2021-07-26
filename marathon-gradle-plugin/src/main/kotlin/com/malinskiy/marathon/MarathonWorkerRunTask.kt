package com.malinskiy.marathon

import com.malinskiy.marathon.worker.MarathonWorker
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

abstract class MarathonWorkerRunTask : DefaultTask() {
    @TaskAction
    fun run() {
        MarathonWorker.await()
    }
}
