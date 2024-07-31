package com.malinskiy.marathon

import com.malinskiy.marathon.execution.AnnotationFilter
import com.malinskiy.marathon.execution.FullyQualifiedClassnameFilter
import com.malinskiy.marathon.execution.SimpleClassnameFilter
import com.malinskiy.marathon.execution.TestFilter
import com.malinskiy.marathon.execution.TestPackageFilter
import org.gradle.api.provider.ListProperty

interface FilterWrapper {
    val simpleClassNameFilter: ListProperty<String>
    val fullyQualifiedClassnameFilter: ListProperty<String>
    val testPackageFilter: ListProperty<String>
    val annotationFilter: ListProperty<String>
}

internal fun FilterWrapper.toList(): List<TestFilter> =
    annotationFilter.get().map { AnnotationFilter(it.toRegex()) } +
        fullyQualifiedClassnameFilter.get().map { FullyQualifiedClassnameFilter(it.toRegex()) } +
        testPackageFilter.get().map { TestPackageFilter(it.toRegex()) } +
        simpleClassNameFilter.get().map { SimpleClassnameFilter(it.toRegex()) }
