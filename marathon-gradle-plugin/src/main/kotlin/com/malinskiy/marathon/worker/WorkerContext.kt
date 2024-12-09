package com.malinskiy.marathon.worker

import com.malinskiy.marathon.Marathon
import com.malinskiy.marathon.di.marathonStartKoin
import com.malinskiy.marathon.execution.ComponentInfo
import com.malinskiy.marathon.execution.Configuration
import kotlinx.coroutines.channels.Channel
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

internal class WorkerContext(configuration: Configuration) : WorkerHandler {
    private val executor = Executors.newSingleThreadExecutor()
    private val componentsChannel: Channel<ComponentInfo> = Channel(capacity = Channel.UNLIMITED)

    private val application = marathonStartKoin(configuration)
    private val marathon = application.koin.get<Marathon>()
    private val isRunning = AtomicBoolean(false)
    private val startedLatch = CountDownLatch(1)

    private lateinit var finishFuture: Future<*>

    override fun scheduleTests(componentInfo: ComponentInfo) {
        ensureStarted()
        componentsChannel.trySend(componentInfo)
    }

    override fun await() {
        if (!isRunning.getAndSet(false)) return

        startedLatch.await(WAITING_FOR_START_TIMEOUT_MINUTES, TimeUnit.MINUTES)
        componentsChannel.close()

        try {
            // Use future to propagate all exceptions from runnable
            finishFuture.get()
        } finally {
            executor.shutdown()
        }
    }

    override fun close() {
        isRunning.set(false)
        componentsChannel.close()
        executor.shutdown()
        marathon.close()
        application.close()
    }

    private fun ensureStarted() {
        if (isRunning.getAndSet(true)) return

        val runnable = WorkerRunnable(marathon, componentsChannel)
        finishFuture = executor.submit(runnable)

        startedLatch.countDown()
    }

    private companion object {
        private const val WAITING_FOR_START_TIMEOUT_MINUTES = 1L
    }
}
