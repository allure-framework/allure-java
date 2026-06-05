# allure-junit4

JUnit 4 listener integration for Allure Java.

## Coordinates

`io.qameta.allure:allure-junit4`

```kotlin
dependencies {
    testImplementation(platform("io.qameta.allure:allure-bom:<allure-version>"))
    testImplementation("io.qameta.allure:allure-junit4")
}
```

## Use

Register `io.qameta.allure.junit4.AllureJunit4` as a JUnit 4 `RunListener` when your launcher supports listener configuration.

For Gradle's built-in JUnit 4 execution, use `allure-junit4-aspect` instead. Gradle does not expose a supported listener registration hook for JUnit 4, so the AspectJ artifact performs automatic listener registration.

## Captured Data

- JUnit 4 test cases, ignored tests, failures, and assumptions.
- Allure labels, links, descriptions, JavaDoc descriptions, and title paths.
- Allure test-plan filtering through `AllureJunit4Filter`.
