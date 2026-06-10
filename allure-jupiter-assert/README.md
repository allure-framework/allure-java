# allure-jupiter-assert

JUnit Jupiter assertion step integration for Allure Java.

Use this module when you already use `allure-jupiter` and want individual JUnit Jupiter assertion calls to appear as nested steps in Allure Report.

## Supported Versions

- Allure Java 3.x requires Java 17 or newer.
- This module targets JUnit Jupiter assertions.
- The current build validates against the JUnit 5.10.3 APIs and AspectJ 1.9.25.1.
- The old `allure-junit5-assert` artifact alias is no longer published; use `allure-jupiter-assert`.

## Installation

Gradle:

```kotlin
val aspectjAgent by configurations.creating

dependencies {
    testImplementation(platform("io.qameta.allure:allure-bom:<allure-version>"))
    testImplementation("io.qameta.allure:allure-jupiter")
    testImplementation("io.qameta.allure:allure-jupiter-assert")
    testRuntimeOnly("org.aspectj:aspectjrt:<aspectj-version>")
    aspectjAgent("org.aspectj:aspectjweaver:<aspectj-version>")
}

tasks.test {
    useJUnitPlatform()
    doFirst {
        jvmArgs("-javaagent:${aspectjAgent.singleFile}")
    }
}
```

Maven, with `allure-bom` imported in dependency management, can use the same artifact together with `allure-jupiter` and an AspectJ javaagent configured for the test JVM.

## Setup

Enable the AspectJ weaver for the test JVM. The module weaves JUnit Jupiter assertions and reports them as nested Allure steps.

## Report Output

- JUnit Jupiter assertions as Allure steps.
- Failed assertions update the Allure step status and status details.
- Ordinary JUnit Jupiter test reporting still comes from `allure-jupiter`.

Use this only when assertion-level reporting is desired. `allure-jupiter` is enough for normal JUnit Jupiter reporting.
