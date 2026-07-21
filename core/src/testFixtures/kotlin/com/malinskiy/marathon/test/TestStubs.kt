package com.malinskiy.marathon.test

import com.malinskiy.marathon.execution.ComponentInfo
import com.malinskiy.marathon.execution.StubComponentInfo

fun stubTest(
    pkg: String = "test",
    clazz: String = "SimpleTest",
    method: String = "method",
    componentInfo: ComponentInfo = StubComponentInfo(),
    vararg annotations: String
) = Test(pkg, clazz, method, annotations.map { MetaProperty(it) }, componentInfo)

fun stubTestBatch(
    vararg tests: Test,
    id: String = "test_batch_id",
    componentInfo: ComponentInfo = tests.firstOrNull()?.componentInfo ?: StubComponentInfo()
) = TestBatch(id = id, tests = tests.toList(), componentInfo = componentInfo)

fun stubTests(
    count: Int,
    pkg: String = "pkg",
    clazz: String = "clazz",
    method: String = "method",
    componentInfo: ComponentInfo = StubComponentInfo(),
    vararg annotations: String
): List<Test> = (0 until count).map {
    stubTest("$pkg$it", "$clazz$it", "$method$it", componentInfo, *annotations)
}
