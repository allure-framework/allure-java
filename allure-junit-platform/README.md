# allure-junit-platform

Shared JUnit Platform adapter for Allure Java.

Most Allure Report users should add `allure-jupiter` instead. Use this module directly when you build a custom JUnit Platform based integration and need Allure's listener and post-discovery test-plan filter.

## Supported Versions

- Allure Java 3.x requires Java 17 or newer.
- The current build validates against the JUnit 6.1.0 platform APIs.

## Installation

Gradle:

```kotlin
dependencies {
    testImplementation(platform("io.qameta.allure:allure-bom:<allure-version>"))
    testImplementation("io.qameta.allure:allure-junit-platform")
}
```

Maven, with `allure-bom` imported in dependency management:

```xml
<dependency>
    <groupId>io.qameta.allure</groupId>
    <artifactId>allure-junit-platform</artifactId>
    <scope>test</scope>
</dependency>
```

## Setup

The module registers these JUnit Platform services:

- `io.qameta.allure.junitplatform.AllureJunitPlatform`
- `io.qameta.allure.junitplatform.AllurePostDiscoveryFilter`

Custom engines and launchers can reuse those services to write Allure results from JUnit Platform execution events.

## Report Output

- JUnit Platform test descriptors and execution events.
- Report entries for parameters and fixture metadata.
- Test-plan filtering through `allure-java-commons`.
