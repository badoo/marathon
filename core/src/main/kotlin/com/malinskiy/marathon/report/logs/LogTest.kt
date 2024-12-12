package com.malinskiy.marathon.report.logs

import com.malinskiy.marathon.test.Test

data class LogTest(
    val pkg: String,
    val clazz: String,
    val method: String
) {
    override fun toString(): String = "$pkg.$clazz#$method"
}

fun Test.toLogTest(): LogTest =
    LogTest(pkg, clazz, method)
