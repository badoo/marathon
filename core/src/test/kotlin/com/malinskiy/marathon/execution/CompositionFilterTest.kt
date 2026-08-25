package com.malinskiy.marathon.execution

import com.malinskiy.marathon.test.stubTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class CompositionFilterTest {
    private val dogTest = stubTest(clazz = "FilterAnimalDogTest", annotations = arrayOf("com.example.BestAnimal"))
    private val catTest = stubTest(clazz = "FilterAnimalCatTest", annotations = arrayOf(""))
    private val horseTest = stubTest(clazz = "FilterAnimalHorseTest", annotations = arrayOf(""))
    private val tests = listOf(dogTest, catTest, horseTest)

    private val filterUnion = CompositionFilter(
        listOf(
            SimpleClassnameFilter(".*Cat.*".toRegex()),
            AnnotationFilter("com.example.BestAnimal".toRegex()),
        ),
        CompositionFilter.OPERATION.UNION,
    )

    private val filterIntersection = CompositionFilter(
        listOf(
            SimpleClassnameFilter(".*Dog.*".toRegex()),
            AnnotationFilter("com.example.BestAnimal".toRegex()),
        ),
        CompositionFilter.OPERATION.INTERSECTION,
    )

    private val filterSubtract = CompositionFilter(
        listOf(
            SimpleClassnameFilter(".*Dog.*".toRegex()),
            AnnotationFilter("com.example.BestAnimal".toRegex()),
        ),
        CompositionFilter.OPERATION.SUBTRACT,
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
}
