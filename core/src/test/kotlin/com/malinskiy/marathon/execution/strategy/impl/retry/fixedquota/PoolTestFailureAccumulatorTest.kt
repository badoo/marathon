package com.malinskiy.marathon.execution.strategy.impl.retry.fixedquota

import com.malinskiy.marathon.device.DevicePoolId
import com.malinskiy.marathon.generateTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class PoolTestFailureAccumulatorTest {
    private val accumulator = PoolTestFailureAccumulator()
    private val devicePoolId = DevicePoolId("DevicePoolId")
    private val test = generateTest()

    @Test
    fun `default retry count value = 0`() {
        val count = accumulator.getCount(devicePoolId, test)

        assertThat(count).isEqualTo(0)
    }

    @Test
    fun `record should increment retry counter`() {
        val initialCount = accumulator.getCount(devicePoolId, test)

        assertThat(initialCount).isEqualTo(0)

        accumulator.record(devicePoolId, test)
        val countAfterFirstRecord = accumulator.getCount(devicePoolId, test)

        assertThat(countAfterFirstRecord).isEqualTo(1)

        accumulator.record(devicePoolId, test)
        val countAfterSecondRecord = accumulator.getCount(devicePoolId, test)

        assertThat(countAfterSecondRecord).isEqualTo(2)
    }

    @Test
    fun `record should increment only associated counter`() {
        val test2 = generateTest(method = "testMethod2")

        val initialTest1Count = accumulator.getCount(devicePoolId, test)
        val initialTest2Count = accumulator.getCount(devicePoolId, test2)

        assertThat(initialTest1Count).isEqualTo(0)
        assertThat(initialTest2Count).isEqualTo(0)

        accumulator.record(devicePoolId, test)
        val test1CountAfterFirstRecord = accumulator.getCount(devicePoolId, test)
        val test2CountAfterFirstRecord = accumulator.getCount(devicePoolId, test2)

        assertThat(test1CountAfterFirstRecord).isEqualTo(1)
        assertThat(test2CountAfterFirstRecord).isEqualTo(0)

        accumulator.record(devicePoolId, test2)
        val test1CountAfterSecondRecord = accumulator.getCount(devicePoolId, test)
        val test2CountAfterSecondRecord = accumulator.getCount(devicePoolId, test2)

        assertThat(test1CountAfterSecondRecord).isEqualTo(1)
        assertThat(test2CountAfterSecondRecord).isEqualTo(1)

        accumulator.record(devicePoolId, test2)
        val test1CountAfterThirdRecord = accumulator.getCount(devicePoolId, test)
        val test2CountAfterThirdRecord = accumulator.getCount(devicePoolId, test2)

        assertThat(test1CountAfterThirdRecord).isEqualTo(1)
        assertThat(test2CountAfterThirdRecord).isEqualTo(2)
    }

    @Test
    fun `record should increment counter only for specified pool id`() {
        val pool1 = DevicePoolId("DevicePoolId-1")
        val pool2 = DevicePoolId("DevicePoolId-2")
        val test = generateTest()

        val initialPool1Count = accumulator.getCount(pool1, test)
        val initialPool2Count = accumulator.getCount(pool2, test)

        assertThat(initialPool1Count).isEqualTo(0)
        assertThat(initialPool2Count).isEqualTo(0)

        accumulator.record(pool1, test)
        accumulator.record(pool2, test)
        val pool1CountAfterRecord = accumulator.getCount(pool1, test)
        val pool2CountAfterRecord = accumulator.getCount(pool2, test)

        assertThat(pool1CountAfterRecord).isEqualTo(1)
        assertThat(pool2CountAfterRecord).isEqualTo(1)
    }
}
