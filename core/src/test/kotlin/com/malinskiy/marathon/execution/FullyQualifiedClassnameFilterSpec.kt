package com.malinskiy.marathon.execution

import com.malinskiy.marathon.test.Test
import com.malinskiy.marathon.test.TestComponentInfo
import org.amshove.kluent.shouldBeEqualTo
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it

object FullyQualifiedClassnameFilterSpec : Spek(
    {
        val simpleTest = stubTest("com.example")
        val complexTest = stubTest("com.example.subpackage")
        val someClass = stubTest("com.sample")


        describe("a FQ classname filter") {
            val filter = FullyQualifiedClassnameFilter("""com\.example\.ClassTest""".toRegex())

            group("a bunch of tests") {
                val tests = listOf(
                    simpleTest,
                    complexTest,
                    someClass
                )
                it("should filter properly") {
                    filter.filter(tests) shouldBeEqualTo listOf(simpleTest)
                }
                it("should filterNot properly") {
                    filter.filterNot(tests) shouldBeEqualTo listOf(complexTest, someClass)
                }
            }
        }
    })

private fun stubTest(pkg: String) = Test(pkg, "ClassTest", "fakeMethod", emptyList(), TestComponentInfo())
