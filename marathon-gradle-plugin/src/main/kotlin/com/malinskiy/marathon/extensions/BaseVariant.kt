package com.malinskiy.marathon.extensions

import com.android.build.gradle.api.ApkVariantOutput
import com.android.build.gradle.api.ApplicationVariant
import com.android.build.gradle.api.BaseVariant
import com.android.build.gradle.api.LibraryVariant
import java.io.File

fun BaseVariant.extractApplication(): File? {
    return when (this) {
        is ApplicationVariant -> {
            val output = outputs.first() as ApkVariantOutput
            val packageApplicationTask = packageApplicationProvider.get()
            File(packageApplicationTask.outputDirectory.asFile.get(), output.outputFileName)
        }
        is LibraryVariant -> {
            null
        }
        else -> {
            throw RuntimeException("Unsupported variant type ${javaClass.canonicalName}")
        }
    }
}
