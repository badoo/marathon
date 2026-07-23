package com.malinskiy.marathon

import org.gradle.api.provider.SetProperty

interface FilterConfiguration {
    val annotationFilter: SetProperty<String>
    val fullyQualifiedClassnameFilter: SetProperty<String>
    val simpleClassNameFilter: SetProperty<String>
    val testPackageFilter: SetProperty<String>
}
