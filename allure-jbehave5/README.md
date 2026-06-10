# allure-jbehave5

JBehave 5 story reporter integration for Allure Java.

Use this module when your BDD tests run on JBehave 5 and you want stories, scenarios, examples, and steps to appear in Allure Report.

## Supported Versions

- Allure Java 3.x requires Java 17 or newer.
- This module targets JBehave 5.x.
- The current build validates against JBehave 5.2.0.

## Installation

Gradle:

```kotlin
dependencies {
    testImplementation(platform("io.qameta.allure:allure-bom:<allure-version>"))
    testImplementation("io.qameta.allure:allure-jbehave5")
}
```

Maven, with `allure-bom` imported in dependency management:

```xml
<dependency>
    <groupId>io.qameta.allure</groupId>
    <artifactId>allure-jbehave5</artifactId>
    <scope>test</scope>
</dependency>
```

## Setup

Register `io.qameta.allure.jbehave5.AllureJbehave5` as a JBehave story reporter in your JBehave configuration.

## Report Output

- Stories, scenarios, examples, and steps.
- Given stories and nested execution.
- Labels, parameters, status, and status details.
