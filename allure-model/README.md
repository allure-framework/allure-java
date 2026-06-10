# allure-model

Serializable Allure result model for Java integrations.

Most Allure Report users receive this module transitively from `allure-java-commons`. Add it directly only when you build tooling that reads, writes, validates, or transforms Allure result files.

## Supported Versions

- Allure Java 3.x requires Java 17 or newer.
- Model classes match the result format written by the same Allure Java release line.

## Installation

Gradle:

```kotlin
dependencies {
    implementation(platform("io.qameta.allure:allure-bom:<allure-version>"))
    implementation("io.qameta.allure:allure-model")
}
```

Maven, with `allure-bom` imported in dependency management:

```xml
<dependency>
    <groupId>io.qameta.allure</groupId>
    <artifactId>allure-model</artifactId>
</dependency>
```

## Use

Use this module when you need Java objects for Allure result JSON, containers, fixtures, steps, attachments, labels, links, parameters, statuses, and stages.

Normal test projects should prefer a framework adapter and `allure-java-commons` runtime APIs rather than constructing model objects directly.

## Provides

- `TestResult`, `StepResult`, `FixtureResult`, and `ScopeResult`.
- Attachments, labels, links, parameters, statuses, stages, and status details.
- Common model interfaces for attachments, steps, parameters, links, and statuses.

## What To Expect

This module does not discover or run tests. It is a data model for tools and integrations. Regular test projects should use a framework adapter to create model objects and write result files automatically.
