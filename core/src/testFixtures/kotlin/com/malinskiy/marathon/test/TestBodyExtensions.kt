package com.malinskiy.marathon.test

import com.malinskiy.marathon.Marathon
import com.malinskiy.marathon.execution.ComponentInfo
import com.malinskiy.marathon.execution.StubComponentInfo
import com.malinskiy.marathon.test.factory.MarathonFactory

fun setupMarathon(f: MarathonFactory.() -> Unit): Marathon {
    val marathonFactory = MarathonFactory()
    return marathonFactory.apply(f).build()
}

suspend fun Marathon.runAsync(componentInfo: ComponentInfo = StubComponentInfo()): Boolean = use {
    start()
    scheduleTests(componentInfo)
    stopAndWaitForCompletion()
}
