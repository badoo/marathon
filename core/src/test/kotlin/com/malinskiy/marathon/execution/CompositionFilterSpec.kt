package com.malinskiy.marathon.execution

import com.malinskiy.marathon.test.MetaProperty
import com.malinskiy.marathon.test.Test
import com.malinskiy.marathon.test.TestComponentInfo
import org.amshove.kluent.shouldBeEqualTo
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it

object CompositionFilterSpec : Spek({
    val dogTest = stubTest("FilterAnimalDogTest", "com.example.BestAnimal")
    val catTest = stubTest("FilterAnimalCatTest", "")
    val horseTest = stubTest("FilterAnimalHorseTest", "")

    describe("a CompositionFilter with different Filters and Union Operation") {
        val filterUnion = CompositionFilter(
            listOf(
                SimpleClassnameFilter(".*Cat.*".toRegex()),
                AnnotationFilter("com.example.BestAnimal".toRegex())
            ),
            CompositionFilter.OPERATION.UNION
        )

        group("a bunch of tests") {
            val tests = listOf(
                dogTest,
                catTest,
                horseTest
            )
            it("should filter properly the union") {
                filterUnion.filter(tests) shouldBeEqualTo listOf(catTest, dogTest)
            }
            it("should filterNot properly the union") {
                filterUnion.filterNot(tests) shouldBeEqualTo listOf(horseTest)
            }
        }
    }

    describe("a CompositionFilter with different Filters and Intersection Operation") {
        val filterIntersection = CompositionFilter(
            listOf(
                SimpleClassnameFilter(".*Dog.*".toRegex()),
                AnnotationFilter("com.example.BestAnimal".toRegex())
            ),
            CompositionFilter.OPERATION.INTERSECTION
        )

        group("a bunch of tests") {
            val tests = listOf(
                dogTest,
                catTest,
                horseTest
            )
            it("should filter properly the intersection") {
                filterIntersection.filter(tests) shouldBeEqualTo listOf(dogTest)
            }
            it("should filterNot properly the intersection") {
                filterIntersection.filterNot(tests) shouldBeEqualTo listOf(catTest, horseTest)
            }
        }
    }

    describe("a CompositionFilter with different Filters and Subtract Operation") {
        val filterIntersection = CompositionFilter(
            listOf(
                SimpleClassnameFilter(".*Dog.*".toRegex()),
                AnnotationFilter("com.example.BestAnimal".toRegex())
            ),
            CompositionFilter.OPERATION.SUBTRACT
        )

        group("a bunch of tests") {
            val tests = listOf(
                dogTest,
                catTest,
                horseTest
            )
            it("should filter properly the subtract") {
                filterIntersection.filter(tests) shouldBeEqualTo listOf(catTest, horseTest)
            }
            it("should filterNot properly the subtract") {
                filterIntersection.filterNot(tests) shouldBeEqualTo listOf(dogTest)
            }
        }
    }
})

private fun stubTest(className: String, vararg annotations: MetaProperty) =
    Test("com.example", className, "fakeMethod", listOf(*annotations), TestComponentInfo())

private fun stubTest(className: String, vararg annotations: String) =
    Test("com.sample", className, "fakeMethod", annotations.map { MetaProperty(it) }, TestComponentInfo())
