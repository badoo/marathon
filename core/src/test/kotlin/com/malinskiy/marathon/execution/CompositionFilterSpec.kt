package com.malinskiy.marathon.execution

import com.malinskiy.marathon.test.MetaProperty
import com.malinskiy.marathon.test.Test
import com.malinskiy.marathon.test.TestComponentInfo
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.junit.jupiter.api.Assertions.assertIterableEquals

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
                assertIterableEquals(listOf(catTest, dogTest), filterUnion.filter(tests))
            }
            it("should filterNot properly the union") {
                assertIterableEquals(listOf(horseTest), filterUnion.filterNot(tests))
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
                assertIterableEquals(listOf(dogTest), filterIntersection.filter(tests))
            }
            it("should filterNot properly the intersection") {
                assertIterableEquals(listOf(catTest, horseTest), filterIntersection.filterNot(tests))
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
                assertIterableEquals(listOf(catTest, horseTest), filterIntersection.filter(tests))
            }
            it("should filterNot properly the subtract") {
                assertIterableEquals(listOf(dogTest), filterIntersection.filterNot(tests))
            }
        }
    }
})

private fun stubTest(className: String, vararg annotations: String) =
    Test("com.sample", className, "fakeMethod", annotations.map { MetaProperty(it) }, TestComponentInfo())
