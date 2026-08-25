package com.malinskiy.marathon.log

import org.slf4j.Logger
import org.slf4j.LoggerFactory

object MarathonLogging {
    var debug = true

    fun getLogger(name: String): Logger = LoggerFactory.getLogger(name).wrapIfNeeded()

    fun getLogger(clazz: Class<*>): Logger = LoggerFactory.getLogger(clazz).wrapIfNeeded()

    private fun Logger.wrapIfNeeded(): Logger = if (debug) LifecycleLoggerWrapper(this) else this
}
