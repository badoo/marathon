package com.malinskiy.marathon.execution

import com.malinskiy.marathon.test.TestComponentInfo
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import com.malinskiy.marathon.test.Test as MarathonTest

class SimpleClassnameFilterTest {
    private val simpleTest = stubTest("SimpleTest")
    private val complexTest = stubTest("ComplexTest")
    private val someClass = stubTest("SomeClass")
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

    private fun stubTest(clazz: String) =
        MarathonTest(
            pkg = "com.example",
            clazz = clazz,
            method = "fakeMethod",
            metaProperties = emptyList(),
            componentInfo = TestComponentInfo()
        )
}
