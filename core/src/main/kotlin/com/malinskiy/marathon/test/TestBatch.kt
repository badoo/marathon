package com.malinskiy.marathon.test

import com.malinskiy.marathon.execution.ComponentInfo
import java.util.UUID

data class TestBatch(
    val id: String = UUID.randomUUID().toString(),
    val tests: List<Test>,
    val componentInfo: ComponentInfo
) {

    init {
        val componentInfos = tests.map { it.componentInfo }.distinct()
        require(componentInfos.size <= 1) { "TestBatch contains different ComponentInfo instances" }

        if (tests.isNotEmpty()) {
            val componentInfosFromTests = componentInfos.first()
            require(componentInfosFromTests == componentInfo) {
                "Expected all tests to contain $componentInfo but the tests list contains $componentInfosFromTests"
            }
        }
    }
}
