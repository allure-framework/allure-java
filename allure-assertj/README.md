# allure-assertj

AssertJ assertion step integration for Allure Java.

Use this module when you want AssertJ assertion chains to appear as nested steps in Allure Report.

## Supported Versions

- Allure Java 3.x requires Java 17 or newer.
- This module targets AssertJ Core.
- The current build validates against AssertJ Core 3.27.7 and AspectJ 1.9.25.1.

## Installation

Gradle:

```kotlin
val aspectjAgent by configurations.creating

dependencies {
    testImplementation(platform("io.qameta.allure:allure-bom:<allure-version>"))
    testImplementation("io.qameta.allure:allure-assertj")
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

Enable the AspectJ weaver for the test JVM. The module weaves AssertJ calls and reports assertion chains as Allure steps.

## Report Output

- AssertJ `Assertions.assertThat(...)` and BDD `then(...)` entry points as assertion steps.
- Assertion chains grouped under the corresponding test or user step.
- Failed assertions update the corresponding Allure step status and status details.

For large classpaths, consider an AspectJ `META-INF/aop.xml` that limits weaving to Allure, AssertJ, and your test packages.
