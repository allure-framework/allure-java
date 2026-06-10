# allure-jsonunit

JsonUnit diff attachment integration for Allure Java.

Use this module when your tests compare JSON payloads with JsonUnit and you want readable JSON differences attached to Allure Report.

## Supported Versions

- Allure Java 3.x requires Java 17 or newer.
- This module targets JsonUnit 5.x.
- The current build validates against JsonUnit 5.1.2.

## Installation

Gradle:

```kotlin
dependencies {
    testImplementation(platform("io.qameta.allure:allure-bom:<allure-version>"))
    testImplementation("io.qameta.allure:allure-jsonunit")
}
```

Maven, with `allure-bom` imported in dependency management:

```xml
<dependency>
    <groupId>io.qameta.allure</groupId>
    <artifactId>allure-jsonunit</artifactId>
    <scope>test</scope>
</dependency>
```

## Setup

Use `JsonPatchMatcher.jsonEquals(...)` when comparing JSON payloads.

```java
assertThat(actualJson, JsonPatchMatcher.jsonEquals(expectedJson));
```

## Report Output

- JsonUnit comparison differences.
- A rendered JSON diff attachment named `JSON difference`.
- Failed assertions keep their normal assertion failure status and include the diff as supporting evidence.
