# allure-junit4

JUnit 4 listener integration for Allure Java.

Use this module when your JUnit 4 launcher lets you register a `RunListener` and you want JUnit 4 tests, assumptions, ignored tests, failures, labels, and links in Allure Report.

## Supported Versions

- Allure Java 3.x requires Java 17 or newer.
- This module targets JUnit 4.
- The current build validates against JUnit 4.13.2.

## Installation

Gradle:

```kotlin
dependencies {
    testImplementation(platform("io.qameta.allure:allure-bom:<allure-version>"))
    testImplementation("io.qameta.allure:allure-junit4")
}

tasks.test {
    useJUnit()
}
```

Maven, with `allure-bom` imported in dependency management:

```xml
<dependency>
    <groupId>io.qameta.allure</groupId>
    <artifactId>allure-junit4</artifactId>
    <scope>test</scope>
</dependency>
```

## Setup

Register `io.qameta.allure.junit4.AllureJunit4` as a JUnit 4 `RunListener` when your launcher supports listener configuration.

For Gradle's built-in JUnit 4 execution, use `allure-junit4-aspect` instead. Gradle does not expose a supported listener registration hook for JUnit 4, so the AspectJ artifact performs automatic listener registration.

## Report Output

- JUnit 4 test cases, ignored tests, failures, and assumptions.
- Labels, links, descriptions, JavaDoc descriptions, parameters, and title paths.
- Test-plan filtering through `AllureJunit4Filter`.
