# allure-spock2

Spock 2 integration for Allure Java.

Use this module when your specifications run on Spock 2 and you want specifications, features, iterations, fixtures, parameters, and failures to appear in Allure Report.

## Supported Versions

- Allure Java 3.x requires Java 17 or newer.
- This module targets Spock 2.4, whose block-listener API is used for block reporting.
- The current build validates against Spock 2.4 for Groovy 5.0 and Groovy 5.0.7.

## Installation

Gradle:

```kotlin
dependencies {
    testImplementation(platform("io.qameta.allure:allure-bom:<allure-version>"))
    testImplementation("io.qameta.allure:allure-spock2")
}
```

Maven, with `allure-bom` imported in dependency management:

```xml
<dependency>
    <groupId>io.qameta.allure</groupId>
    <artifactId>allure-spock2</artifactId>
    <scope>test</scope>
</dependency>
```

## Setup

Add the dependency to a Spock 2 project. The module registers `io.qameta.allure.spock2.AllureSpock2` as a Spock global extension through service loader metadata.

Block reporting uses Spock runtime events and does not install a Groovy AST transformation, so the order of the Spock and Allure dependencies does not affect compilation.

## Report Output

- Specifications, features, iterations, fixture methods, and errors.
- Executed `given`/`setup`, `expect`, `when`, `then`, and `cleanup` blocks, with iteration variables resolved in block descriptions. Spock does not emit execution events for `where` and `filter` blocks.
- Data-driven parameters and Spock tags.
- Labels, links, JavaDoc descriptions, test-plan filtering, and fixture metadata.
