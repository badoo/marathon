package com.malinskiy.marathon

import com.malinskiy.marathon.execution.Configuration

fun interface MarathonFactory {
    fun createMarathon(configuration: Configuration): Marathon
}
