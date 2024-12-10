package com.malinskiy.marathon.execution

import com.malinskiy.marathon.test.Test
import com.malinskiy.marathon.test.TestComponentInfo
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.junit.jupiter.api.Assertions.assertIterableEquals

object SimpleClassnameFilterSpec : Spek({
    val simpleTest = stubTest("SimpleTest")
    val complexTest = stubTest("ComplexTest")
    val someClass = stubTest("SomeClass")

    describe("a simple classname filter") {
        val simpleClassnameFilter =
            SimpleClassnameFilter("""^((?!Abstract).)*Test${'$'}""".toRegex())

        group("a bunch of tests") {
            val tests = listOf(
                simpleTest,
                complexTest,
                someClass
            )
            it("should filter properly") {
                assertIterableEquals(listOf(simpleTest, complexTest), simpleClassnameFilter.filter(tests))
            }

            it("should filterNot properly") {
                assertIterableEquals(listOf(someClass), simpleClassnameFilter.filterNot(tests))
            }
        }
    }
})

private fun stubTest(clazz: String) = Test("com.example", clazz, "fakeMethod", emptyList(), TestComponentInfo())
