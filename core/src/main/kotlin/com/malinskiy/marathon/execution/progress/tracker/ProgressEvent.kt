package com.malinskiy.marathon.execution.progress.tracker

sealed class ProgressEvent {
    data object Passed : ProgressEvent()
    data object Failed : ProgressEvent()
    data object Ignored : ProgressEvent()
}
