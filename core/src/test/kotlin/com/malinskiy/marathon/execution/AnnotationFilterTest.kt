package com.malinskiy.marathon.execution

import com.malinskiy.marathon.test.MetaProperty
import com.malinskiy.marathon.test.TestComponentInfo
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import com.malinskiy.marathon.test.Test as MarathonTest

class AnnotationFilterTest {
    private val test1 = stubTest("com.example.AnnotationOne", "com.sample.AnnotationTwo")
    private val test2 = stubTest("com.example.AnnotationOne")
    private val test3 = stubTest()
    private val tests = listOf(test1, test2, test3)
    private val filter = AnnotationFilter("""com\.example.*""".toRegex())

    @Test
    fun `should filter properly`() {
        val result = filter.filter(tests)

        assertThat(result).containsExactly(test1, test2)
    }

    @Test
    fun `should filterNot properly`() {
        val result = filter.filterNot(tests)

        assertThat(result).containsExactly(test3)
    }

    private fun stubTest(vararg annotations: String) =
        MarathonTest(
            pkg = "com.sample",
            clazz = "SimpleTest",
            method = "fakeMethod",
            metaProperties = annotations.map { MetaProperty(it) },
            componentInfo = TestComponentInfo()
        )
}
