package com.malinskiy.marathon

import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.services.ServiceReference
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault

@DisableCachingByDefault
abstract class MarathonWorkerRunTask : DefaultTask() {
    @get:ServiceReference(MarathonBuildService.NAME)
    abstract val buildService: Property<MarathonBuildService>

    @TaskAction
    fun run() {
        buildService.get().await()
    }
}
