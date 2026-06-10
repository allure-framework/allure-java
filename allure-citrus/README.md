# allure-citrus

Citrus listener integration for Allure Java.

Use this module when your integration tests run on Citrus and you want Citrus suites, test cases, actions, parameters, and failures to appear in Allure Report.

## Supported Versions

- Allure Java 3.x requires Java 17 or newer.
- This module targets the classic `com.consol.citrus` API.
- The current build validates against Citrus 2.8.0.

## Installation

Gradle:

```kotlin
dependencies {
    testImplementation(platform("io.qameta.allure:allure-bom:<allure-version>"))
    testImplementation("io.qameta.allure:allure-citrus")
}
```

Maven, with `allure-bom` imported in dependency management:

```xml
<dependency>
    <groupId>io.qameta.allure</groupId>
    <artifactId>allure-citrus</artifactId>
    <scope>test</scope>
</dependency>
```

## Setup

Register `io.qameta.allure.citrus.AllureCitrus` with Citrus as a test, test suite, and test action listener. The listener translates Citrus suite, test case, and action events into Allure tests and steps.

## Report Output

- Citrus test cases and suite lifecycle.
- Test actions as Allure steps.
- Labels, links, parameters, status, and status details.
