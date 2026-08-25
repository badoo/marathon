package com.malinskiy.marathon.android

import com.linkedin.dex.parser.DecodedValue
import com.linkedin.dex.parser.DexParser
import com.linkedin.dex.parser.TestAnnotation
import com.malinskiy.marathon.execution.ComponentInfo
import com.malinskiy.marathon.execution.TestParser
import com.malinskiy.marathon.test.MetaProperty
import com.malinskiy.marathon.test.Test
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

class AndroidTestParser(private val ioDispatcher: CoroutineDispatcher) : TestParser {
    override suspend fun extract(componentInfo: ComponentInfo): List<Test> = withContext(ioDispatcher) {
        require(componentInfo is AndroidComponentInfo)

        val tests = DexParser.findTestMethods(componentInfo.testApplicationOutput.absolutePath)

        tests.map { test ->
            val testName = test.testName
            val annotations = test.annotations.map { it.toMetaProperty() }
            val split = testName.split("#")

            check(split.size == 2) { "Can't parse test $testName" }

            val methodName = split[1]
            val packageAndClassName = split[0]

            val lastDotIndex = packageAndClassName.indexOfLast { c -> c == '.' }

            check(lastDotIndex != -1) { "Can't parse package name for test $testName" }

            val packageName = packageAndClassName.substring(0 until lastDotIndex)
            val className = packageAndClassName.substring(lastDotIndex + 1 until packageAndClassName.length)

            Test(packageName, className, methodName, annotations, componentInfo)
        }
    }

    private fun TestAnnotation.toMetaProperty(): MetaProperty {
        val metaMap = values.mapValues {
            val realValue = when (val value = it.value) {
                is DecodedValue.DecodedString -> value.value
                is DecodedValue.DecodedByte -> value.value
                is DecodedValue.DecodedShort -> value.value
                is DecodedValue.DecodedChar -> value.value
                is DecodedValue.DecodedInt -> value.value
                is DecodedValue.DecodedLong -> value.value
                is DecodedValue.DecodedFloat -> value.value
                is DecodedValue.DecodedDouble -> value.value
                is DecodedValue.DecodedType -> value.value
                DecodedValue.DecodedNull -> null
                is DecodedValue.DecodedBoolean -> value.value
                is DecodedValue.DecodedEnum -> value.value
                is DecodedValue.DecodedArrayValue -> value.values
                is DecodedValue.DecodedAnnotationValue -> null
            }
            realValue
        }
        return MetaProperty(name, metaMap)
    }
}
