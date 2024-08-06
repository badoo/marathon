package com.malinskiy.marathon

import com.malinskiy.marathon.execution.AnnotationFilter
import com.malinskiy.marathon.execution.FullyQualifiedClassnameFilter
import com.malinskiy.marathon.execution.SimpleClassnameFilter
import com.malinskiy.marathon.execution.TestFilter
import com.malinskiy.marathon.execution.TestPackageFilter
import org.gradle.api.provider.SetProperty

interface FilterConfiguration {
    val annotationFilter: SetProperty<String>
    val fullyQualifiedClassnameFilter: SetProperty<String>
    val simpleClassNameFilter: SetProperty<String>
    val testPackageFilter: SetProperty<String>
}

internal fun FilterConfiguration.toList(): List<TestFilter> =
    annotationFilter.get().map { AnnotationFilter(it.toRegex()) } +
        fullyQualifiedClassnameFilter.get().map { FullyQualifiedClassnameFilter(it.toRegex()) } +
        testPackageFilter.get().map { TestPackageFilter(it.toRegex()) } +
        simpleClassNameFilter.get().map { SimpleClassnameFilter(it.toRegex()) }
