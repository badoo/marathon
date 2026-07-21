package com.malinskiy.marathon.execution

import com.malinskiy.marathon.test.stubTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class SimpleClassnameFilterTest {
    private val simpleTest = stubTest(clazz = "SimpleTest")
    private val complexTest = stubTest(clazz = "ComplexTest")
    private val someClass = stubTest(clazz = "SomeClass")
    private val tests = listOf(simpleTest, complexTest, someClass)
    private val simpleClassnameFilter = SimpleClassnameFilter("""^((?!Abstract).)*Test${'$'}""".toRegex())

    @Test
    fun `should filter properly`() {
        val result = simpleClassnameFilter.filter(tests)

        assertThat(result).containsExactly(simpleTest, complexTest)
    }

    @Test
    fun `should filterNot properly`() {
        val result = simpleClassnameFilter.filterNot(tests)

        assertThat(result).containsExactly(someClass)
    }
}
