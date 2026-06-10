# allure-junit4-aspect

AspectJ based JUnit 4 integration for Gradle test execution.

Use this module for Gradle projects that run JUnit 4 tests with Gradle's built-in runner and cannot register a JUnit 4 `RunListener` directly.

## Supported Versions

- Allure Java 3.x requires Java 17 or newer.
- This module targets JUnit 4 and delegates reporting to `allure-junit4`.
- The current build validates against JUnit 4.13.2 and AspectJ 1.9.25.1.

## Installation

Gradle:

```kotlin
val aspectjAgent by configurations.creating

dependencies {
    testImplementation(platform("io.qameta.allure:allure-bom:<allure-version>"))
    testImplementation("io.qameta.allure:allure-junit4-aspect")
    testRuntimeOnly("org.aspectj:aspectjrt:<aspectj-version>")
    aspectjAgent("org.aspectj:aspectjweaver:<aspectj-version>")
}

tasks.test {
    useJUnit()
    doFirst {
        jvmArgs("-javaagent:${aspectjAgent.singleFile}")
    }
}
```

Maven users normally use `allure-junit4` with a listener-capable test runner. Use this AspectJ artifact only when you specifically need weaving-based listener registration.

## Setup

Enable the AspectJ weaver for the test JVM. The aspects add the Allure JUnit 4 listener to `RunNotifier` and apply the Allure JUnit 4 test-plan filter during JUnit 4 execution.

If your runner already lets you register `AllureJunit4` directly, prefer `allure-junit4`.

## Report Output

- The same JUnit 4 tests, assumptions, ignored tests, labels, links, and failures reported by `allure-junit4`.
- Automatic listener registration for Gradle's built-in JUnit 4 test task.
- Test-plan filtering without manual JUnit 4 runner wiring.

For large test classpaths, consider an AspectJ `META-INF/aop.xml` that limits weaving to Allure, JUnit, and your test packages.
