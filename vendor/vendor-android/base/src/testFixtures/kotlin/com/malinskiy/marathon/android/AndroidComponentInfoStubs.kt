package com.malinskiy.marathon.android

import java.io.File

fun stubAndroidComponentInfo(
    name: String = "component",
    applicationId: String? = null,
    testApplicationId: String = "com.example.test",
    applicationOutput: File? = null,
    testApplicationOutput: File = File("test.apk")
): AndroidComponentInfo = AndroidComponentInfo(
    name = name,
    applicationId = applicationId,
    testApplicationId = testApplicationId,
    applicationOutput = applicationOutput,
    testApplicationOutput = testApplicationOutput
)
