package com.malinskiy.marathon.exceptions

class NoDevicesException(cause: Throwable) : RuntimeException("No devices found", cause)
