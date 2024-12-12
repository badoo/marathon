package com.malinskiy.marathon.log

import org.slf4j.Logger
import org.slf4j.Marker

internal class LifecycleLoggerWrapper(underlyingLogger: Logger) : Logger by underlyingLogger {
    override fun debug(format: String, vararg arguments: Any) {
        info(LIFECYCLE, format, *arguments)
    }

    override fun debug(format: String?, arg: Any?) {
        info(LIFECYCLE, format, arg)
    }

    override fun debug(format: String?, arg1: Any?, arg2: Any?) {
        info(LIFECYCLE, format, arg1, arg2)
    }

    override fun debug(msg: String?, t: Throwable?) {
        info(LIFECYCLE, msg, t)
    }

    override fun debug(msg: String?) {
        info(LIFECYCLE, msg)
    }

    override fun info(format: String?, arg1: Any?, arg2: Any?) {
        info(LIFECYCLE, format, arg1, arg2)
    }

    override fun info(msg: String?) {
        info(LIFECYCLE, msg)
    }

    override fun info(format: String?, vararg arguments: Any?) {
        info(LIFECYCLE, format, *arguments)
    }

    override fun info(msg: String?, t: Throwable?) {
        info(LIFECYCLE, msg, t)
    }

    override fun info(format: String?, arg: Any?) {
        info(LIFECYCLE, format, arg)
    }

    override fun trace(format: String?, arg1: Any?, arg2: Any?) {
        info(LIFECYCLE, format, arg1, arg2)
    }

    override fun trace(msg: String?) {
        info(LIFECYCLE, msg)
    }

    override fun trace(format: String?, arg: Any?) {
        info(LIFECYCLE, format, arg)
    }

    override fun trace(msg: String?, t: Throwable?) {
        info(LIFECYCLE, msg, t)
    }

    override fun trace(format: String?, vararg arguments: Any?) {
        info(LIFECYCLE, format, *arguments)
    }

    companion object {
        private val LIFECYCLE = getGradleLifecycleMarker()

        private fun getGradleLifecycleMarker(): Marker? {
            return try {
                val gradleLogging = Class.forName("org.gradle.api.logging.Logging")
                gradleLogging.getDeclaredField("LIFECYCLE").get(null) as Marker?
            } catch (@Suppress("SwallowedException") e: ClassNotFoundException) {
                null
            } catch (@Suppress("SwallowedException") e: NoSuchFieldException) {
                null
            }
        }
    }
}
