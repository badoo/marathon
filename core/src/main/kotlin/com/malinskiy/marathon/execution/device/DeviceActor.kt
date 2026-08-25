package com.malinskiy.marathon.execution.device

import com.malinskiy.marathon.actor.Actor
import com.malinskiy.marathon.actor.StateMachine
import com.malinskiy.marathon.analytics.internal.pub.Tracker
import com.malinskiy.marathon.device.Device
import com.malinskiy.marathon.device.DevicePoolId
import com.malinskiy.marathon.exceptions.DeviceLostException
import com.malinskiy.marathon.exceptions.TestBatchExecutionException
import com.malinskiy.marathon.execution.Configuration
import com.malinskiy.marathon.execution.DevicePoolMessage
import com.malinskiy.marathon.execution.DevicePoolMessage.FromDevice.IsReady
import com.malinskiy.marathon.execution.TestBatchResults
import com.malinskiy.marathon.execution.progress.ProgressReporter
import com.malinskiy.marathon.execution.withRetry
import com.malinskiy.marathon.log.MarathonLogging
import com.malinskiy.marathon.test.TestBatch
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CompletionHandler
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import kotlin.coroutines.CoroutineContext

class DeviceActor(
    private val devicePoolId: DevicePoolId,
    private val pool: SendChannel<DevicePoolMessage>,
    private val configuration: Configuration,
    val device: Device,
    private val progressReporter: ProgressReporter,
    private val tracker: Tracker,
    parent: Job,
    context: CoroutineContext
) : Actor<DeviceEvent>(name = "DeviceActor[${device.serialNumber}]", context, parent) {

    private val logger = MarathonLogging.getLogger("DevicePool[$devicePoolId]_DeviceActor[${device.serialNumber}]")
    private val state = StateMachine.create<DeviceState, DeviceEvent, DeviceAction> {
        initialState(DeviceState.Connected)
        state<DeviceState.Connected> {
            on<DeviceEvent.Initialize> {
                transitionTo(DeviceState.Initializing, DeviceAction.Initialize)
            }
            on<DeviceEvent.Terminate> {
                transitionTo(DeviceState.Terminated, DeviceAction.Terminate())
            }
            on<DeviceEvent.WakeUp> {
                dontTransition()
            }
        }
        state<DeviceState.Initializing> {
            on<DeviceEvent.Complete> {
                transitionTo(DeviceState.Ready, DeviceAction.NotifyIsReady())
            }
            on<DeviceEvent.Terminate> {
                transitionTo(DeviceState.Terminated, DeviceAction.Terminate())
            }
            on<DeviceEvent.WakeUp> {
                dontTransition()
            }
        }
        state<DeviceState.Ready> {
            on<DeviceEvent.Execute> {
                val deferred = CompletableDeferred<TestBatchResults>()
                transitionTo(DeviceState.Running(it.batch, deferred), DeviceAction.ExecuteBatch(it.batch, deferred))
            }
            on<DeviceEvent.WakeUp> {
                transitionTo(DeviceState.Ready, DeviceAction.NotifyIsReady())
            }
            on<DeviceEvent.Terminate> {
                transitionTo(DeviceState.Terminated, DeviceAction.Terminate())
            }
        }
        state<DeviceState.Running> {
            on<DeviceEvent.Terminate> {
                transitionTo(DeviceState.Terminated, DeviceAction.Terminate(testBatch))
            }
            on<DeviceEvent.Complete> {
                transitionTo(DeviceState.Ready, DeviceAction.NotifyIsReady(this.result))
            }
            on<DeviceEvent.WakeUp> {
                dontTransition()
            }
        }
        state<DeviceState.Terminated> {
            on<DeviceEvent.Complete> {
                dontTransition()
            }
        }
        onTransition { transition ->
            val validTransition = transition as? StateMachine.Transition.Valid
            if (validTransition !is StateMachine.Transition.Valid) {
                if (transition.event !is DeviceEvent.WakeUp) {
                    logger.error("Invalid transition from {} event {}", transition.fromState, transition.event)
                }
                return@onTransition
            }
            when (val sideEffect = validTransition.sideEffect) {
                DeviceAction.Initialize -> {
                    initialize()
                }
                is DeviceAction.NotifyIsReady -> {
                    sideEffect.result?.let {
                        sendResults(it)
                    }
                    notifyIsReady()
                }
                is DeviceAction.ExecuteBatch -> {
                    executeBatch(sideEffect.batch, sideEffect.result)
                }
                is DeviceAction.Terminate -> {
                    val batch = sideEffect.batch
                    if (batch == null) {
                        terminate()
                    } else {
                        returnBatchAnd(batch, "Device ${device.serialNumber} terminated") {
                            terminate()
                        }
                    }
                }
                else -> Unit
            }
        }
    }

    val isAvailable: Boolean
        get() = !isClosedForSend && state.state == DeviceState.Ready

    override suspend fun receive(msg: DeviceEvent) {
        when (msg) {
            is DeviceEvent.GetDeviceState -> {
                msg.deferred.complete(state.state)
            }
            else -> {
                state.transition(msg)
            }
        }
    }

    private fun sendResults(result: CompletableDeferred<TestBatchResults>) {
        scope.launch {
            val testResults = result.await()
            pool.send(DevicePoolMessage.FromDevice.CompletedTestBatch(device, testResults))
        }
    }

    private fun notifyIsReady() {
        scope.launch {
            pool.send(IsReady(device))
        }
    }

    private var job: Job? = null

    private fun initialize() {
        logger.debug("[{}] Initializing", device.serialNumber)
        job = scope.launch {
            try {
                withRetry(maxAttempts = 30, retryDelay = Duration.ofSeconds(10)) {
                    try {
                        device.prepare(configuration)
                    } catch (e: Exception) {
                        logger.debug("[{}] Initialization failed. Retrying", device.serialNumber, e)
                        throw e
                    }
                }
                state.transition(DeviceEvent.Complete)
            } catch (e: Exception) {
                currentCoroutineContext().ensureActive()
                logger.error("[{}] Initialization failed", device.serialNumber, e)
                state.transition(DeviceEvent.Terminate)
            }
        }.apply {
            invokeOnCompletion { cause ->
                if (cause != null && cause !is CancellationException) {
                    logger.error("[{}] Unrecoverable error during initialization. Terminating device", device.serialNumber, cause)
                    close()
                }
            }
        }
    }

    private fun executeBatch(batch: TestBatch, result: CompletableDeferred<TestBatchResults>) {
        logger.debug("[{}] Executing batch", device.serialNumber)
        var batchReturned = false
        job = scope.async {
            val start = Instant.now()
            try {
                device.execute(configuration, devicePoolId, batch, result, progressReporter)
                state.transition(DeviceEvent.Complete)
            } catch (e: CancellationException) {
                logger.warn("[{}] Device execution has been cancelled", device.serialNumber, e)
                state.transition(DeviceEvent.Terminate)
            } catch (e: DeviceLostException) {
                logger.error("[{}] Critical error during execution", device.serialNumber, e)
                state.transition(DeviceEvent.Terminate)
            } catch (e: TestBatchExecutionException) {
                logger.warn("[{}] Test batch failed execution", device.serialNumber, e)
                pool.send(
                    DevicePoolMessage.FromDevice.ReturnTestBatch(
                        device,
                        batch,
                        "Test batch failed execution:\n${e.stackTraceToString()}"
                    )
                )
                batchReturned = true
                state.transition(DeviceEvent.Complete)
            } catch (e: InterruptedException) {
                logger.warn("[{}] Device execution has been interrupted", device.serialNumber, e)
                state.transition(DeviceEvent.Terminate)
            } catch (e: Exception) {
                currentCoroutineContext().ensureActive()
                logger.error("[{}] Unknown vendor exception caught. Considering this a recoverable error", device.serialNumber, e)
                pool.send(
                    DevicePoolMessage.FromDevice.ReturnTestBatch(
                        device, batch, "Unknown vendor exception caught. \n" +
                            "${e.stackTraceToString()}"
                    )
                )
                batchReturned = true
                state.transition(DeviceEvent.Complete)
            } finally {
                val finish = Instant.now()
                tracker.executingBatch(device.serialNumber, start, finish)
            }
        }.apply {
            invokeOnCompletion { cause ->
                if (cause != null && cause !is CancellationException) {
                    logger.error("[{}] Unrecoverable error during batch execution. Terminating device", device.serialNumber, cause)
                    if (!batchReturned) {
                        pool.trySend(
                            DevicePoolMessage.FromDevice.ReturnTestBatch(device, batch, "Unrecoverable error:\n${cause.stackTraceToString()}")
                        )
                    }
                    close()
                }
            }
        }
    }

    private fun returnBatchAnd(batch: TestBatch, reason: String, completionHandler: CompletionHandler = {}): Job {
        return scope.launch {
            pool.send(DevicePoolMessage.FromDevice.ReturnTestBatch(device, batch, reason))
        }.apply {
            invokeOnCompletion(completionHandler)
        }
    }

    private fun terminate() {
        logger.debug("[{}] Terminating", device.serialNumber)
        job?.cancel()
        close()
    }
}
