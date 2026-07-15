package com.malinskiy.marathon.execution

import com.malinskiy.marathon.test.TestComponentInfo
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import com.malinskiy.marathon.test.Test as MarathonTest

class FullyQualifiedClassnameFilterTest {
    private val simpleTest = stubTest("com.example")
    private val complexTest = stubTest("com.example.subpackage")
    private val someClass = stubTest("com.sample")
    private val tests = listOf(simpleTest, complexTest, someClass)
    private val filter = FullyQualifiedClassnameFilter("""com\.example\.ClassTest""".toRegex())

    @Test
    fun `should filter properly`() {
        val result = filter.filter(tests)

        assertThat(result).containsExactly(simpleTest)
    }

    @Test
    fun `should filterNot properly`() {
        val result = filter.filterNot(tests)

        assertThat(result).containsExactly(complexTest, someClass)
    }

    private fun stubTest(pkg: String) =
        MarathonTest(
            pkg = pkg,
            clazz = "ClassTest",
            method = "fakeMethod",
            metaProperties = emptyList(),
            componentInfo = TestComponentInfo()
        )
}
