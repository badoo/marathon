package com.malinskiy.marathon.execution.progress.tracker

sealed class ProgressTestState {
    data object Started : ProgressTestState()
    data object Passed : ProgressTestState()
    data object Failed : ProgressTestState()
    data object Ignored : ProgressTestState()
}
