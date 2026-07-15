package com.malinskiy.marathon.execution

import com.malinskiy.marathon.test.MetaProperty
import com.malinskiy.marathon.test.TestComponentInfo
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import com.malinskiy.marathon.test.Test as MarathonTest

class CompositionFilterTest {
    private val dogTest = stubTest("FilterAnimalDogTest", "com.example.BestAnimal")
    private val catTest = stubTest("FilterAnimalCatTest", "")
    private val horseTest = stubTest("FilterAnimalHorseTest", "")
    private val tests = listOf(dogTest, catTest, horseTest)

    private val filterUnion = CompositionFilter(
        listOf(
            SimpleClassnameFilter(".*Cat.*".toRegex()),
            AnnotationFilter("com.example.BestAnimal".toRegex())
        ),
        CompositionFilter.OPERATION.UNION
    )

    private val filterIntersection = CompositionFilter(
        listOf(
            SimpleClassnameFilter(".*Dog.*".toRegex()),
            AnnotationFilter("com.example.BestAnimal".toRegex())
        ),
        CompositionFilter.OPERATION.INTERSECTION
    )

    private val filterSubtract = CompositionFilter(
        listOf(
            SimpleClassnameFilter(".*Dog.*".toRegex()),
            AnnotationFilter("com.example.BestAnimal".toRegex())
        ),
        CompositionFilter.OPERATION.SUBTRACT
    )

    @Test
    fun `should filter properly the union`() {
        val result = filterUnion.filter(tests)

        assertThat(result).containsExactly(catTest, dogTest)
    }

    @Test
    fun `should filterNot properly the union`() {
        val result = filterUnion.filterNot(tests)

        assertThat(result).containsExactly(horseTest)
    }

    @Test
    fun `should filter properly the intersection`() {
        val result = filterIntersection.filter(tests)

        assertThat(result).containsExactly(dogTest)
    }

    @Test
    fun `should filterNot properly the intersection`() {
        val result = filterIntersection.filterNot(tests)

        assertThat(result).containsExactly(catTest, horseTest)
    }

    @Test
    fun `should filter properly the subtract`() {
        val result = filterSubtract.filter(tests)

        assertThat(result).containsExactly(catTest, horseTest)
    }

    @Test
    fun `should filterNot properly the subtract`() {
        val result = filterSubtract.filterNot(tests)

        assertThat(result).containsExactly(dogTest)
    }

    private fun stubTest(className: String, vararg annotations: String) =
        MarathonTest(
            pkg = "com.sample",
            clazz = className,
            method = "fakeMethod",
            metaProperties = annotations.map { MetaProperty(it) },
            componentInfo = TestComponentInfo()
        )
}
