package com.malinskiy.marathon.execution

import com.malinskiy.marathon.test.stubTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class FullyQualifiedClassnameFilterTest {
    private val simpleTest = stubTest(pkg = "com.example", clazz = "ClassTest")
    private val complexTest = stubTest(pkg = "com.example.subpackage", clazz = "ClassTest")
    private val someClass = stubTest(pkg = "com.sample", clazz = "ClassTest")
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
}
