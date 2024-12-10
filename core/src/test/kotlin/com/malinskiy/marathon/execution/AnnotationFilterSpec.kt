package com.malinskiy.marathon.execution

import com.malinskiy.marathon.test.MetaProperty
import com.malinskiy.marathon.test.Test
import com.malinskiy.marathon.test.TestComponentInfo
import org.amshove.kluent.shouldBeEqualTo
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it

object AnnotationFilterSpec : Spek({
    val test1 = stubTest("com.example.AnnotationOne", "com.sample.AnnotationTwo")
    val test2 = stubTest("com.example.AnnotationOne")
    val test3 = stubTest(*arrayOf<MetaProperty>())

    describe("an annotation filter") {
        val filter = AnnotationFilter("""com\.example.*""".toRegex())

        group("a bunch of tests") {
            val tests = listOf(test1, test2, test3)
            it("should filter properly") {
                filter.filter(tests) shouldBeEqualTo listOf(test1, test2)
            }

            it("should filterNot properly") {
                filter.filterNot(tests) shouldBeEqualTo listOf(test3)
            }
        }
    }
})

private fun stubTest(vararg annotations: MetaProperty) =
    Test("com.sample", "SimpleTest", "fakeMethod", listOf(*annotations), TestComponentInfo())

private fun stubTest(vararg annotations: String) =
    Test("com.sample", "SimpleTest", "fakeMethod", annotations.map { MetaProperty(it) }, TestComponentInfo())
