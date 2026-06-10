# allure-jupiter

JUnit Jupiter adapter for Allure Java.

Use this module when your tests run on JUnit Jupiter and you want test cases, fixtures, parameters, labels, links, descriptions, and runtime steps to appear in Allure Report.

## Supported Versions

- Allure Java 3.x requires Java 17 or newer.
- This is the primary JUnit Jupiter adapter for Allure Java 3.x.
- The current build validates against the JUnit 6.1.0 platform APIs.
- The old `allure-junit5` artifact alias is no longer published; use `allure-jupiter`.

## Installation

Gradle:

```kotlin
dependencies {
    testImplementation(platform("io.qameta.allure:allure-bom:<allure-version>"))
    testImplementation("io.qameta.allure:allure-jupiter")
}

tasks.test {
    useJUnitPlatform()
}
```

Maven, with `allure-bom` imported in dependency management:

```xml
<dependency>
    <groupId>io.qameta.allure</groupId>
    <artifactId>allure-jupiter</artifactId>
    <scope>test</scope>
</dependency>
```

## Setup

No explicit listener registration is needed in most projects. The module contributes the Allure JUnit Platform listener and the Jupiter extension through service loader metadata.

Run your JUnit Jupiter tests as usual. Allure result files are written to the configured Allure results directory, usually `build/allure-results` for Gradle or `target/allure-results` for Maven.

## Report Output

- Test cases for JUnit Jupiter tests, dynamic tests, repeated tests, and parameterized tests.
- Before/after fixtures with timing and failure details.
- Labels, links, descriptions, JavaDoc descriptions, parameters, status, and status details.
- Runtime steps and attachments created through `allure-java-commons`.

## Related Modules

- Add `allure-jupiter-assert` only when individual JUnit assertion calls should be reported as nested Allure steps.
- Use `allure-junit-platform` directly only when building a custom JUnit Platform integration.
