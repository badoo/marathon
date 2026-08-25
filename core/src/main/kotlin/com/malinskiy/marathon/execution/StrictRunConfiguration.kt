package com.malinskiy.marathon.execution

import com.fasterxml.jackson.annotation.JsonProperty

data class StrictRunConfiguration(
    @JsonProperty("filter", required = false) val filter: Collection<TestFilter> = emptyList(),
    @JsonProperty("runs", required = false) val runs: Int = 1,
)
