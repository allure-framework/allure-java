# allure-jooq

jOOQ execute listener integration for Allure Java.

Use this module when your tests execute SQL through jOOQ and you want rendered SQL, execution status, and database failures to appear as Allure steps.

## Supported Versions

- Allure Java 3.x requires Java 17 or newer.
- This module targets jOOQ 3.21.x and requires Java 21 or newer, matching the jOOQ runtime baseline.
- The current build validates against jOOQ 3.21.5.

## Installation

Gradle:

```kotlin
dependencies {
    testImplementation(platform("io.qameta.allure:allure-bom:<allure-version>"))
    testImplementation("io.qameta.allure:allure-jooq")
}
```

Maven, with `allure-bom` imported in dependency management:

```xml
<dependency>
    <groupId>io.qameta.allure</groupId>
    <artifactId>allure-jooq</artifactId>
    <scope>test</scope>
</dependency>
```

## Setup

Register `io.qameta.allure.jooq.AllureJooq` as a jOOQ `ExecuteListener`.

```java
Configuration configuration = new DefaultConfiguration()
        .set(new DefaultExecuteListenerProvider(new AllureJooq()));
```

## Report Output

- SQL rendering and execution as Allure steps.
- Result records and execution failures.
- Step status and status details based on jOOQ execution events.
