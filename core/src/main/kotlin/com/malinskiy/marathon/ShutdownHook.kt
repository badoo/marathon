package com.malinskiy.marathon

import com.malinskiy.marathon.execution.Configuration

class ShutdownHook(
    configuration: Configuration,
    private val runtime: Runtime = Runtime.getRuntime(),
    val block: () -> Unit
) : Thread() {
    private val debug = configuration.debug

    override fun run() {
        block()
    }

    fun install(): Boolean {
        return when (debug) {
            true -> try {
                runtime.addShutdownHook(this)
                true
            } catch (ignored: IllegalStateException) {
                false
            } catch (ignored: SecurityException) {
                false
            }
            else -> true
        }
    }

    fun uninstall(): Boolean {
        return when (debug) {
            true -> try {
                runtime.removeShutdownHook(this)
                true
            } catch (ignored: IllegalStateException) {
                false
            } catch (ignored: SecurityException) {
                false
            }
            else -> true
        }
    }
}
