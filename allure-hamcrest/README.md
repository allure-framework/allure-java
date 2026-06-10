# allure-hamcrest

Hamcrest assertion step integration for Allure Java.

Use this module when you want Hamcrest assertions to appear as nested steps in Allure Report.

## Supported Versions

- Allure Java 3.x requires Java 17 or newer.
- This module targets Hamcrest.
- The current build validates against Hamcrest 3.0 and AspectJ 1.9.25.1.

## Installation

Gradle:

```kotlin
val aspectjAgent by configurations.creating

dependencies {
    testImplementation(platform("io.qameta.allure:allure-bom:<allure-version>"))
    testImplementation("io.qameta.allure:allure-hamcrest")
    testRuntimeOnly("org.aspectj:aspectjrt:<aspectj-version>")
    aspectjAgent("org.aspectj:aspectjweaver:<aspectj-version>")
}

tasks.test {
    doFirst {
        jvmArgs("-javaagent:${aspectjAgent.singleFile}")
    }
}
```

Maven, with `allure-bom` imported in dependency management, can use the same artifact together with an AspectJ javaagent configured for the test JVM.

## Setup

Enable the AspectJ weaver for the test JVM. The module weaves Hamcrest `MatcherAssert.assertThat(...)` calls and reports them as Allure steps.

## Report Output

- Hamcrest assertions as Allure steps.
- Matcher descriptions and mismatch details where available.
- Failed assertions update the Allure step status and status details.

For large classpaths, consider an AspectJ `META-INF/aop.xml` that limits weaving to Allure, Hamcrest, and your test packages.
