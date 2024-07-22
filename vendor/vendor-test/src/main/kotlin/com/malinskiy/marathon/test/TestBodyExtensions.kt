package com.malinskiy.marathon.test

import com.malinskiy.marathon.Marathon
import com.malinskiy.marathon.execution.ComponentInfo
import com.malinskiy.marathon.test.factory.MarathonFactory
import org.jetbrains.spek.api.dsl.TestBody

fun TestBody.setupMarathon(f: MarathonFactory.() -> Unit): Marathon {
    val marathonFactory = MarathonFactory()
    return marathonFactory.apply(f).build()
}

suspend fun Marathon.runAsync(componentInfo: ComponentInfo = TestComponentInfo()): Boolean {
    start()
    scheduleTests(componentInfo)
    return stopAndWaitForCompletion()
}
