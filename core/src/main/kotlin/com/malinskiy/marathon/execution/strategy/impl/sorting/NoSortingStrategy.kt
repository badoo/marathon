package com.malinskiy.marathon.execution.strategy.impl.sorting

import com.malinskiy.marathon.analytics.external.MetricsProvider
import com.malinskiy.marathon.execution.strategy.SortingStrategy
import com.malinskiy.marathon.test.Test
import java.util.Comparator

class NoSortingStrategy : SortingStrategy {
    override fun process(metricsProvider: MetricsProvider): Comparator<Test> =
        Comparator { _, _ -> 0 }

    override fun equals(other: Any?): Boolean {
        if (other == null) return false
        val javaClass: Class<Any> = other.javaClass
        return this.javaClass.canonicalName == javaClass.canonicalName
    }

    override fun hashCode(): Int = javaClass.canonicalName.hashCode()

    override fun toString(): String = "NoSortingStrategy()"
}
