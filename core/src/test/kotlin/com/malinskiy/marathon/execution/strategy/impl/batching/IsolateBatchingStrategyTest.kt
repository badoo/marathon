package com.malinskiy.marathon.execution.strategy.impl.batching

import com.malinskiy.marathon.analytics.external.Analytics
import com.malinskiy.marathon.analytics.external.NoOpMetricsProvider
import com.malinskiy.marathon.generateTests
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.LinkedList
import com.malinskiy.marathon.test.Test as MarathonTest

class IsolateBatchingStrategyTest {
    private val analytics = Analytics(NoOpMetricsProvider())

    @Test
    fun `should return batches with size = 1`() {
        val strategy = IsolateBatchingStrategy()
        val queue = LinkedList<MarathonTest>()
        val tests = generateTests(50)
        queue.addAll(tests)

        assertThat(queue).hasSize(50)

        val batch1 = strategy.process(queue, analytics)

        assertThat(batch1.tests).hasSize(1)
        assertThat(queue).hasSize(49)

        val batch2 = strategy.process(queue, analytics)

        assertThat(batch2.tests).hasSize(1)
        assertThat(queue).hasSize(48)

        val batch3 = strategy.process(queue, analytics)

        assertThat(batch3.tests).hasSize(1)
        assertThat(queue).hasSize(47)
    }
}
