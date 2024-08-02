package com.malinskiy.marathon.worker

import com.malinskiy.marathon.execution.ComponentInfo

interface WorkerHandler : AutoCloseable {
    fun scheduleTests(componentInfo: ComponentInfo)
    fun await()
}
