package com.malinskiy.marathon.extensions

import com.android.build.gradle.api.ApkVariantOutput
import com.android.build.gradle.api.LibraryVariantOutput
import com.android.build.gradle.api.TestVariant
import java.io.File

fun TestVariant.extractTestApplication(): File {
    return when (val output = outputs.first()) {
        is ApkVariantOutput -> {
            val packageTask =
                packageApplicationProvider.orNull ?: throw IllegalArgumentException("Can't find package application provider")
            File(packageTask.outputDirectory.asFile.get(), output.outputFileName)
        }
        is LibraryVariantOutput -> {
            output.outputFile
        }
        else -> {
            throw RuntimeException("Unsupported output type ${output.javaClass.canonicalName}")
        }
    }
}
