# allure-junit4-aspect

AspectJ based JUnit 4 integration for Gradle test execution.

## Coordinates

`io.qameta.allure:allure-junit4-aspect`

```kotlin
dependencies {
    testImplementation(platform("io.qameta.allure:allure-bom:<allure-version>"))
    testImplementation("io.qameta.allure:allure-junit4-aspect")
}
```

## Why This Exists

JUnit 4 supports reporting through `RunListener`, and `allure-junit4` provides `AllureJunit4`. Gradle's built-in JUnit 4 test runner does not expose a supported listener configuration hook, so this module uses AspectJ to register the listener and apply the Allure test-plan filter during JUnit 4 execution.

Use this artifact for Gradle + JUnit 4 projects that need automatic Allure lifecycle reporting. If your runner already lets you register `AllureJunit4` directly, prefer `allure-junit4`.

## Gradle Setup

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

## What It Does

- `AllureJunit4ListenerAspect` adds the Allure JUnit 4 listener to `RunNotifier`.
- `AllureJunit4FilterAspect` applies `AllureJunit4Filter` to `Request.getRunner()`.
- The aspects run only when the AspectJ weaver is enabled for the test JVM.

For large test classpaths, consider an AspectJ `META-INF/aop.xml` that limits weaving to Allure, JUnit, and your test packages.
