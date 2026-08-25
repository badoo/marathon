package com.malinskiy.marathon.execution.device

import com.malinskiy.marathon.analytics.internal.pub.Tracker
import com.malinskiy.marathon.device.Device
import com.malinskiy.marathon.device.DevicePoolId
import com.malinskiy.marathon.device.StubDevice
import com.malinskiy.marathon.exceptions.DeviceLostException
import com.malinskiy.marathon.exceptions.TestBatchExecutionException
import com.malinskiy.marathon.execution.DevicePoolMessage
import com.malinskiy.marathon.execution.progress.ProgressReporter
import com.malinskiy.marathon.execution.stubTestBatchResults
import com.malinskiy.marathon.test.factory.configuration
import com.malinskiy.marathon.test.stubTest
import com.malinskiy.marathon.test.stubTestBatch
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import kotlin.time.Duration.Companion.milliseconds

class DeviceActorTest {
    private val configuration = configuration()
    private val pool = Channel<DevicePoolMessage>(Channel.UNLIMITED)
    private val tracker = mock<Tracker>()
    private val progressReporter = mock<ProgressReporter>()
    private val parentJob = Job()
    private val poolId = DevicePoolId("test-pool")

    @AfterEach
    fun tearDown() {
        parentJob.cancel()
    }

    @Test
    fun `newly created actor is connected and not available`() = runTest {
        val actor = createActor(StubDevice())

        val state = currentState(actor)

        assertThat(state).isEqualTo(DeviceState.Connected)
        assertThat(actor.isAvailable).isFalse()
    }

    @Test
    fun `initialize prepares the device and notifies the pool`() = runTest {
        val device = StubDevice()
        val actor = createActor(device)

        actor.send(DeviceEvent.Initialize)
        val message = pool.receive()

        assertThat(message).isInstanceOf(DevicePoolMessage.FromDevice.IsReady::class.java)
        assertThat((message as DevicePoolMessage.FromDevice.IsReady).device).isSameAs(device)
        assertThat(device.prepareCount).isEqualTo(1)
        assertThat(actor.isAvailable).isTrue()
    }

    @Test
    fun `initialization failure closes the actor after exhausting retries`() = runTest {
        val device = StubDevice()
        device.prepareAction = { throw IllegalStateException("prepare failed") }
        val actor = createActor(device)

        actor.send(DeviceEvent.Initialize)
        awaitTermination(actor)
        val leftover = pool.tryReceive()

        assertThat(device.prepareCount).isEqualTo(30)
        assertThat(leftover.isSuccess).isFalse()
        assertThat(actor.isAvailable).isFalse()
    }

    @Test
    fun `executing a batch sends results and a ready notification to the pool`() = runTest {
        val device = StubDevice()
        val batch = stubTestBatch(stubTest())
        val results = stubTestBatchResults(batchId = batch.id, device = device, componentInfo = batch.componentInfo)
        device.executeAction = { it.complete(results) }
        val actor = createActor(device)
        initializeToReady(actor)

        actor.send(DeviceEvent.Execute(batch))
        val first = pool.receive()
        val second = pool.receive()

        assertThat(first).isInstanceOf(DevicePoolMessage.FromDevice.CompletedTestBatch::class.java)
        assertThat((first as DevicePoolMessage.FromDevice.CompletedTestBatch).results).isEqualTo(results)
        assertThat(second).isInstanceOf(DevicePoolMessage.FromDevice.IsReady::class.java)
        assertThat(actor.isAvailable).isTrue()
        verify(tracker).executingBatch(eq("serial-1"), any(), any())
    }

    @Test
    fun `batch execution failure returns the batch and keeps the device ready`() = runTest {
        val device = StubDevice()
        device.executeAction = { throw TestBatchExecutionException("simulated batch failure") }
        val batch = stubTestBatch(stubTest())
        val actor = createActor(device)
        initializeToReady(actor)

        actor.send(DeviceEvent.Execute(batch))
        val first = pool.receive()
        val second = pool.receive()

        val returned = first as DevicePoolMessage.FromDevice.ReturnTestBatch
        assertThat(returned.batch).isEqualTo(batch)
        assertThat(returned.reason).contains("Test batch failed execution")
        assertThat(second).isInstanceOf(DevicePoolMessage.FromDevice.IsReady::class.java)
        assertThat(actor.isAvailable).isTrue()
    }

