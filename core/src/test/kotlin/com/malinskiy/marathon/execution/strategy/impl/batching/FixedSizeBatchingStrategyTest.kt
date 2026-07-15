package com.malinskiy.marathon.execution.strategy.impl.batching

import com.malinskiy.marathon.analytics.external.Analytics
import com.malinskiy.marathon.analytics.external.NoOpMetricsProvider
import com.malinskiy.marathon.generateTests
import com.malinskiy.marathon.test.TestComponentInfo
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.LinkedList

class FixedSizeBatchingStrategyTest {
    private val analytics = Analytics(NoOpMetricsProvider())

    @Test
    fun `should create 5 batches for 50 tests with batch size 10`() {
        val tests = LinkedList(generateTests(50))
        val strategy = FixedSizeBatchingStrategy(10)
        val batch = strategy.process(tests, analytics)

        assertThat(batch.tests).hasSize(10)
    }

    @Test
    fun `should create 1 batch for 10 tests with batch size 10`() {
        val tests = LinkedList(generateTests(10))
        val strategy = FixedSizeBatchingStrategy(10)
        val batch = strategy.process(tests, analytics)

        assertThat(batch.tests).hasSize(10)
    }

    @Test
    fun `should create 2 batches for 10 tests with batch size 10 and different component infos`() {
        val componentInfo1 = TestComponentInfo("first")
        val componentInfo2 = TestComponentInfo("second")
        val tests = LinkedList(
            generateTests(5, componentInfo = componentInfo1) + generateTests(5, componentInfo = componentInfo2)
        )
        val strategy = FixedSizeBatchingStrategy(10)
        val batch1 = strategy.process(tests, analytics)

        assertThat(batch1.tests).hasSize(5)
        assertThat(batch1.tests).flatExtracting({ it.componentInfo }).containsOnly(componentInfo1)

        val batch2 = strategy.process(tests, analytics)

        assertThat(batch2.tests).hasSize(5)
        assertThat(batch2.tests).flatExtracting({ it.componentInfo }).containsOnly(componentInfo2)
    }
}
