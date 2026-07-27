package com.malinskiy.marathon.worker

import com.malinskiy.marathon.MarathonFactory
import com.malinskiy.marathon.actor.consumeConcurrently
import com.malinskiy.marathon.actor.unboundedChannel
import com.malinskiy.marathon.di.DefaultMarathonFactory
import com.malinskiy.marathon.execution.ComponentInfo
import com.malinskiy.marathon.execution.Configuration
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.gradle.api.tasks.testing.TestExecutionException

internal class WorkerContext(
    private val configuration: Configuration,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    marathonFactory: MarathonFactory = DefaultMarathonFactory(ioDispatcher = ioDispatcher)
) : WorkerHandler {

    private val marathon = marathonFactory.createMarathon(configuration)
    private val coroutineScope = CoroutineScope(ioDispatcher.limitedParallelism(1, "WorkerContext"))
    private val componentsChannel = unboundedChannel<ComponentInfo>()
    private val runResult = coroutineScope.async(CoroutineName("marathon-run")) { runMarathon() }

    override fun scheduleTests(componentInfo: ComponentInfo) {
        componentsChannel.trySend(componentInfo).getOrThrow()
    }

    override fun await() {
        componentsChannel.close()

        val success = runBlocking { runResult.await() }
        if (!success && !configuration.ignoreFailures) {
            throw TestExecutionException("Tests failed! See ${configuration.outputDir}/html/index.html")
        }
    }

    override fun close() {
        componentsChannel.close()
        coroutineScope.cancel()
        marathon.close()
    }

    private suspend fun runMarathon(): Boolean {
        marathon.start()

        componentsChannel.consumeConcurrently(SCHEDULING_CONCURRENCY) { component ->
            marathon.scheduleTests(component)
        }

        return marathon.stopAndWaitForCompletion()
    }

    private companion object {
        private val SCHEDULING_CONCURRENCY = maxOf(Runtime.getRuntime().availableProcessors() / 2, 1)
    }
}