    @Test
    fun `unknown execution error returns the batch and keeps the device ready`() = runTest {
        val device = StubDevice()
        device.executeAction = { throw IllegalStateException("unexpected vendor failure") }
        val batch = stubTestBatch(stubTest())
        val actor = createActor(device)
        initializeToReady(actor)

        actor.send(DeviceEvent.Execute(batch))
        val first = pool.receive()
        val second = pool.receive()

        val returned = first as DevicePoolMessage.FromDevice.ReturnTestBatch
        assertThat(returned.batch).isEqualTo(batch)
        assertThat(returned.reason).contains("Unknown vendor exception caught")
        assertThat(second).isInstanceOf(DevicePoolMessage.FromDevice.IsReady::class.java)
        assertThat(actor.isAvailable).isTrue()
    }

    @Test
    fun `device loss during execution returns the batch and closes the actor`() = runTest {
        val device = StubDevice()
        device.executeAction = { throw DeviceLostException(RuntimeException("connection dropped")) }
        val batch = stubTestBatch(stubTest())
        val actor = createActor(device)
        initializeToReady(actor)

        actor.send(DeviceEvent.Execute(batch))
        val message = pool.receive()
        awaitTermination(actor)

        val returned = message as DevicePoolMessage.FromDevice.ReturnTestBatch
        assertThat(returned.batch).isEqualTo(batch)
        assertThat(returned.reason).isEqualTo("Device serial-1 terminated")
        assertThat(actor.isAvailable).isFalse()
    }

    @Test
    fun `unrecoverable execution error returns the batch and closes the actor`() = runTest {
        val device = StubDevice()
        device.executeAction = { throw NotImplementedError("unrecoverable failure") }
        val batch = stubTestBatch(stubTest())
        val actor = createActor(device)
        initializeToReady(actor)

        actor.send(DeviceEvent.Execute(batch))
        val message = pool.receive()
        awaitTermination(actor)

        val returned = message as DevicePoolMessage.FromDevice.ReturnTestBatch
        assertThat(returned.batch).isEqualTo(batch)
        assertThat(returned.reason).contains("Unrecoverable error")
    }

    @Test
    fun `terminate while running returns the batch and closes the actor`() = runTest {
        val device = StubDevice()
        device.executeAction = { awaitCancellation() }
        val batch = stubTestBatch(stubTest())
        val actor = createActor(device)
        initializeToReady(actor)
        actor.send(DeviceEvent.Execute(batch))
        val runningState = currentState(actor)

        actor.send(DeviceEvent.Terminate)
        val message = pool.receive()
        awaitTermination(actor)

        assertThat(runningState).isInstanceOf(DeviceState.Running::class.java)
        assertThat((runningState as DeviceState.Running).testBatch).isEqualTo(batch)
        val returned = message as DevicePoolMessage.FromDevice.ReturnTestBatch
        assertThat(returned.batch).isEqualTo(batch)
        assertThat(returned.reason).isEqualTo("Device serial-1 terminated")
    }

    @Test
    fun `terminate when idle closes the actor without returning batches`() = runTest {
        val actor = createActor(StubDevice())
        initializeToReady(actor)

        actor.send(DeviceEvent.Terminate)
        awaitTermination(actor)
        val leftover = pool.tryReceive()

        assertThat(leftover.isSuccess).isFalse()
        assertThat(actor.isAvailable).isFalse()
    }

    @Test
    fun `wake up when ready notifies the pool again`() = runTest {
        val actor = createActor(StubDevice())
        initializeToReady(actor)

        actor.send(DeviceEvent.WakeUp)
        val message = pool.receive()

        assertThat(message).isInstanceOf(DevicePoolMessage.FromDevice.IsReady::class.java)
    }

    @Test
    fun `wake up before initialization keeps the actor connected`() = runTest {
        val actor = createActor(StubDevice())

        actor.send(DeviceEvent.WakeUp)
        val state = currentState(actor)
        val leftover = pool.tryReceive()

        assertThat(state).isEqualTo(DeviceState.Connected)
        assertThat(leftover.isSuccess).isFalse()
    }

    private fun TestScope.createActor(device: Device): DeviceActor = DeviceActor(
        devicePoolId = poolId,
        pool = pool,
        configuration = configuration,
        device = device,
        progressReporter = progressReporter,
        tracker = tracker,
        parent = parentJob,
        context = StandardTestDispatcher(testScheduler),
    )

    private suspend fun currentState(actor: DeviceActor): DeviceState {
        val deferred = CompletableDeferred<DeviceState>()
        actor.send(DeviceEvent.GetDeviceState(deferred))
        return deferred.await()
    }

    private suspend fun initializeToReady(actor: DeviceActor) {
        actor.send(DeviceEvent.Initialize)
        pool.receive()
    }

    private suspend fun awaitTermination(actor: DeviceActor) {
        while (!actor.isClosedForSend) {
            delay(10.milliseconds)
        }
    }
}
