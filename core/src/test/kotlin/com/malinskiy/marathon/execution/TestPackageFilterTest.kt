package com.malinskiy.marathon.execution

import com.malinskiy.marathon.test.stubTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class TestPackageFilterTest {
    private val simpleTest = stubTest(pkg = "com.example")
    private val complexTest = stubTest(pkg = "com.example.subpackage")
    private val someClass = stubTest(pkg = "com.sample")
    private val tests = listOf(simpleTest, complexTest, someClass)
    private val filter = TestPackageFilter("""com\.example.*""".toRegex())

    @Test
    fun `should filter properly`() {
        val result = filter.filter(tests)

        assertThat(result).containsExactly(simpleTest, complexTest)
    }

    @Test
    fun `should filterNot properly`() {
        val result = filter.filterNot(tests)

        assertThat(result).containsExactly(someClass)
    }
}
