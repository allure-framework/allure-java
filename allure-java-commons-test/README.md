# allure-java-commons-test

Test support utilities for Allure Java adapter development.

Normal Allure Report users should not add this module to application or test suites. Use it only when you are writing tests for a custom Allure Java adapter and need the same fixtures used by this repository.

## Supported Versions

- Allure Java 3.x requires Java 17 or newer.
- This module is meant to be used with the matching `allure-java-commons` release.

## Installation

Gradle:

```kotlin
dependencies {
    testImplementation(platform("io.qameta.allure:allure-bom:<allure-version>"))
    testImplementation("io.qameta.allure:allure-java-commons-test")
}
```

Maven, with `allure-bom` imported in dependency management:

```xml
<dependency>
    <groupId>io.qameta.allure</groupId>
    <artifactId>allure-java-commons-test</artifactId>
    <scope>test</scope>
</dependency>
```

## Use

Use the helpers to run adapter code against an in-memory Allure lifecycle and then assert the produced results.

## Provides

- In-memory Allure result writer stubs.
- Test result predicates and assertions.
- JUnit Platform helper runners for adapter tests.
- Random test data helpers used by adapter test suites.

## What To Expect

This module is for tests of Allure integrations. It does not add reporting to an application test suite and should not be required for normal Allure Report usage.
