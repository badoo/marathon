package com.malinskiy.marathon.device

import com.fasterxml.jackson.annotation.JsonCreator

enum class DeviceFeature {
    VIDEO,
    SCREENSHOT,
    ;

    companion object {
        @JvmStatic
        @JsonCreator
        fun fromString(key: String?): DeviceFeature? = key?.let {
            DeviceFeature.valueOf(it.uppercase())
        }
    }
}
