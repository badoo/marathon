package com.malinskiy.marathon.cli.args

import com.malinskiy.marathon.android.serial.SerialStrategy
import com.malinskiy.marathon.exceptions.ConfigurationException
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldThrow
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import java.io.File

object FileAndroidConfigurationSpek : Spek(
    {
        describe("FileAndroidConfiguration") {
            val configuration by memoized {
                FileAndroidConfiguration(
                    null,
                    null,
                    null,
                    File.createTempFile("foo", "bar"),
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    SerialStrategy.AUTOMATIC
                )
            }

            val env = File.createTempFile("foo", "bar")
            val sdk = File.createTempFile("android", "sdk")

            group("androidSdk is null") {
                it("should throw Exception if env android sdk also is null") {
                    { configuration.toAndroidConfiguration(null) } shouldThrow ConfigurationException::class
                }
                it("should use env android sdk if it is not null") {
                    configuration.toAndroidConfiguration(env).androidSdk shouldBeEqualTo env
                }
            }
            group("android sdk is not null") {
                it("should use android sdk instead of env if both exists") {
                    configuration.copy(androidSdk = sdk).toAndroidConfiguration(env).androidSdk shouldBeEqualTo sdk
                }
            }
            group("test application output") {
                it("should be null by default") {
                    configuration.toAndroidConfiguration(env).applicationOutput shouldBeEqualTo null
                }
                it("should be null if provided") {
                    configuration.copy(applicationOutput = env).toAndroidConfiguration(env).applicationOutput shouldBeEqualTo env
                }
            }
            group("test application apk") {
                it("should be equal") {
                    configuration.copy(testApplicationOutput = env).toAndroidConfiguration(env).testApplicationOutput shouldBeEqualTo env
                }
            }
            group("auto grant permissions") {
                it("should be false by default") {
                    configuration.toAndroidConfiguration(env).autoGrantPermission shouldBeEqualTo false
                }
                it("should be equal") {
                    configuration.copy(autoGrantPermission = false).toAndroidConfiguration(env).autoGrantPermission shouldBeEqualTo false
                    configuration.copy(autoGrantPermission = true).toAndroidConfiguration(env).autoGrantPermission shouldBeEqualTo true
                }
            }
            group("adb init timeout millis") {
                it("should be 30_000 by default") {
                    configuration.toAndroidConfiguration(env).adbInitTimeoutMillis shouldBeEqualTo 30_000
                }
                it("should be equal") {
                    val timeout = 500_000
                    configuration.copy(adbInitTimeoutMillis = timeout).toAndroidConfiguration(env).adbInitTimeoutMillis shouldBeEqualTo timeout
                }
            }
            group("install options") {
                it("should be empty string by default") {
                    configuration.toAndroidConfiguration(env).installOptions shouldBeEqualTo ""
                }
                it("should be equal if provided") {
                    configuration.copy(installOptions = "-d").toAndroidConfiguration(env).installOptions shouldBeEqualTo "-d"
                }
            }
        }
    })
