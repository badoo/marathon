package com.malinskiy.marathon.android.executor.listeners.video

import com.malinskiy.marathon.android.AndroidDevice
import com.malinskiy.marathon.log.MarathonLogging

internal class ScreenRecorderStopper(private val device: AndroidDevice) {
    private val logger = MarathonLogging.getLogger(ScreenRecorderStopper::class.java)

    fun stopScreenRecord() {
        var hasKilledScreenRecord = true
        var tries = 0
        while (hasKilledScreenRecord && tries++ < SCREEN_RECORD_KILL_ATTEMPTS) {
            hasKilledScreenRecord = attemptToGracefullyKillScreenRecord()
            Thread.sleep(PAUSE_BETWEEN_RECORDER_PROCESS_KILL.toLong())
        }
    }

    private fun grepPid(): String {
        val output = if (device.version.isGreaterOrEqualThan(26)) {
            device.safeExecuteShellCommand("ps -A | grep screenrecord")
        } else {
            device.safeExecuteShellCommand("ps | grep screenrecord")
        }

        if (output.isBlank()) {
            return ""
        }
        val split = output.split("\\s+".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        val pid = split[1]
        logger.trace("Extracted PID {} from output {}", pid, output)
        return pid
    }

    private fun attemptToGracefullyKillScreenRecord(): Boolean {
        try {
            val pid = grepPid()
            if (pid.isNotBlank()) {
                logger.trace("[{}] Killing PID {}", device.serialNumber, pid)
                device.safeExecuteShellCommand("kill -2 $pid")
                return true
            } else {
                logger.trace("[{}] Did not kill any screen recording process", device.serialNumber)
            }
        } catch (e: InterruptedException) {
            throw e
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            logger.error("[{}] Error while killing recording processes", device.serialNumber, e)
        }
        return false
    }

    companion object {
        private const val SCREEN_RECORD_KILL_ATTEMPTS = 5

        /*
        * Workaround for https://github.com/Malinskiy/marathon/issues/133
        */
        private const val PAUSE_BETWEEN_RECORDER_PROCESS_KILL = 300
    }
}
