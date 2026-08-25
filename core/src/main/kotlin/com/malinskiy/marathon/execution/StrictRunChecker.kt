package com.malinskiy.marathon.execution

import com.malinskiy.marathon.test.Test

interface StrictRunChecker {
    fun isStrictRun(test: Test): Boolean
    fun hasFailFastFailures(stackTrace: String? = null): Boolean
}

class ConfigurationStrictRunChecker(private val configuration: Configuration) : StrictRunChecker {
    override fun isStrictRun(test: Test): Boolean = configuration.strictMode || configuration.strictRunConfiguration.filter.matches(test)

    override fun hasFailFastFailures(stackTrace: String?): Boolean = stackTrace != null && configuration.failFastFailureRegexes.any { it.matches(stackTrace) }
}
