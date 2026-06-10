# allure-testng

TestNG listener integration for Allure Java.

Use this module when your test suite runs on TestNG and you want TestNG suites, tests, configuration methods, parameters, and failures to appear in Allure Report.

## Supported Versions

- Allure Java 3.x requires Java 17 or newer.
- This module targets TestNG 7.x.
- The current build validates against TestNG 7.11.0.

## Installation

Gradle:

```kotlin
dependencies {
    testImplementation(platform("io.qameta.allure:allure-bom:<allure-version>"))
    testImplementation("io.qameta.allure:allure-testng")
}
```

Maven, with `allure-bom` imported in dependency management:

```xml
<dependency>
    <groupId>io.qameta.allure</groupId>
    <artifactId>allure-testng</artifactId>
    <scope>test</scope>
</dependency>
```

## Setup

The module exposes `io.qameta.allure.testng.AllureTestNg` as a TestNG listener through service loader metadata. You can also register it explicitly in `testng.xml` or with `@Listeners`.

```java
import io.qameta.allure.testng.AllureTestNg;
import org.testng.annotations.Listeners;

@Listeners(AllureTestNg.class)
class MyTest {
}
```

## Report Output

- TestNG suites, tests, classes, methods, configuration methods, and data-provider invocations.
- Suite and test fixtures represented as Allure scopes.
- Labels, links, parameters, JavaDoc descriptions, status details, and test-plan filtering.
