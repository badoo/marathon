package com.malinskiy.marathon.test

import com.malinskiy.marathon.execution.ComponentInfo
import com.malinskiy.marathon.execution.TestParser

class StubTestParser : TestParser {
    var tests: List<Test> = emptyList()

    override suspend fun extract(componentInfo: ComponentInfo): List<Test> = tests
}
