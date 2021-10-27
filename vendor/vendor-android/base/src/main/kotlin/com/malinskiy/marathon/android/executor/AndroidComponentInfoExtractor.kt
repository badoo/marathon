package com.malinskiy.marathon.android

import com.malinskiy.marathon.execution.ComponentInfo
import com.malinskiy.marathon.execution.ComponentInfoExtractor
import com.malinskiy.marathon.execution.Configuration

class AndroidComponentInfoExtractor : ComponentInfoExtractor {

    override fun extract(configuration: Configuration): ComponentInfo {
        val androidConfiguration = configuration.vendorConfiguration as AndroidConfiguration

        return AndroidComponentInfo(
            name = configuration.name,
            applicationId = "<unknown>",
            testApplicationId = "<unknown>",
            applicationOutput = androidConfiguration.applicationOutput,
            testApplicationOutput = androidConfiguration.testApplicationOutput
        )
    }
}
