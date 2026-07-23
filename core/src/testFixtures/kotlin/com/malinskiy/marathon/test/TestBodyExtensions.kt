package com.malinskiy.marathon.test

import com.malinskiy.marathon.Marathon
import com.malinskiy.marathon.execution.ComponentInfo
import com.malinskiy.marathon.execution.StubComponentInfo
import com.malinskiy.marathon.test.factory.StubMarathonFactory

fun setupMarathon(f: StubMarathonFactory.() -> Unit): Marathon {
    val marathonFactory = StubMarathonFactory().apply(f)
    return marathonFactory.createMarathon(marathonFactory.configurationFactory.build())
}

suspend fun Marathon.runAsync(componentInfo: ComponentInfo = StubComponentInfo()): Boolean = use {
    start()
    scheduleTests(componentInfo)
    stopAndWaitForCompletion()
}
