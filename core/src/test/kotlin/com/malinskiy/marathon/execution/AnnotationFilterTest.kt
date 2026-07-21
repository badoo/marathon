package com.malinskiy.marathon.execution

import com.malinskiy.marathon.test.stubTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class AnnotationFilterTest {
    private val test1 = stubTest(annotations = arrayOf("com.example.AnnotationOne", "com.sample.AnnotationTwo"))
    private val test2 = stubTest(annotations = arrayOf("com.example.AnnotationOne"))
    private val test3 = stubTest()
    private val tests = listOf(test1, test2, test3)
    private val filter = AnnotationFilter("""com\.example.*""".toRegex())

    @Test
    fun `should filter properly`() {
        val result = filter.filter(tests)

        assertThat(result).containsExactly(test1, test2)
    }

    @Test
    fun `should filterNot properly`() {
        val result = filter.filterNot(tests)

        assertThat(result).containsExactly(test3)
    }
}
