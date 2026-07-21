package com.malinskiy.marathon.android

import com.malinskiy.marathon.execution.ComponentInfo
import java.io.File

data class AndroidComponentInfo(
    override val name: String,
    val applicationId: String?,
    val testApplicationId: String,
    val applicationOutput: File?,
    val testApplicationOutput: File,
    val instrumentationArgs: Map<String, String> = emptyMap()
) : ComponentInfo
