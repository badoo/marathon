package com.malinskiy.marathon.execution

import com.malinskiy.marathon.test.Test
import com.malinskiy.marathon.test.TestComponentInfo
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.junit.jupiter.api.Assertions.assertIterableEquals

object TestPackageFilterSpec : Spek({
    val simpleTest = stubTest("com.example")
    val complexTest = stubTest("com.example.subpackage")
    val someClass = stubTest("com.sample")

    describe("a simple classname filter") {
        val simpleClassnameFilter = TestPackageFilter("""com\.example.*""".toRegex())

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

private fun stubTest(pkg: String) = Test(pkg, "SimpleTest", "fakeMethod", emptyList(), TestComponentInfo())
