package com.malinskiy.marathon.execution.strategy.impl.retry.fixedquota

import com.malinskiy.marathon.device.DevicePoolId
import com.malinskiy.marathon.generateTest
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.junit.jupiter.api.Assertions.assertEquals

class PoolTestFailureAccumulatorSpek : Spek({
    describe("") {
        val accumulator by memoized {
            PoolTestFailureAccumulator()
        }
        group("same device pool id") {
            val devicePoolId = DevicePoolId("DevicePoolId")
            val test = generateTest()

            it("default retry count value = 0") {
                assertEquals(0, accumulator.getCount(devicePoolId, test))
            }
            it("record should increment retry counter") {
                assertEquals(0, accumulator.getCount(devicePoolId, test))
                accumulator.record(devicePoolId, test)
                assertEquals(1, accumulator.getCount(devicePoolId, test))
                accumulator.record(devicePoolId, test)
                assertEquals(2, accumulator.getCount(devicePoolId, test))
            }

            it("record should increment only associated counter") {
                val test2 = generateTest(method = "testMethod2")
                assertEquals(0, accumulator.getCount(devicePoolId, test))
                assertEquals(0, accumulator.getCount(devicePoolId, test2))
                accumulator.record(devicePoolId, test)
                assertEquals(1, accumulator.getCount(devicePoolId, test))
                assertEquals(0, accumulator.getCount(devicePoolId, test2))
                accumulator.record(devicePoolId, test2)
                assertEquals(1, accumulator.getCount(devicePoolId, test))
                assertEquals(1, accumulator.getCount(devicePoolId, test2))
                accumulator.record(devicePoolId, test2)
                assertEquals(1, accumulator.getCount(devicePoolId, test))
                assertEquals(2, accumulator.getCount(devicePoolId, test2))
            }
        }
        group("different device pool ids") {
            it("record should increment counter only for specified pool id") {
                val pool1 = DevicePoolId("DevicePoolId-1")
                val pool2 = DevicePoolId("DevicePoolId-2")
                val test = generateTest()
                assertEquals(0, accumulator.getCount(pool1, test))
                assertEquals(0, accumulator.getCount(pool2, test))
                accumulator.record(pool1, test)
                accumulator.record(pool2, test)
                assertEquals(1, accumulator.getCount(pool1, test))
                assertEquals(1, accumulator.getCount(pool2, test))
            }
        }
    }
})
