package com.malinskiy.marathon.execution

import com.malinskiy.marathon.test.Test

interface TestOwnerProvider {

    fun getTestOwner(test: Test): TestOwner

    data class TestOwner(
        val team: String?,
        val component: String
    )
}
