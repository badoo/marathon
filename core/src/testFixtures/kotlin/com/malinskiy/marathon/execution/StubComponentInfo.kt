package com.malinskiy.marathon.execution

data class StubComponentInfo(
    override val name: String = "some-name",
    val someInfo: String = "test"
) : ComponentInfo
