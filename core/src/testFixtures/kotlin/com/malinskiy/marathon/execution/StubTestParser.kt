package com.malinskiy.marathon.execution

import com.malinskiy.marathon.test.Test

class StubTestParser : TestParser {
    var tests: List<Test> = emptyList()

    override suspend fun extract(componentInfo: ComponentInfo): List<Test> = tests.filter { it.componentInfo == componentInfo }
}
