# allure-junit-platform

Shared JUnit Platform adapter for Allure Java.

## Coordinates

`io.qameta.allure:allure-junit-platform`

```kotlin
dependencies {
    testImplementation(platform("io.qameta.allure:allure-bom:<allure-version>"))
    testImplementation("io.qameta.allure:allure-junit-platform")
}
```

## Use

Most users should depend on `allure-jupiter` instead. Use this module directly when you are building a custom JUnit Platform based integration and need the shared Allure `TestExecutionListener` and post-discovery test-plan filter.

The module registers:

- `io.qameta.allure.junitplatform.AllureJunitPlatform`
- `io.qameta.allure.junitplatform.AllurePostDiscoveryFilter`

## Captured Data

- JUnit Platform test descriptors and execution events.
- Report entries for parameters and fixture metadata.
- Allure test-plan filtering through commons.
